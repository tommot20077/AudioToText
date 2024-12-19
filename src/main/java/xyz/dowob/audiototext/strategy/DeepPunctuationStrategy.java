package xyz.dowob.audiototext.strategy;

import ai.djl.MalformedModelException;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.vosk.Model;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.translator.PunctuationTranslator;
import xyz.dowob.audiototext.type.ModelType;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * @author yuan
 * @program AudioToText
 * @ClassName DeepPunctuationStrategy
 * @description
 * @create 2024-12-18 12:27
 * @Version 1.0
 **/
// todo 自動下載模型
@Log4j2
@Component
public class DeepPunctuationStrategy implements PunctuationStrategy {

    private static final String MODEL_URL = "oliverguhr/fullstop-punctuation-multilang-large";

    private static final String[] REQUIRED_FILES = {"config.json", "pytorch_model.bin", "special_tokens_map.json", "tokenizer_config" +
            ".json", "tokenizer.json"};

    private final AudioProperties audioProperties;

    private final ZooModel<String, String> model;


    public DeepPunctuationStrategy(
            AudioProperties audioProperties,
            List<ModelType> modelTypes) throws IOException, ModelNotFoundException, MalformedModelException {
        this.audioProperties = audioProperties;

        System.setProperty("ai.djl.pytorch.use_cpu", "true");
        Path modelDirPath = Path.of(audioProperties.getPath().getPunctuationModelDirectory());
        ensureModelDirectory(modelDirPath);

        if (!isModelComplete(modelDirPath)) {
            log.info("模型文件不完整，開始下載模型...");
            downloadModel(modelDirPath);
        }

        PunctuationTranslator translator = new PunctuationTranslator(HuggingFaceTokenizer.newInstance(modelDirPath), audioProperties);
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

    private void ensureModelDirectory(Path modelPath) throws IOException {
        if (!Files.exists(modelPath)) {
            log.info("創建模型目錄: {}", modelPath);
            Files.createDirectories(modelPath);
        }
    }

    private boolean isModelComplete(Path modelPath) {
        for (String file : REQUIRED_FILES) {
            if (!Files.exists(modelPath.resolve(file))) {
                log.info("缺少模型文件: {}", file);
                return false;
            }
        }
        return true;
    }

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

    private String postProcessResult(String result) {
        return result
                .replaceAll("\\[PERIOD\\]", ".")
                .replaceAll("\\[COMMA\\]", ",")
                .replaceAll("\\[QUESTION\\]", "?")
                .replaceAll("\\[EXCLAMATION\\]", "!");
    }
}
