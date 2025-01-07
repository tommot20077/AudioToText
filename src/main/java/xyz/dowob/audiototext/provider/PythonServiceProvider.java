package xyz.dowob.audiototext.provider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.config.ServiceConfig;
import xyz.dowob.audiototext.dto.PunctuationTaskDTO;
import xyz.dowob.audiototext.handler.PythonProcessHandler;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
@ConditionalOnProperty(name = "audio.service.enable-punctuation-restoration", havingValue = "true")
public class PythonServiceProvider {
    /**
     * 線程池，用於管理 Python 處理器的執行緒
     */
    private final ExecutorService pythonExecutor;
    /**
     * 信號量，用於限制 Python 處理器的最大數量
     */
    private final Semaphore semaphore;
    /**
     * 音訊的配置信息，用於獲取 Python 處理器的配置信息
     */
    private final AudioProperties audioProperties;
    /**
     * 必要的文件列表，用於檢查資源目錄是否完整
     */
    private static final String[] REQUIRED_FILES = {"PunctuationRestoration.py", "requirements.txt"};
    /**
     * 最大 Python 處理器數量，用於限制 Python 處理器的最大數量
     */
    private final int MAX_PROCESSING_NUMBER;
    /**
     * Python 腳本目錄，用於 Python 腳本相關資料的目錄
     */
    private final String PYTHON_DIRECTORY;
    /**
     * Python 處理器列表，用於保存 Python 處理器，當有任務時，從列表中選擇一個處理器進行處理
     */
    private final List<PythonProcessHandler> processHandlers = new ArrayList<>();
    /**
     * 任務隊列，用於保存等待處理的任務，使用併發隊列，保證多線程安全
     */
    private final Queue<PunctuationTaskDTO> TASK_QUEUE = new ConcurrentLinkedQueue<>();
    /**
     * Python 程序名稱，用於檢查 Python 環境
     */
    private String PYTHON_NAME;


