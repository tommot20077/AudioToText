package xyz.dowob.audiototext.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import xyz.dowob.audiototext.dto.TaskStatusDTO;

/**
 * 任務更新事件，用於通知 WebSocketHandler 進行 WebSocket 通知
 * 包含任務的狀態信息，用於通知前端進行更新
 * 任務的狀態信息包含任務 ID、任務狀態、進度、結果等
 * 繼承 ApplicationEvent 事件類，使用 TaskStatusDTO 作為事件的數據
 *
 * @author yuan
 * @program AudioToText
 * @ClassName TaskUpdateEvent
 * @description
 * @create 2024-12-21 14:58
 * @Version 1.0
 **/
@Getter
public class TaskUpdateEvent extends ApplicationEvent {
    /**
     * 任務的狀態信息 {@link TaskStatusDTO}
     */
    private final TaskStatusDTO taskStatusDTO;

    /**
     * 任務更新事件構造方法
     *
     * @param source        事件源
     * @param taskStatusDTO 任務的狀態信息
     */
    public TaskUpdateEvent(Object source, TaskStatusDTO taskStatusDTO) {
        super(source);
        this.taskStatusDTO = taskStatusDTO;
    }
}
