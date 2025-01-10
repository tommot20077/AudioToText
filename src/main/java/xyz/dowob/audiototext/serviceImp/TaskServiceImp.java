package xyz.dowob.audiototext.serviceImp;

import org.springframework.stereotype.Service;
import xyz.dowob.audiototext.dto.TaskStatusDTO;
import xyz.dowob.audiototext.entity.Task;
import xyz.dowob.audiototext.repository.TaskRepository;
import xyz.dowob.audiototext.service.TaskService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任務服務實現類，用於實現任務的增刪改查
 * 實現 TaskService 介面，規範任務的操作方法
 *
 * @author yuan
 * @program AudioToText
 * @ClassName TaskServiceImp
 * @description
 * @create 2024-12-21 20:15
 * @Version 1.0
 **/
@Service
public class TaskServiceImp implements TaskService {
    /**
     * 任務數據庫操作類，用於操作任務的數據庫操作
     */
    private final TaskRepository taskRepository;

    /**
     * 任務狀態 Map，用於保存任務的狀態信息
     * Key 為任務 ID，Value 為任務狀態 DTO
     * 使用 ConcurrentHashMap 類，保證多線程安全
     */
    private final Map<String, TaskStatusDTO> currentTaskMap = new ConcurrentHashMap<>();

    /**
     * TaskServiceImp 構造方法
     *
     * @param taskRepository 任務數據庫操作類
     */
    public TaskServiceImp(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * 根據任務 ID 查詢任務狀態
     *
     * @param taskId 任務 ID
     *
     * @return 任務狀態 DTO
     */
    @Override
    public Optional<TaskStatusDTO> getTaskStatus(String taskId) {
        return currentTaskMap.get(taskId) == null ? Optional.empty() : Optional.of(currentTaskMap.get(taskId));
    }

    /**
     * 更新任務狀態
     *
     * @param taskStatusDTO 任務狀態 DTO
     * @param isDelete      是否刪除
     */
    @Override
    public void updateTaskStatus(TaskStatusDTO taskStatusDTO, boolean isDelete) {
        if (isDelete) {
            currentTaskMap.remove(taskStatusDTO.getTaskId());
            return;
        }
        currentTaskMap.put(taskStatusDTO.getTaskId(), taskStatusDTO);
    }

    /**
     * 根據任務 ID 查詢任務
     *
     * @param taskId 任務 ID
     *
     * @return Task 任務
     */
    @Override
    public final Task findTaskByTaskId (String taskId, TaskStatusDTO.Status... status) {
        if (status.length == 0) {
            return taskRepository.findByTaskId(taskId);
        }
        List<TaskStatusDTO.Status> statuses = List.of(status);
        return taskRepository.findByTaskIdAndStatuses(taskId, statuses);
    }

    /**
     * 查詢所有未完成以及未刪除的任務
     *
     * @return List<Task> 任務列表
     */
    @Override
    public List<Task> findAllFailAndNotDeletedTasks () {
        return taskRepository.findAllNotDelete(TaskStatusDTO.Status.FAILED);
    }

    /**
     * 儲存任務狀態
     * @param task 任務
     */
    @Override
    public void saveTaskStatus(Task task) {
        taskRepository.save(task);
    }

    /**
     * 刪除任務狀態
     * @param task 任務
     */
    @Override
    public void deleteTaskStatus(Task task) {
        taskRepository.delete(task);
    }

    /**
     * 查詢所有未完成的任務，並且創建時間在指定時間之前
     *
     * @param minusHours 當前時間扣除指定小時數
     */
    @Override
    public List<Task> findAllCanNotFinishTasks(int minusHours) {
        LocalDateTime time = LocalDateTime.now().minusHours(minusHours);
        return taskRepository.findAllCanNotFinishTasks(TaskStatusDTO.Status.PROCESSING, time);
    }

    /**
     * 查詢所有過期的任務
     *
     * @param minusHours 當前時間扣除指定小時數
     */
    @Override
    public List<Task> findAllByExpireTasks(int minusHours) {
        LocalDateTime time = LocalDateTime.now().minusHours(minusHours);
        return taskRepository.findAllByExpireTasks(time);
    }

}
