package xyz.dowob.audiototext.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import xyz.dowob.audiototext.entity.Task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * 任務狀態 DTO，用於返回任務的狀態，用於前端查詢任務的狀態
 *
 * @author yuan
 * @program AudioToText
 * @ClassName TaskStatusDTO
 * @description
 * @create 2024-12-21 12:18
 * @Version 1.0
 **/
@Setter
@Getter
public class TaskStatusDTO {
    /**
     * 任務 ID
     */
    private String taskId;

    /**
     * 任務進度，默認為 0.0
     */
    private BigDecimal progress = BigDecimal.valueOf(0.0);

    /**
     * 任務狀態，默認為處理中
     */
    private Status status = Status.PROCESSING;

    /**
     * 任務結果
     */
    private Object result;


    /**
     * 帶參數的構造方法
     *
     * @param taskId 任務 ID
     */
    public TaskStatusDTO(String taskId) {
        this.taskId = taskId;
    }

    /**
     * 重寫 toString 方法，返回任務的狀態
     *
     * @return 任務的狀態
     */
    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("taskId", taskId);
        map.put("progress", progress);
        map.put("status", status);
        map.put("result", result);
        return map.toString();
    }

    /**
     * 重寫 equals 方法，比較兩個 TaskStatusDTO 對象是否相等
     *
     * @param obj 要比較的對象
     *
     * @return 如果兩個 TaskStatusDTO 對象的 taskId 相等，則返回 true，否則返回 false
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TaskStatusDTO taskStatusDTO) {
            return taskId.equals(taskStatusDTO.taskId);
        }
        return false;
    }

    /**
     * 重寫 hashCode 方法，返回 TaskStatusDTO 對象的 hashCode
     *
     * @return TaskStatusDTO 對象的 hashCode
     */
    @Override
    public int hashCode() {
        return taskId.hashCode();
    }

    /**
     * 將 TaskStatusDTO 對象轉換為 Task 對象
     *
     * @param isFinish 是否完成任務
     *
     * @return Task 對象
     */
    public Task toTask(boolean isFinish) {
        Task task = new Task();
        task.setStatus(status);
        task.setTaskId(taskId);
        if (result != null) {
            task.setResult(result.toString());
        }
        if (isFinish) {
            task.setFinishTime(LocalDateTime.now());
        }
        return task;
    }

    /**
     * 任務狀態枚舉，包含處理中、成功、失敗
     * 用於標記任務的狀態
     */
    @Getter
    @AllArgsConstructor
    public enum Status {
        PROCESSING("處理中"),
        SUCCESS("成功"),
        FAILED("失敗");

        private final String status;
    }
}
