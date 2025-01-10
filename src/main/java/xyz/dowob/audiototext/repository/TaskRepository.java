package xyz.dowob.audiototext.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import xyz.dowob.audiototext.dto.TaskStatusDTO;
import xyz.dowob.audiototext.entity.Task;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任務數據庫操作接口，用於操作任務的增刪改查
 * 使用 JPA 框架，繼承 JpaRepository 接口
 *
 * @author yuan
 * @program AudioToText
 * @ClassName TaskRepository
 * @description
 * @create 2024-12-21 20:11
 * @Version 1.0
 **/
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {


    /**
     * 根據任務 ID 刪除任務
     *
     * @param taskId 任務 ID
     */
    void deleteByTaskId(String taskId);

    Task findByTaskId (String taskId);

    /**
     * 根據任務 ID 以及任務狀態 查詢任務
     *
     * @param taskId 任務 ID
     * @param statuses 任務狀態列表
     *
     * @return 任務 {@link Task}
     */
    @Query("SELECT t FROM Task t WHERE t.taskId = :taskId AND t.status IN :statuses")
    Task findByTaskIdAndStatuses(@Param("taskId") String taskId, @Param("statuses") List<TaskStatusDTO.Status> statuses);

    /**
     * 查詢所有未完成以及指定時間戳以前的任務
     *
     * @param status 任務狀態
     * @param time   指定時間
     *
     * @return List<Task> 任務列表 {@link Task}
     */
    @Query("SELECT t FROM Task t WHERE t.status = :status AND t.createTime < :time")
    List<Task> findAllCanNotFinishTasks(@Param("status") TaskStatusDTO.Status status, @Param("time") LocalDateTime time);

    /**
     * 根據指定時間戳查詢所有過期的任務
     *
     * @param time 時間
     *
     * @return List<Task> 任務列表 {@link Task}
     */
    @Query("SELECT t FROM Task t WHERE t.finishTime < :time and t.isDeleted = false")
    List<Task> findAllByExpireTasks(@Param("time") LocalDateTime time);

    @Query("SELECT t FROM Task t WHERE t.status = :status and t.isDeleted = false")
    List<Task> findAllNotDelete (@Param("status") TaskStatusDTO.Status status);
}
