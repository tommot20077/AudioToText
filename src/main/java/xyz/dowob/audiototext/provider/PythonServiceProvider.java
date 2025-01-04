package xyz.dowob.audiototext.provider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.config.ServiceConfig;
import xyz.dowob.audiototext.dto.PunctuationTaskDTO;
import xyz.dowob.audiototext.handler.PythonProcessHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

/**
 * Python外部服務類別，用於初始化Python服務並與Python服務進行通信，利用Python來處理句子的編點符號恢復模型
 * 此類別依賴 {@link ServiceConfig} 類別，因此必須在其之後初始化，並且在初始化時檢查Python環境是否正確
 *
 * @author yuan
 * @program AudioToText
 * @ClassName PythonServiceInitializer
 * @create 2025/1/2
 * @Version 1.0
 **/
@Log4j2
@Component
@DependsOn("serviceConfig")
public class PythonServiceProvider {
    /**
     * 必要的文件列表，用於檢查資源目錄是否完整
     */
    private static final String[] REQUIRED_FILES = {"PunctuationRestoration.py", "requirements.txt"};
    /**
     * Python 處理器列表，用於保存 Python 處理器，當有任務時，從列表中選擇一個處理器進行處理
     */
    private final List<PythonProcessHandler> pythonHandlers = new ArrayList<>();
    /**
     * 線程池，用於管理 Python 處理器的執行緒
     */
    private final ExecutorService executorService;
    /**
     * 信號量，用於限制 Python 處理器的最大數量
     */
    private final Semaphore semaphore;
    /**
     * 音訊的配置信息，用於獲取 Python 處理器的配置信息
     */
    private final AudioProperties audioProperties;
    /**
     * 最大 Python 處理器數量，用於限制 Python 處理器的最大數量
     */
    private final int maxPythonProcessNumber;
    /**
     * 任務隊列，用於保存等待處理的任務，使用併發隊列，保證多線程安全
     */
    private final Queue<PunctuationTaskDTO> taskQueue = new ConcurrentLinkedQueue<>();


    /**
     * PythonServiceInitializer 構造方法，初始化 Python 服務提供者
     *
     * @param audioProperties 音訊的配置信息
     */
    public PythonServiceProvider (AudioProperties audioProperties) {
        this.audioProperties = audioProperties;
        this.maxPythonProcessNumber = audioProperties.getThreshold().getMaxPythonProcess();
        this.executorService = Executors.newFixedThreadPool(maxPythonProcessNumber);
        this.semaphore = new Semaphore(maxPythonProcessNumber);
    }

    /**
     * 初始化方法，用於初始化 Python 服務提供者
     * 在初始化時，檢查 Python 環境是否正確，並創建 Python 虛擬環境
     * 安裝 Python 依賴，並初始化 Python 處理器列表
     *
     * @throws IOException          初始化時可能拋出的 IO 異常
     * @throws InterruptedException 初始化時可能拋出的中斷異常
     * @throws URISyntaxException   初始化時可能拋出的 URI 異常
     */
    @PostConstruct
    public void init () throws IOException, InterruptedException, URISyntaxException {
        if (!isPythonInstalled()) {
            throw new RuntimeException("未偵測到 Python 環境，請安裝 Python 3");
        }
        File punctuationScriptDirectory = getResourceDirectory();
        if (!punctuationScriptDirectory.exists() || !checkFileExist(punctuationScriptDirectory)) {
            throw new IllegalArgumentException("資源目錄不存在或不完整: " + punctuationScriptDirectory);
        }

        createVirtualEnvironment(punctuationScriptDirectory);
        installDependencies(punctuationScriptDirectory);

        for (int i = 0; i < maxPythonProcessNumber; i++) {
            pythonHandlers.add(new PythonProcessHandler(punctuationScriptDirectory, audioProperties));
            log.info("初始化 PythonProcessHandler [{}]", i + 1);
        }
    }

