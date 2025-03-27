package xyz.dowob.audiototext.service;

import xyz.dowob.audiototext.dto.TaskStatusDTO;
import xyz.dowob.audiototext.entity.Task;

import java.util.List;
import java.util.Optional;

/**
 * 任務服務接口，用於操作任務的增刪改查
 * 規範了任務服務的操作方法
 *
 * @author yuan
 * @program AudioToText
 * @ClassName TaskService
 * @description
 * @create 2024-12-21 20:13
 * @Version 1.0
 **/
public interface TaskService {

    /**
     * 更新任務狀態
     *
     * @param taskStatusDTO 任務狀態
     * @param isDelete      是否刪除
     */
    void updateTaskStatus (TaskStatusDTO taskStatusDTO, boolean isDelete);

    /**
     * 儲存任務狀態
     *
     * @param task 任務
     */
    void saveTaskStatus (Task task);


    /**
     * 刪除任務狀態
     *
     * @param task 任務
     */
    void deleteTaskStatus (Task task);

    /**
     * 根據任務 ID 查詢任務
     * 並且可以指定任務狀態
     *
     * @param taskId 任務 ID
     * @param status 任務狀態
     *
     * @return Task 任務 {@link Task}
     */
    Task findTaskByTaskId (String taskId, TaskStatusDTO.Status... status);

    /**
     * 查詢所有未完成以及未刪除的任務
     *
     * @return List<Task> 任務列表 {@link Task}
     */
    List<Task> findAllFailAndNotDeletedTasks ();


    /**
     * 查詢所有未完成以及指定時間戳以前的任務
     *
     * @param minusHours 從現在時間減去的小時數
     *
     * @return List<Task> 任務列表 {@link Task}
     */
    List<Task> findAllCanNotFinishTasks (int minusHours);

    /**
     * 根據指定時間戳查詢所有過期的任務
     *
     * @param minusHours 從現在時間減去的小時數
     *
     * @return List<Task> 任務列表 {@link Task}
     */
    List<Task> findAllByExpireTasks (int minusHours);

    /**
     * 取得任務狀態
     *
     * @param taskId 任務ID
     */
    Optional<TaskStatusDTO> getTaskStatus (String taskId);
}
