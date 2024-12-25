package xyz.dowob.audiototext.strategy;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.vosk.Model;
import xyz.dowob.audiototext.dto.ModelInfoDTO;
import xyz.dowob.audiototext.type.ModelType;

import java.util.List;
import java.util.Map;

/**
 * 用於實現語音識別策略模式，
 * 用於根據不同的模型類型獲取對應的模型，以及獲取所有可用的模型信息
 * 可用模型會配置在 model-info.json 文件中
 *
 * @author yuan
 * @program AudioToText
 * @ClassName SpeechRecognitionStrategy
 * @description
 * @create 2024-12-17 16:26
 * @Version 1.0
 **/
@Component
@Log4j2
public class SpeechRecognitionStrategy {
    /**
     * 模型映射表，用於根據模型類型獲取對應的模型
     * 在初始化時會從 model-info.json 文件中讀取配置並加在保存到這個映射表中
     * key 為模型類型，value 為模型
     */
    private final Map<ModelType, Model> modelMap;

    /**
     * 初始化 SpeechRecognitionStrategy
     *
     * @param modelMap 模型映射表
     */
    public SpeechRecognitionStrategy(Map<ModelType, Model> modelMap) {
        this.modelMap = modelMap;
    }

    /**
     * 根據模型類型獲取對應的模型
     *
     * @param modelType 模型類型
     *
     * @return 對應的模型
     */
    public Model getModel(ModelType modelType) {
        Model model = modelMap.get(modelType);
        if (model == null) {
            log.error("無法找到指定的模型: {}", modelType);
            throw new IllegalArgumentException("無法找到指定的模型: " + modelType);
        }
        return model;
    }

    /**
     * 獲取所有可用的模型信息
     *
     * @return 所有可用的模型信息
     */
    public List<ModelInfoDTO> getAvailableModels() {
        return modelMap.keySet().stream().map(model -> new ModelInfoDTO(model.code(), model.description(), model.language())).toList();
    }
}
