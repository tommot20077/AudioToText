package xyz.dowob.audiototext.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import xyz.dowob.audiototext.handler.WebsocketHandler;

/**
 * WebSocket 設定類，處理 WebSocket 相關處理邏輯
 * 實現 WebSocketConfigurer 接口，註冊 WebSocketHandler
 *
 * @author yuan
 * @program AudioToText
 * @ClassName WebSocketConfig
 * @description
 * @create 2024-12-21 12:39
 * @Version 1.0
 **/
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    /**
     * 自定義的 WebSocketHandler，用於處理音訊轉文字的 WebSocket 請求
     * 發送有關處理進度、結果的訊息
     */
    public final WebsocketHandler websocketHandler;

    /**
     * 註冊 WebSocketHandler，設定 WebSocket 的路徑
     * 允許所有來源的請求並支援 SockJS
     *
     * @param registry WebSocket 處理器註冊器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(websocketHandler, "/ws/task").setAllowedOrigins("*");
        registry.addHandler(websocketHandler, "/sockjs/task").setAllowedOrigins("*").withSockJS();
    }
}
