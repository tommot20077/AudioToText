package xyz.dowob.audiototext.listener;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import xyz.dowob.audiototext.event.TaskUpdateEvent;
import xyz.dowob.audiototext.handler.WebsocketHandler;

/**
 * 任務更新事件監聽器，用於監聽任務更新事件，並通知 WebSocketHandler 進行 WebSocket 通知
 * 實現 ApplicationListener 介面，監聽 TaskUpdateEvent 事件
 *
 * @author yuan
 * @program AudioToText
 * @ClassName TaskListener
 * @description
 * @create 2024-12-21 14:57
 * @Version 1.0
 **/
@Log4j2
@Component
@RequiredArgsConstructor
public class TaskListener implements ApplicationListener<TaskUpdateEvent> {
    /**
     * WebSocketHandler 類，用於進行 WebSocket 通知
     */
    private final WebsocketHandler websocketHandler;

    /**
     * @param event 任務更新事件
     */
    @Override
    public void onApplicationEvent(@NonNull TaskUpdateEvent event) {
        log.debug("接收到更新事件 : {}", event);
        websocketHandler.sendTaskStatus(event.getTaskStatusDTO(), null);
    }
}
