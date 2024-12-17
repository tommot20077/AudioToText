package xyz.dowob.audiototext.controller;

import org.springframework.http.ResponseEntity;
import xyz.dowob.audiototext.dto.ApiResponseDTO;

import java.time.LocalDateTime;

/**
 * @author yuan
 * @program AudioToText
 * @ClassName ApiController
 * @description
 * @create 2024-12-13 21:40
 * @Version 1.0
 **/
public interface ApiController {
    default ApiResponseDTO createSuccessResponse(String path, String message, Object data) {
        return new ApiResponseDTO(LocalDateTime.now(), 200, path, message, data);
    }

    default ApiResponseDTO createSuccessResponse(String path, String message) {
        return new ApiResponseDTO(LocalDateTime.now(), 200, path, message, null);
    }

    default ApiResponseDTO createErrorResponse(String path, String message) {
        return new ApiResponseDTO(LocalDateTime.now(), 500, path, message, null);
    }

    default ApiResponseDTO createErrorResponse(String path, String message, Object data) {
        return new ApiResponseDTO(LocalDateTime.now(), 500, path, message, data);
    }

    default ApiResponseDTO createErrorResponse(String path, String message, int status) {
        return new ApiResponseDTO(LocalDateTime.now(), status, path, message, null);
    }

    default ApiResponseDTO createErrorResponse(String path, String message, int status, Object data) {
        return new ApiResponseDTO(LocalDateTime.now(), status, path, message, data);
    }

    default ResponseEntity<?> createResponseEntity(ApiResponseDTO response) {
        return ResponseEntity.status(response.getStatus()).body(response);
    }


}
