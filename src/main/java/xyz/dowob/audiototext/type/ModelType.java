package xyz.dowob.audiototext.type;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import xyz.dowob.audiototext.strategy.DeepPunctuationStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yuan
 * @program AudioToText
 * @ClassName ModelType
 * @create 2024-12-13 23:38
 * @Version 1.0
 **/
@Log4j2
public record ModelType(String code, String description, String language) {
    private static final Map<String, ModelType> CODE_TO_MODEL_TYPE = new HashMap<>();

    @Component
    @RequiredArgsConstructor
    public static class ModelTypeInitializer {
        private final List<ModelType> modelTypeConfigs;

        @PostConstruct
        public void init() {
            log.info("初始化模型類型, 共有{}個模型類型", modelTypeConfigs.size());
            for (ModelType config : modelTypeConfigs) {
                CODE_TO_MODEL_TYPE.put(config.code(), new ModelType(config.code(), config.description(), config.language()));
            }
        }
    }

    public static ModelType getModelTypeByCode(String code) {
        ModelType modelType = CODE_TO_MODEL_TYPE.get(code);
        if (modelType == null) {
            throw new IllegalArgumentException("未知的模型類型: " + code);
        }
        return modelType;
    }

    public static Map<String, ModelType> getModelTypeMap() {
        return CODE_TO_MODEL_TYPE;
    }

    @Override
    public String toString() {
        HashMap<String, String> map = new HashMap<>();
        map.put("code", code);
        map.put("description", description);
        map.put("language", language);
        return map.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ModelType modelType = (ModelType) obj;
        return code.equals(modelType.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
