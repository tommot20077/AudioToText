package xyz.dowob.audiototext.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import xyz.dowob.audiototext.config.AudioProperties;

import java.io.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Python 進程處理器，用於處理與 Python 進程的通訊
 * 用於與 Python 進程通訊，發送文本，接收處理結果，並返回結果
 * 在初始化時，會啟動 Python 進程，並監聽 Python 進程的輸出
 * {@link xyz.dowob.audiototext.provider.PythonServiceProvider} 用於提供 Python 進程處理器以及初始化此類
 *
 * @author yuan
 * @program AudioToText
 * @ClassName PythonProcessHandler
 * @create 2025/1/3
 * @Version 1.0
 **/

@Log4j2
public class PythonProcessHandler {
    /**
     * Python 線程類，管理 Python 進程
     */
    private final Process pythonProcess;

    /**
     * Python 輸出流處理器，用於向 Python 進程發送輸入
     */
    private final BufferedWriter bufferedWriter;

    /**
     * Python 輸入流處理器，用於讀取 Python 進程的輸出
     */
    private final BufferedReader bufferedReader;

    /**
     * Json 映射器，用於解析 Python 進程的輸出並轉換為 Json格式
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 最大處理時間，用於限制 Python 進程的處理時間，此項設定在配置檔{@link AudioProperties}中
     */
    private final int maxProcessingTime;

    /**
     * Python 執行檔名稱
     */
    private final String pythonName;

    /**
     * 此處理器的辨識 ID
     */
    @Getter
    private final int processHandlerId;

    /**
     * 當前處理任務的異步返回值，用於等待 Python 進程的返回結果
     */
    private CompletableFuture<JsonNode> responseFuture;


    /**
     * 建構方法，初始化 Python 進程處理器
     *
     * @param venvDirectory 虛擬環境目錄
     *
     * @throws IOException 初始化 Python 進程處理器時出現 IO 錯誤
     */
    public PythonProcessHandler (File venvDirectory, AudioProperties audioProperties, String pythonName, int processHandlerId) throws IOException {
        this.processHandlerId = processHandlerId;
        this.pythonName = pythonName;
        this.maxProcessingTime = audioProperties.getThreshold().getMaxProcessingTime();
        ProcessBuilder pb = new ProcessBuilder(getProgramPath(venvDirectory), "PunctuationRestoration.py");
        pb.directory(venvDirectory);
        pb.redirectErrorStream(false);
        this.pythonProcess = pb.start();
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream()));
        this.bufferedReader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
        startListener();
        testCommunication();
    }

    /**
     * 測試與 Python 進程的通訊，在初始化時，測試與 Python 進程的通訊是否正常
     *
     * @throws IOException 測試 Python通訊出現錯誤
     */
    private void testCommunication () throws IOException {
        try {
            String testInput = objectMapper.writeValueAsString(Map.of("text", "test", "taskId", "test-init"));
            bufferedWriter.write(testInput + "\n");
            bufferedWriter.flush();
        } catch (Exception e) {
            log.error("測試 Python通訊出現錯誤: {}", e.getMessage());
            throw new IOException("測試 Python通訊出現錯誤: " + e.getMessage());
        }
    }

    /**
     * 獲取 Python 執行檔路徑，用於初始化 Python 進程
     * 根據系統環境，獲取 Python 執行檔路徑
     *
     * @param tempDir 用於存放 Python 程序的臨時目錄
     *
     * @return Python 程序路徑
     */
    private String getProgramPath (File tempDir) {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return tempDir.toPath().resolve(".venv").resolve("Scripts").resolve(String.format("%s.exe", pythonName)).toString();
        }
        return tempDir.toPath().resolve(".venv").resolve("bin").resolve(String.format("%s", pythonName)).toString();
    }

    /**
     * 啟動 Python 進程監聽器，用於監聽 Python 進程的輸出
     * 當 Python 進程輸出有新的輸出時，解析輸出並處理
     * 分成兩種情況，一種是 Python 進程返回結果，另一種是 Python 進程返回錯誤
     * 當 Python 進程返回結果時，將結果返回給當前處理的任務
     */
    private void startListener () {
        log.debug("Python Process 監聽器已啟動");
        Thread listenerThread = new Thread(() -> {
            String line;
            try {
                while ((line = bufferedReader.readLine()) != null) {
                    JsonNode node = objectMapper.readTree(line);
                    if (responseFuture != null && node.has("taskId")) {
                        responseFuture.complete(node);
                        responseFuture = null;
                    }
                }
            } catch (JsonProcessingException e) {
                log.debug("非Json格式訊息: {}", e.getMessage());
            } catch (IOException e) {
                log.warn("無法讀取 Python Process 輸出: {}", e.getMessage());
                if (responseFuture != null) {
                    responseFuture.completeExceptionally(e);
                    responseFuture = null;
                }
            } catch (Exception e) {
                log.error("Python Process 監聽器錯誤: {}", e.getMessage());
                if (responseFuture != null) {
                    responseFuture.completeExceptionally(e);
                    responseFuture = null;
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * 發送文本給 Python 進程，並等待 Python 進程返回結果
     * 獲取 Python 進程處理後的文本，並返回給任務中CompletableFuture
     *
     * @param text   文本
     * @param taskId 任務 ID
     *
     * @return Python 進程處理後的文本
     *
     * @throws IOException          發送文本給 Python 進程時出現 IO 錯誤
     * @throws ExecutionException   等待 Python 進程返回結果時出現異常
     * @throws InterruptedException 等待 Python 進程返回結果時被中斷
     */
    public synchronized String sendText (String text, String taskId) throws IOException, ExecutionException, InterruptedException {
        if (!pythonProcess.isAlive()) {
            throw new IOException("Python 進程已終止");
        }

        responseFuture = new CompletableFuture<>();
        String input = objectMapper.writeValueAsString(Map.of("text", text, "taskId", taskId));

        try {
            bufferedWriter.write(input + "\n");
            bufferedWriter.flush();

            JsonNode node = responseFuture.get(maxProcessingTime, TimeUnit.SECONDS);
            if (node.get("isSuccess").asBoolean()) {
                return node.get("restoredText").asText();
            } else {
                throw new IOException("Python腳本在處理時發生異常: " + node.get("error").asText());
            }
        } catch (TimeoutException e) {
            throw new IOException("處理文檔時間超過限制");
        }
    }

    /**
     * 判斷 Python 進程是否可用
     *
     * @return Python 進程是否可用
     */
    public boolean isAvailable () {
        return responseFuture == null;
    }

    /**
     * 銷毀 Python 進程，關閉 Python 進程的輸入流，輸出流，並銷毀 Python 進程
     */
    public void destroy () {
        try {
            bufferedWriter.close();
            bufferedReader.close();
            pythonProcess.destroy();
        } catch (IOException e) {
            log.error("Python Process 銷毀失敗: {}", e.getMessage());
        }
    }
}
