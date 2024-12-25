package xyz.dowob.audiototext.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.vosk.Model;
import xyz.dowob.audiototext.type.ModelType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 有關於服務的配置，包含模型的初始化、目錄的建立
 *
 * @author yuan
 * @program AudioToText
 * @ClassName ServiceConfig
 * @description
 * @create 2024-12-13 23:38
 * @Version 1.0
 **/
@Log4j2
@Configuration
@RequiredArgsConstructor
public class ServiceConfig {
    /**
     * 音檔配置類: 包含音檔的路徑、格式、閾值等設定
     */
    private final AudioProperties audioProperties;

    /**
     * Json 轉換映射工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 初始化創建運行時所需的目錄，由配置檔案中的路徑設定
     *
     * @throws IOException 無法建立目錄
     */
    @PostConstruct
    public void init() throws IOException {
        createDirectoryIfNotExists(audioProperties.getPath().getOutputDirectory());
        createDirectoryIfNotExists(audioProperties.getPath().getPunctuationModelDirectory());
        createDirectoryIfNotExists(audioProperties.getPath().getModelDirectory());
        createDirectoryIfNotExists(audioProperties.getPath().getTempFileDirectory());
    }


    /**
     * 讀取模型資訊檔案，並返回模型類型的列表
     *
     * @return 模型類型的列表
     *
     * @throws IOException 無法讀取模型資訊檔案
     */
    @Bean
    public List<ModelType> modelTypeConfigs() throws IOException {
        File modelInfoPath = new File(audioProperties.getPath().getModelInfoPath());
        if (!modelInfoPath.exists()) {
            if (!modelInfoPath.createNewFile()) {
                throw new RemoteException("無法建立模型資訊檔案: " + modelInfoPath.getAbsolutePath());
            }
            throw new RuntimeException("模型資訊檔案已建立: {}, 請配置模型資訊檔案: " + modelInfoPath.getAbsolutePath());
        }
        log.info("開始讀取模型資訊檔案: {}", modelInfoPath.getAbsolutePath());
        try (InputStream inputStream = new FileInputStream(modelInfoPath)) {
            Map<String, List<ModelType>> modelMap = objectMapper.readValue(inputStream, new TypeReference<>() {});
            return modelMap.get("model");
        } catch (IOException e) {
            log.error("無法讀取模型資訊檔案: {}", modelInfoPath.getAbsolutePath(), e);
            throw e;
        }
    }


    /**
     * 初始化語音識別模型，並返回模型類型與模型的映射，模型類型為 {@link ModelType} 類別
     * 會根據模型資訊檔案中的模型類型列表，初始化模型，回傳的模型類型與模型的映射中，只包含已經成功加載的模型
     * 此方法會需要依賴於 {@link ModelType} 類別的初始化，需要在其初始化後執行
     *
     * @return 模型類型與模型的映射
     */
    @Bean
    @DependsOn("modelType.ModelTypeInitializer")
    public Map<ModelType, Model> modelMap() {
        log.info("開始初始化語音識別模型...");
        File modelDirectory = new File(audioProperties.getPath().getModelDirectory());
        if (!modelDirectory.exists() || !modelDirectory.isDirectory()) {
            log.error("無法找到模型目錄: {}", modelDirectory.getAbsolutePath());
            if (!modelDirectory.mkdirs()) {
                log.error("無法建立模型目錄: {}", modelDirectory.getAbsolutePath());
                throw new IllegalStateException("無法建立模型目錄: " + modelDirectory.getAbsolutePath());
            }
            throw new IllegalStateException("模型目錄已建立: " + modelDirectory.getAbsolutePath() + "，請將模型放置於此目錄");
        }

        File modelPath = null;
        try {
            Map<ModelType, Model> modelMap = new HashMap<>();
            for (ModelType modelType : ModelType.getModelTypeMap().values()) {
                modelPath = new File(audioProperties.getPath().getModelDirectory() + File.separator + modelType.code());
                if (modelPath.exists() && modelPath.isDirectory()) {
                    modelMap.put(modelType, new Model(modelPath.getAbsolutePath()));
                    log.info("模型加載成功: {}", modelPath);
                } else {
                    log.warn("無法載入模型, 名稱: {}, 路徑: {}", modelType.code(), modelPath.getAbsolutePath());
                }
            }
            return modelMap;
        } catch (IOException e) {
            log.error("模型加載失敗: {}", modelPath.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 創建目錄，如果目錄不存在
     *
     * @param directoryPath 目錄路徑
     *
     * @throws IOException 無法建立目錄
     */
    private void createDirectoryIfNotExists(String directoryPath) throws IOException {
        Path path = Path.of(directoryPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("目錄已建立: {}", directoryPath);
        } else {
            log.debug("目錄已存在: {}", directoryPath);
        }
    }
}
