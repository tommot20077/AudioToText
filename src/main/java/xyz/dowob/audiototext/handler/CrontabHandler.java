package xyz.dowob.audiototext.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.entity.Task;
import xyz.dowob.audiototext.service.TaskService;

import java.io.File;
import java.util.List;

/**
 * Crontab 任務處理器，處理定時任務
 *
 * @author yuan
 * @program AudioToText
 * @ClassName CrontabHandler
 * @description
 * @create 2024-12-25 00:18
 * @Version 1.0
 **/
@Log4j2
@Component
@RequiredArgsConstructor
public class CrontabHandler {
    /**
     * 任務服務類，用於操作任務的增刪改查
     */
    private final TaskService taskService;

    /**
     * 音檔配置類: 包含音檔的路徑、格式、閾值等設定
     */
    private final AudioProperties audioProperties;

    /**
     * 定時清理未完成任務，清理 1 小時後尚未完成的任務
     * 當前時間 - 任務創建時間 > 1 小時的任務將被清理，用於處理無法完成的任務
     * 每天凌晨 1 點執行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanCanNotFinishTasks () {
        List<Task> tasks = taskService.findAllCanNotFinishTasks(1);
        log.info("清理未完成任務: {}", tasks);
        tasks.forEach(taskService::deleteTaskStatus);
    }

    /**
     * 定時清理過期任務的下載檔案
     * 當前時間 - 任務結束時間 > 設定清理時間的任務將被清理
     * 清理時間設定於 application.yml 中的 {@link AudioProperties.Service} 中
     * 每小時執行一次
     */
    @Scheduled(cron = "0 0 */1 * * ?")
    public void cleanExpireTasks () {
        int expireTimeHour = audioProperties.getService().getOutputFileExpiredTime();
        if (expireTimeHour <= 0) {
            return;
        }
        List<Task> tasks = taskService.findAllByExpireTasks(expireTimeHour);
        log.info("清理過期任務: {}", tasks);
        File outputDirectory = new File(audioProperties.getPath().getOutputDirectory());
        File[] files = outputDirectory.listFiles();
        if (files == null) {
            return;
        }
        tasks.forEach(task -> {
            String taskId = task.getTaskId();
            for (File file : files) {
                if (file.getName().contains(taskId)) {
                    if (file.delete()) {
                        task.setDeleted(true);
                        taskService.saveTaskStatus(task);
                        log.info("刪除過期檔案: {}", file.getName());
                    } else {
                        log.error("刪除過期檔案失敗: {}", file.getName());
                    }
                }
            }
        });
    }

    /**
     * 定時檢查任務狀態，將失敗的任務標記為刪除
     * 每小時執行一次
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkTaskStatus () {
        List<Task> tasks = taskService.findAllFailAndNotDeletedTasks();
        log.info("檢查失敗任務狀態 {}", tasks);
        tasks.forEach(task -> {
            task.setDeleted(true);
            taskService.saveTaskStatus(task);
        });
    }
}
