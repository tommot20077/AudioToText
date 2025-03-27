package xyz.dowob.audiototext.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 用於保存標點符號任務的數據傳輸對象 用於保存任務的 ID、文本、結果
 * 當前任務的結果取回方法 當取回方法完成時，可以獲取到任務的結果
 *
 * @author yuan
 * @program AudioToText
 * @ClassName PunctuationTaskDTO
 * @create 2025/1/3
 * @Version 1.0
 **/

@Getter
@Setter
@AllArgsConstructor
public class PunctuationTaskDTO {
    /**
     * 任務 ID
     */
    private String taskId;

    /**
     * 任務文本
     */
    private String text;

    /**
     * 任務的結果取回方法
     */
    private CompletableFuture<String> future;

    /**
     * 重寫 hashCode 和 equals 方法，用於比較任務 ID
     *
     * @return 任務 ID 的 hashCode
     */
    @Override
    public int hashCode () {
        return taskId.hashCode();
    }

    /**
     * 重寫 equals 方法，用於比較任務 ID
     *
     * @param obj 要比較的對象
     *
     * @return 是否相等
     */
    @Override
    public boolean equals (Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PunctuationTaskDTO that = (PunctuationTaskDTO) obj;
        return taskId.equals(that.taskId);
    }

    /**
     * 重寫 toString 方法，用於打印任務的信息
     *
     * @return 任務的信息
     */
    @Override
    public String toString () {
        Map<String, String> map = new HashMap<>();
        String truncatedResult = text != null && text.length() > 100 ? text.substring(0, 100) + "..." : text;
        map.put("taskId", taskId);
        map.put("text", truncatedResult);
        return map.toString();
    }

}
