package xyz.dowob.audiototext.type;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用於實現模型類型的紀錄類，用於保存模型類型的代碼、描述、語言等信息
 *
 * @author yuan
 * @program AudioToText
 * @ClassName ModelType
 * @create 2024-12-13 23:38
 * @Version 1.0
 **/
@Log4j2
public record ModelType(String code, String description, String language) {
    /**
     * 模型類型的映射表，用於根據代碼獲取對應的模型類型
     */
    private static final Map<String, ModelType> CODE_TO_MODEL_TYPE = new HashMap<>();

    /**
     * 根據代碼獲取模型類型
     *
     * @param code 代碼
     *
     * @return 模型類型
     *
     * @throws IllegalArgumentException 未知的模型類型
     */
    public static ModelType getModelTypeByCode(String code) {
        ModelType modelType = CODE_TO_MODEL_TYPE.get(code);
        if (modelType == null) {
            throw new IllegalArgumentException("未知的模型類型: " + code);
        }
        return modelType;
    }

    /**
     * 獲取模型類型的映射表
     *
     * @return 模型類型的映射表
     */
    public static Map<String, ModelType> getModelTypeMap() {
        return CODE_TO_MODEL_TYPE;
    }

    /**
     * 重寫 toString 方法，用於將模型類型轉換為字符串
     *
     * @return 模型類型的字符串表示
     */
    @Override
    public String toString() {
        HashMap<String, String> map = new HashMap<>();
        map.put("code", code);
        map.put("description", description);
        map.put("language", language);
        return map.toString();
    }

    /**
     * 重寫 equals 方法，用於判斷兩個模型類型是否相等
     *
     * @param obj 要比較的對象
     *
     * @return 兩個模型類型是否相等
     */
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

    /**
     * 重寫 hashCode 方法，用於計算模型類型的哈希碼
     *
     * @return 模型類型的哈希碼
     */
    @Override
    public int hashCode() {
        return code.hashCode();
    }

    /**
     * 用於初始化模型類型的組件，用於在應用啟動時初始化模型類型
     * 並將模型類型保存到 CODE_TO_MODEL_TYPE 中
     *
     * @author yuan
     * @program AudioToText
     * @ClassName ModelTypeInitializer
     * @create 2024-12-25 15:38
     * @Version 1.0
     */
    @Component
    @RequiredArgsConstructor
    public static class ModelTypeInitializer {
        /**
         * 模型類型的配置信息
         */
        private final List<ModelType> modelTypeConfigs;

        /**
         * 初始化模型類型，讀取模型訊息文件並保存到 CODE_TO_MODEL_TYPE 中
         */
        @PostConstruct
        public void init() {
            log.info("初始化模型類型, 共有{}個模型類型", modelTypeConfigs.size());
            for (ModelType config : modelTypeConfigs) {
                CODE_TO_MODEL_TYPE.put(config.code(), new ModelType(config.code(), config.description(), config.language()));
            }
        }
    }
}