    /**
     * 檢查 Python 環境是否正確，檢查是否安裝了 Python 3
     *
     * @return 是否安裝了 Python 3
     */
    private boolean isPythonInstalled () {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "--version");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String version = reader.readLine();
            int exitCode = process.waitFor();
            return exitCode == 0 && version.startsWith("Python 3");
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * 創建 Python 虛擬環境，用於安裝 Python 依賴
     *
     * @param virDirectory 虛擬環境目錄
     *
     * @throws IOException          創建虛擬環境時可能拋出的 IO 異常
     * @throws InterruptedException 創建虛擬環境時可能拋出的中斷異常
     */
    private void createVirtualEnvironment (File virDirectory) throws IOException, InterruptedException {
        if (new File(virDirectory, "venv").exists()) {
            log.debug("Python 虛擬環境已存在，跳過創建步驟");
            return;
        }
        ProcessBuilder pb = new ProcessBuilder("python", "-m", "venv", "venv");
        pb.directory(virDirectory);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("無法創建 Python 虛擬環境, 退出碼: " + exitCode);
        }
        log.info("創建 Python 虛擬環境完成");
    }

    /**
     * 安裝 Python 依賴，用於安裝 Python 依賴
     *
     * @param tempDir 依賴安裝目錄
     *
     * @throws IOException          安裝依賴時可能拋出的 IO 異常
     * @throws InterruptedException 安裝依賴時可能拋出的中斷異常
     */
    private void installDependencies (File tempDir) throws IOException, InterruptedException {
        ProcessBuilder checkBuilder = new ProcessBuilder(getProgramPath(tempDir), "list");
        checkBuilder.directory(tempDir);
        Process checkProcess = checkBuilder.start();

        Set<String> installedPackages = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                installedPackages.add(line.split(" ")[0]);
            }
        }
        int checkExitCode = checkProcess.waitFor();

        if (checkExitCode != 0) {
            throw new RuntimeException("檢查已安裝依賴失敗，退出碼：" + checkExitCode);
        }
        List<String> requiredPackages = Files.readAllLines(tempDir.toPath().resolve("requirements.txt"));
        log.debug("已安裝的 Python 依賴: {}", installedPackages);
        log.debug("需要安裝的 Python 依賴: {}", requiredPackages);

        boolean allInstalled = installedPackages.containsAll(requiredPackages);

        if (allInstalled) {
            log.debug("所有依賴已安裝，跳過安裝步驟。");
            return;
        }

        ProcessBuilder pipBuilder = new ProcessBuilder(getProgramPath(tempDir), "install", "-r", "requirements.txt");
        pipBuilder.directory(tempDir);
        log.info("安裝 Python 依賴...");
        Process pipProcess = pipBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(pipProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        }
        int exitCode = pipProcess.waitFor();

        log.info("Python 依賴安裝完成");
        if (exitCode != 0) {
            throw new RuntimeException("安裝 Python 依賴失敗，退出碼：" + exitCode);
        }
    }

    /**
     * 獲取 Python 程序路徑，用於初始化 Python 進程，會根據系統環境獲取 Pip 程序路徑來安裝 Python 依賴
     *
     * @param venvDirectory 用於存放 Python 程序的臨時目錄
     *
     * @return Python 程序路徑
     */
    private String getProgramPath (File venvDirectory) {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return venvDirectory.toPath().resolve("venv").resolve("Scripts").resolve(String.format("%s.exe", "pip")).toString();
        }
        return venvDirectory.toPath().resolve("venv").resolve("bin").resolve(String.format("%s", "pip")).toString();
    }

    /**
     * 獲取資源目錄，用於獲取 Python 腳本目錄
     *
     * @return 資源目錄
     *
     * @throws URISyntaxException 獲取資源目錄時可能拋出的 URI 異常
     */
    private File getResourceDirectory () throws URISyntaxException {
        URL resourceUrl = getClass().getClassLoader().getResource("python");
        if (resourceUrl == null) {
            throw new IllegalArgumentException("找不到資源目錄: python");
        }
        return new File(resourceUrl.toURI());
    }

    /**
     * 檢查文件是否存在，用於檢查資源目錄是否完整
     *
     * @param directory 資源目錄
     *
     * @return 資源目錄是否完整
     */
    private boolean checkFileExist (File directory) {
        for (String file : REQUIRED_FILES) {
            if (!new File(directory, file).exists()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 獲取標點符號恢復結果，用於獲取標點符號恢復結果
     *
     * @param text   文本
     * @param taskId 任務ID
     *
     * @return 標點符號恢復結果
     *
     * @throws InterruptedException 獲取標點符號恢復結果時可能拋出的中斷異常
     * @throws ExecutionException   獲取標點符號恢復結果時可能拋出的執行異常
     */
    public synchronized String getPunctuationResult (String text, String taskId) throws InterruptedException, ExecutionException {
        PunctuationTaskDTO task = new PunctuationTaskDTO(taskId, text);
        boolean acquired = semaphore.tryAcquire();
        if (acquired) {
            log.debug("提交任務: {}", task);
            submitTask(task);
        } else {
            log.debug("任務進入等待隊列: {}", task);
            taskQueue.offer(task);
        }
        return task.getFuture().get();
    }

    /**
     * 提交任務，用於提交標點符號恢復任務
     * 會檢查是否有可用的 Python 處理器，如果有則提交任務，否則將任務加入等待隊列
     *
     * @param task 任務
     */
    private void submitTask (PunctuationTaskDTO task) {

            try {
                PythonProcessHandler handler = getAvailableHandler();
                if (handler == null) {
                    throw new RuntimeException("沒有可用的 Python 處理器");
                }
                String result = handler.sendText(task.getText(), task.getTaskId());
                log.debug("Python 處理完成: {}", result);
                task.getFuture().complete(result);
            } catch (Exception e) {
                log.error("Python 處理錯誤: {}", e.getMessage());
                task.getFuture().completeExceptionally(e);
            } finally {
                log.debug("釋放處理器");
                semaphore.release();
                if (taskQueue.peek() != null) {
                    log.debug("處理等待隊列任務");
                    if (semaphore.tryAcquire()) {
                        log.debug("獲取到處理器，提交任務");
                        submitTask(taskQueue.poll());
                    }
                }
            }
        executorService.submit(() -> {
        });
    }

    /**
     * 獲取可用的 Python 處理器，用於獲取可用的 Python 處理器
     * 此方法會搜尋 Python 處理器列表，並返回第一個可用的 Python 處理器
     *
     * @return 可用的 Python 處理器
     */
    private synchronized PythonProcessHandler getAvailableHandler () {
        if (pythonHandlers.isEmpty()) {
            throw new RuntimeException("Python 處理器列表為空，請檢查初始化是否正確");
        }
        return pythonHandlers.stream().filter(PythonProcessHandler::isAvailable).findFirst().orElse(null);
    }

    /**
     * 銷毀方法，用於銷毀 Python 服務提供者
     * 在銷毀時，關閉所有 Python 處理器
     */
    @PreDestroy
    public void destroy () {
        executorService.shutdown();
        pythonHandlers.forEach(PythonProcessHandler::destroy);
        log.info("關閉所有 PythonProcessHandler");
    }
}

