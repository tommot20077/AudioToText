package xyz.dowob.audiototext.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
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
    private final AudioProperties audioProperties;
    private final ObjectMapper objectMapper;
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

}
