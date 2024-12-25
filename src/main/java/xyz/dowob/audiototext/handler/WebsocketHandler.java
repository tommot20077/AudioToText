package xyz.dowob.audiototext.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import xyz.dowob.audiototext.dto.TaskStatusDTO;
import xyz.dowob.audiototext.service.TaskService;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 處理器，用於處理 WebSocket 的連接和訊息
 * 自訂義 TextWebSocketHandler 類，重寫 handleTextMessage 方法
 * 用於處理 WebSocket 的訊息，接收任務 ID，並將任務狀態信息發送給前端
 * 用於 WebSocket 的訊息通知，通知前端進行任務狀態的更新
 * 繼承 TextWebSocketHandler 類
 *
 * @author yuan
 * @program AudioToText
 * @ClassName WebsocketHandler
 * @description
 * @create 2024-12-21 12:38
 * @Version 1.0
 **/
@Log4j2
@Component
public class WebsocketHandler extends TextWebSocketHandler {
    /**
     * 任務服務類，用於操作任務的增刪改查
     */
    private final TaskService taskService;

    /**
     * Jackson ObjectMapper 類，用於將對象轉換為 JSON 字符串
     */
    private final ObjectMapper objectMapper;

    /**
     * WebSocket 連接 Session Map，用於保存 WebSocket 連接 Session
     * 其中 Key 為任務 ID，Value 為 WebSocket 連接 Session
     * 使用 ConcurrentHashMap 類，保證多線程安全
     */
    Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    /**
     * WebSocketHandler 構造方法
     *
     * @param taskService  任務服務類
     * @param objectMapper Jackson ObjectMapper 類
     */
    public WebsocketHandler(TaskService taskService, ObjectMapper objectMapper) {
        this.taskService = taskService;
        this.objectMapper = objectMapper;
    }

    /**
     * WebSocket 連接關閉時觸發
     * 將 Session 從 Session Map 中移除並關閉 Session
     *
     * @param session WebSocket 連接 Session
     *
     * @throws IOException 關閉 Session 時可能拋出的異常
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws IOException {
        sessionMap.values().remove(session);
        session.close();
    }

    /**
     * WebSocket 連接建立時觸發，用於初始化
     * 當 WebSocket 連接建立時，將 Session 加入 Session Map
     * 並發送任務狀態信息給前端
     *
     * @param session WebSocket 連接 Session
     */
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        String payload = message.getPayload();
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String taskId = jsonNode.get("taskId").asText();
            log.info("接收到任務ID: {}", taskId);
            TaskStatusDTO statusDTO = taskService
                    .getTaskStatus(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("任務ID: " + taskId + " 不存在"));
            sessionMap.put(taskId, session);
            sendTaskStatus(statusDTO);
        } catch (IOException e) {
            log.error("處理訊息失敗: {}", e.getMessage());
        }

    }

    /**
     * 發送任務狀態信息給前端
     * 通過 WebSocketSession 發送任務狀態信息給前端
     *
     * @param taskStatusDTO 任務狀態信息
     */
    public void sendTaskStatus(@NotNull TaskStatusDTO taskStatusDTO) {
        WebSocketSession session = sessionMap.get(taskStatusDTO.getTaskId());
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(taskStatusDTO)));
            }
        } catch (Exception e) {
            log.error("發送訊息失敗: {}", e.getMessage());
        }
    }
}