    /**
     * PythonServiceInitializer 構造方法，初始化 Python 服務提供者
     *
     * @param audioProperties 音訊的配置信息
     */
    public PythonServiceProvider (AudioProperties audioProperties) {
        this.audioProperties = audioProperties;
        this.MAX_PROCESSING_NUMBER = audioProperties.getThreshold().getMaxPythonProcess();
        this.PYTHON_DIRECTORY = audioProperties.getPath().getPythonScriptPath();
        this.pythonExecutor = new ThreadPoolExecutor(MAX_PROCESSING_NUMBER,
                                                     MAX_PROCESSING_NUMBER,
                                                     0L,
                                                     TimeUnit.MILLISECONDS,
                                                     new LinkedBlockingQueue<>(),
                                                     ThreadFactory -> {
                                                         Thread thread = new Thread(ThreadFactory);
                                                         thread.setName("Python-Service-Thread");
                                                         thread.setDaemon(true);
                                                         return thread;
                                                     },
                                                     new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.semaphore = new Semaphore(MAX_PROCESSING_NUMBER);
    }

    /**
     * 初始化方法，用於初始化 Python 服務提供者
     * 在初始化時，檢查 Python 環境是否正確，並創建 Python 虛擬環境
     * 安裝 Python 依賴，並初始化 Python 處理器列表
     *
     * @throws IOException          初始化時可能拋出的 IO 異常
     * @throws InterruptedException 初始化時可能拋出的中斷異常
     */
    @PostConstruct
    public void init () throws IOException, InterruptedException {
        if (checkPythonVersion("python") && checkPythonVersion("python3")) {
            throw new RuntimeException("未偵測到 Python 環境，請安裝 Python 3");
        }
        File punctuationScriptDirectory = getResourceDirectory();

        createVirtualEnvironment(punctuationScriptDirectory);
        installDependencies(punctuationScriptDirectory);

        for (int i = 0; i < MAX_PROCESSING_NUMBER; i++) {
            processHandlers.add(new PythonProcessHandler(punctuationScriptDirectory, audioProperties, PYTHON_NAME));
            log.info("初始化 PythonProcessHandler [{}]", i + 1);
        }
    }

    /**
     * 檢查 Python 環境是否正確，檢查是否安裝了 Python 3
     * 此方法使用反轉的邏輯，如果 Python 3 未安裝，則返回 true，否則返回 false
     *
     * @return 是否安裝了 Python 3
     */
    private boolean checkPythonVersion (String pythonName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonName, "--version");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String version = reader.readLine();
            int exitCode = process.waitFor();

            boolean isInstalled = exitCode == 0 && version.startsWith("Python 3");
            if (isInstalled) {
                this.PYTHON_NAME = pythonName;
            }
            return !isInstalled;
        } catch (Exception e) {
            return true;
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
        if (new File(virDirectory, ".venv").exists()) {
            log.debug("Python 虛擬環境已存在，跳過創建步驟");
            return;
        }
        ProcessBuilder pb = new ProcessBuilder(PYTHON_NAME, "-m", "venv", ".venv");
        pb.directory(virDirectory);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("無法創建 Python 虛擬環境, 退出碼: " + exitCode + "，請檢查是否安裝 python3-venv");
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
        ProcessBuilder checkBuilder = new ProcessBuilder(getProgramPath(tempDir), "-m", "pip", "list");
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

        ProcessBuilder pipBuilder = new ProcessBuilder(getProgramPath(tempDir), "-m", "pip", "install", "-r", "requirements.txt");
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
            return venvDirectory.toPath().resolve(".venv").resolve("Scripts").resolve(String.format("%s.exe", PYTHON_NAME)).toString();
        }
        return venvDirectory.toPath().resolve(".venv").resolve("bin").resolve(String.format("%s", PYTHON_NAME)).toString();
    }

    /**
     * 獲取資源目錄，用於獲取 Python 腳本目錄
     *
     * @return 資源目錄
     */
    private File getResourceDirectory () throws IOException {
        File workDir = new File(PYTHON_DIRECTORY);
        if ((!workDir.exists() && workDir.mkdirs()) || !checkFileExist(workDir)) {
            log.debug("資料有缺失複製資源到目錄: {}", workDir);
            copyResourcesToDirectory(workDir);
        }
        return workDir;
    }

    /**
     * 複製資源到目錄，用於複製 Python 腳本目錄
     * 此方法會從 JAR 檔或資源目錄中複製資源到目標目錄
     *
     * @param targetDir 目標目錄
     *
     * @throws IOException 複製資源時可能拋出的 IO 異常
     */
    private void copyResourcesToDirectory (File targetDir) throws IOException {
        String resourcePath = "BOOT-INF/classes/python";
        ClassLoader classLoader = getClass().getClassLoader();
        URL resourceURL = classLoader.getResource(resourcePath);
        if (resourceURL == null) {
            throw new IllegalArgumentException("找不到資源目錄: " + resourcePath);
        }

        if (resourceURL.getProtocol().equals("jar")) {
            String jarPath = resourceURL.getPath().substring(5, resourceURL.getPath().indexOf("!"));
            log.debug("從 JAR 檔複製資源到目錄: {}", jarPath);
            try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entryName.startsWith(resourcePath + "/") && !entry.isDirectory()) {
                        String fileName = entryName.substring(resourcePath.length() + 1);
                        try (InputStream is = classLoader.getResourceAsStream(entryName)) {
                            if (is != null) {
                                File targetFile = new File(targetDir, fileName);
                                Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
            }
        } else {
            File directory = new File(resourceURL.getPath());
            log.debug("從資源目錄複製資源到目錄: {}", directory);
            if (directory.isDirectory()) {
                for (String fileName : Objects.requireNonNull(directory.list())) {
                    try (InputStream is = classLoader.getResourceAsStream(resourcePath + "/" + fileName)) {
                        if (is != null) {
                            File targetFile = new File(targetDir, fileName);
                            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
        }
    }

    /**
     * 檢查文件是否存在，用於檢查資源目錄是否完整
     *
     * @param directory 資源目錄
     *
     * @return 資源目錄是否完整
     */
    private boolean checkFileExist (File directory) {
        log.debug("檢查資源目錄是否完整: {}", directory);
        for (String file : REQUIRED_FILES) {
            if (!new File(directory, file).exists()) {
                log.debug("資源目錄不完整: {}", file);
                return false;
            }
        }
        log.debug("資源目錄完整");
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
     */
    public synchronized String getPunctuationResult (String text, String taskId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        PunctuationTaskDTO task = new PunctuationTaskDTO(taskId, text, future);

        if (semaphore.tryAcquire()) {
            log.debug("提交任務: {}", task);
            submitTask(task);
        } else {
            log.debug("任務進入等待隊列: {}", task);
            TASK_QUEUE.offer(task);
        }

        try {
            log.debug("等待任務完成: {}", task);
            return future.get(5, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("獲取結果時發生錯誤: ", e);
            throw new RuntimeException("處理任務失敗", e);
        }
    }

    /**
     * 提交任務，用於提交標點符號恢復任務
     * 會檢查是否有可用的 Python 處理器，如果有則提交任務，否則將任務加入等待隊列
     *
     * @param task 任務
     */
    private void submitTask (PunctuationTaskDTO task) {
        CompletableFuture.supplyAsync(() -> {
            try {
                PythonProcessHandler handler = getAvailableHandler();
                if (handler == null) {
                    throw new RuntimeException("沒有可用的 Python 處理器");
                }
                log.debug("使用處理器: {}", handler);
                return handler.sendText(task.getText(), task.getTaskId());
            } catch (Exception e) {
                log.error("Python 處理錯誤: {}", e.getMessage());
                throw new CompletionException(e);
            }
        }, pythonExecutor).whenComplete((result, throwable) -> {
            if (throwable != null) {
                task.getFuture().completeExceptionally(throwable);
                log.error("任務 {} 執行失敗: {}", task.getTaskId(), throwable.getMessage());
            } else {
                task.getFuture().complete(result);
                log.debug("任務 {} 完成: {}", task.getTaskId(), result);
            }
            log.debug("釋放處理器");
            semaphore.release();
            if (TASK_QUEUE.peek() != null && semaphore.tryAcquire()) {
                log.debug("獲取到處理器，提交下一個任務");
                submitTask(TASK_QUEUE.poll());
            }
        });
    }

    /**
     * 獲取可用的 Python 處理器，用於獲取可用的 Python 處理器
     * 此方法會搜尋 Python 處理器列表，並返回第一個可用的 Python 處理器
     *
     * @return 可用的 Python 處理器
     */
    private PythonProcessHandler getAvailableHandler () {
        if (processHandlers.isEmpty()) {
            throw new RuntimeException("Python 處理器列表為空，請檢查初始化是否正確");
        }
        return processHandlers.stream().filter(PythonProcessHandler::isAvailable).findFirst().orElse(null);
    }

    /**
     * 銷毀方法，用於銷毀 Python 服務提供者
     * 在銷毀時，關閉所有 Python 處理器
     */
    @PreDestroy

    public void destroy () {
        pythonExecutor.shutdown();
        try {
            if (!pythonExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                pythonExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            pythonExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

