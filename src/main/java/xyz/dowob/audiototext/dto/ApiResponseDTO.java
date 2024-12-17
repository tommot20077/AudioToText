package xyz.dowob.audiototext.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * @author yuan
 * @program AudioToText
 * @ClassName ApiResponseDTO
 * @description
 * @create 2024-12-13 21:41
 * @Version 1.0
 **/
@Data
@Builder
public class ApiResponseDTO {
    @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;

    private int status;

    private String path;

    private String message;

    private Object data;

    public ApiResponseDTO(LocalDateTime timestamp, int status, String path, String message, Object data) {
        this.timestamp = timestamp;
        this.status = status;
        this.path = path;
        this.message = message;
        this.data = data;
    }
}
