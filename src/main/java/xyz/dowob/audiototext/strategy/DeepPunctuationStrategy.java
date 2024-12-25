package xyz.dowob.audiototext.strategy;

import ai.djl.MalformedModelException;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.translator.PunctuationTranslator;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 用於實現模型添加標點符號的策略模式
 * 實現 PunctuationStrategy 介面，實現添加標點符號的功能
 *
 * @author yuan
 * @program AudioToText
 * @ClassName DeepPunctuationStrategy
 * @description
 * @create 2024-12-18 12:27
 * @Version 1.0
 **/
@Log4j2
@Component
public class DeepPunctuationStrategy implements PunctuationStrategy {

    /**
     * 模型下載地址，使用 HuggingFace 模型庫
     */
    private static final String MODEL_URL = "oliverguhr/fullstop-punctuation-multilang-large";

    /**
     * 模型所需的文件，用於確保模型完整性
     */
    private static final String[] REQUIRED_FILES = {"config.json", "pytorch_model.bin", "special_tokens_map.json", "tokenizer_config" +
            ".json", "tokenizer.json"};

    /**
     * 音訊的配置信息
     */
    private final AudioProperties audioProperties;

    /**
     * 用於添加標點符號的模型
     */
    private final ZooModel<String, String> model;


    /**
     * 創建 DeepPunctuationStrategy 實例
     * 使用 HuggingFace 模型庫下載模型，並初始化模型
     * 優先使用DLJ模型庫，若失敗則使用備用方法從 HuggingFace 模型庫下載模型
     * 若模型文件不完整，則下載模型文件
     * 使用 PyTorch 引擎，並使用 ProgressBar 顯示進度，關閉 GPU 加速，使用 CPU 運行
     *
     * @param audioProperties 音訊的配置信息
     *
     * @throws IOException             模型文件讀取、下載時發生錯誤
     * @throws ModelNotFoundException  模型未找到
     * @throws MalformedModelException 模型格式錯誤
     */
    public DeepPunctuationStrategy(
            AudioProperties audioProperties) throws IOException, ModelNotFoundException, MalformedModelException {
        this.audioProperties = audioProperties;

        System.setProperty("ai.djl.pytorch.use_cpu", "true");
        Path modelDirPath = Path.of(audioProperties.getPath().getPunctuationModelDirectory());
        ensureModelDirectory(modelDirPath);

        if (!isModelComplete(modelDirPath)) {
            log.info("模型文件不完整，開始下載模型...");
            downloadModel(modelDirPath);
        }

        PunctuationTranslator translator = new PunctuationTranslator(HuggingFaceTokenizer.newInstance(modelDirPath));
        Criteria<String, String> criteria = null;
        try {
            criteria = Criteria
                    .builder()
                    .setTypes(String.class, String.class)
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/" + MODEL_URL)
                    .optTranslator(translator)
                    .optEngine("PyTorch")
                    .optProgress(new ProgressBar())
                    .build();
        } catch (Exception e) {
            log.error("模型載入失敗，嘗試使用備用方法", e);
            criteria = Criteria
                    .builder()
                    .setTypes(String.class, String.class)
                    .optModelUrls("https://huggingface.co/" + MODEL_URL)
                    .optTranslator(translator)
                    .optEngine("PyTorch")
                    .optProgress(new ProgressBar())
                    .build();

        } finally {
            if (criteria == null) {
                throw new IOException(String.format("無法載入模型: %s", MODEL_URL));
            }
            model = criteria.loadModel();
            log.info("模型: {} 載入完成", MODEL_URL);
        }

    }

    /**
     * 確保模型目錄存在，若不存在則創建
     *
     * @param modelPath 模型目錄路徑
     *
     * @throws IOException 創建目錄時發生錯誤
     */
    private void ensureModelDirectory(Path modelPath) throws IOException {
        if (!Files.exists(modelPath)) {
            log.info("創建模型目錄: {}", modelPath);
            Files.createDirectories(modelPath);
        }
    }

    /**
     * 檢查模型是否完整，當模型目錄下缺少必要文件時返回 false
     *
     * @param modelPath 模型目錄路徑
     *
     * @return 是否完整
     */
    private boolean isModelComplete(Path modelPath) {
        for (String file : REQUIRED_FILES) {
            if (!Files.exists(modelPath.resolve(file))) {
                log.info("缺少模型文件: {}", file);
                return false;
            }
        }
        return true;
    }

    /**
     * 下載模型文件，下載模型所需的文件到指定目錄
     *
     * @param modelPath 模型目錄路徑
     *
     * @throws IOException 下載文件時發生錯誤
     */
    private void downloadModel(Path modelPath) throws IOException {
        String baseUrl = "https://huggingface.co/" + MODEL_URL + "/resolve/main/";

        for (String file : REQUIRED_FILES) {
            Path targetPath = modelPath.resolve(file);
            String fileUrl = baseUrl + file;

            log.info("下載文件: {} -> {}", fileUrl, targetPath);

            try {
                downloadFile(fileUrl, targetPath);
            } catch (IOException e) {
                log.error("下載文件失敗: {}", file, e);
                throw new IOException("下載模型文件失敗: " + file, e);
            }
        }
    }

    /**
     * 下載文件，將文件下載到指定路徑
     *
     * @param url        文件下載地址
     * @param targetPath 目標路徑
     *
     * @throws IOException 下載文件時發生錯誤
     */
    private void downloadFile(String url, Path targetPath) throws IOException {
        URLConnection connection = URL.of(URI.create(url), null).openConnection();
        long fileSize = connection.getContentLengthLong();

        try (InputStream in = new BufferedInputStream(connection.getInputStream());
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(targetPath))) {

            byte[] buffer = new byte[audioProperties.getThreshold().getChunkBufferSize()];
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // 顯示下載進度
                double progress = (double) totalBytesRead / fileSize * 100;
                BigDecimal roundedProgress = new BigDecimal(progress).setScale(2, RoundingMode.HALF_UP);
                log.info("下載進度: {}%", roundedProgress);
            }
        }
    }

    /**
     * 添加標點符號到文本中
     *
     * @param text 原始文本
     *
     * @return 添加標點符號後的文本
     *
     * @throws Exception 添加標點符號時發生錯誤
     */
    @Override
    public String addPunctuation(String text) throws Exception {
        try (Predictor<String, String> predictor = model.newPredictor()) {
            String result = predictor.predict(text);
            return postProcessResult(result);
        }
    }

    /**
     * 取得模型名稱
     *
     * @return 模型名稱
     */
    @Override
    public String getModelName() {
        return "DeepPunctuation";
    }

    /**
     * 後處理模型輸出結果，將特殊標記替換為對應的標點符號
     *
     * @param result 模型輸出結果
     *
     * @return 替換後的結果
     */
    private String postProcessResult(String result) {
        return result
                .replaceAll("\\[PERIOD\\]", ".")
                .replaceAll("\\[COMMA\\]", ",")
                .replaceAll("\\[QUESTION\\]", "?")
                .replaceAll("\\[EXCLAMATION\\]", "!");
    }
}
