package xyz.dowob.audiototext.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import xyz.dowob.audiototext.dto.TaskStatusDTO;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * 伺服器任務實體，用於保存任務的相關信息
 * 用於保存任務的 ID、狀態、結果、下載地址、創建時間、完成時間
 * 使用 JPA 標註，將實體映射到數據庫中
 *
 * @author yuan
 * @program AudioToText
 * @ClassName Task
 * @description
 * @create 2024-12-21 19:45
 * @Version 1.0
 **/
@Getter
@Setter
@Entity
@NoArgsConstructor
public class Task {
    /**
     * 任務 ID，主鍵，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * 任務 ID，唯一，不可為空
     */
    @Column(name = "task_id",
            nullable = false,
            unique = true)
    private String taskId;

    /**
     * 任務狀態: 使用 {@link TaskStatusDTO.Status} 列舉類型 ，不可為空
     */
    @Column(name = "status",
            nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatusDTO.Status status;

    /**
     * 任務結果，使用 MEDIUMTEXT 類型保存(最大 16MB, 16*1024*1024)
     */
    @Column(name = "result",
            columnDefinition = "MEDIUMTEXT")
    private String result;

    /**
     * 下載地址，保存結果的下載地址
     */
    @Column(name = "download_url")
    private String downloadUrl;

    /**
     * 創建時間，不可為空
     */
    @Column(name = "create_time",
            nullable = false)
    private LocalDateTime createTime;

    /**
     * 完成時間
     */
    @Column(name = "finish_time")
    private LocalDateTime finishTime;


    /**
     * 在持久化之前，設置創建時間
     */
    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
    }

    /**
     * 重寫 toString 方法，返回任務的相關信息
     *
     * @return 任務的相關信息
     */
    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("taskId", taskId);
        map.put("status", status);
        map.put("downloadUrl", downloadUrl);
        map.put("createTime", createTime);
        map.put("finishTime", finishTime);

        String truncatedResult = result != null && result.length() > 100 ? result.substring(0, 100) + "..." : result;
        map.put("result", truncatedResult);
        return map.toString();
    }

    /**
     * 重寫 equals 方法，比較任務的 ID 是否相同
     *
     * @param obj 比較對象
     *
     * @return 任務 ID 是否相同
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Task task) {
            return taskId.equals(task.taskId);
        }
        return false;
    }

}
