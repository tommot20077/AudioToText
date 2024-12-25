package xyz.dowob.audiototext.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通用的 API 響應 DTO，用於返回 API 的響應
 *
 * @author yuan
 * @program AudioToText
 * @ClassName ApiResponseDTO
 * @description
 * @create 2024-12-13 21:41
 * @Version 1.0
 **/
@Data
public class ApiResponseDTO {
    /**
     * 響應的時間戳，格式為 yyyy-MM-dd'T'HH:mm:ss.SSSSSS
     * 使用 @JsonFormat 註解將 LocalDateTime 格式化為指定格式
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;

    /**
     * 響應的狀態碼
     */
    private int status;

    /**
     * 請求的路徑
     */
    private String path;

    /**
     * 響應的消息
     */
    private String message;

    /**
     * 響應的數據
     */
    private Object data;

    /**
     * 帶參數的構造方法
     *
     * @param timestamp 響應的時間戳
     * @param status    響應的狀態碼
     * @param path      請求的路徑
     * @param message   響應的消息
     * @param data      響應的數據
     */
    public ApiResponseDTO(LocalDateTime timestamp, int status, String path, String message, Object data) {
        this.timestamp = timestamp;
        this.status = status;
        this.path = path;
        this.message = message;
        this.data = data;
    }
}
