package xyz.dowob.audiototext.strategy;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.vosk.Model;
import xyz.dowob.audiototext.dto.ModelInfoDTO;
import xyz.dowob.audiototext.type.ModelType;

import java.util.List;
import java.util.Map;

/**
 * @author yuan
 * @program AudioToText
 * @ClassName SpeechRecognitionStrategyStrategy
 * @description
 * @create 2024-12-17 16:26
 * @Version 1.0
 **/
@Component
@Log4j2
public class SpeechRecognitionStrategyStrategy {
    private final Map<ModelType, Model> modelMap;

    public SpeechRecognitionStrategyStrategy(Map<ModelType, Model> modelMap) {
        this.modelMap = modelMap;
    }

    public Model getModel(ModelType modelType) {
        Model model = modelMap.get(modelType);
        if (model == null) {
            log.error("無法找到指定的模型: {}", modelType);
            throw new IllegalArgumentException("無法找到指定的模型: " + modelType);
        }
        return model;
    }

    public List<ModelInfoDTO> getAvailableModels() {
        return modelMap
                .keySet()
                .stream()
                .map(model -> new ModelInfoDTO(model.code(), model.description(), model.language()))
                .toList();
    }
}
