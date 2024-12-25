package xyz.dowob.audiototext.controller;

import org.springframework.http.ResponseEntity;
import xyz.dowob.audiototext.dto.ApiResponseDTO;

import java.time.LocalDateTime;

/**
 * 通用的 Controller 接口，提供了一些常用的方法，用於生成 API 的響應
 *
 * @author yuan
 * @program AudioToText
 * @ClassName ApiController
 * @description
 * @create 2024-12-13 21:40
 * @Version 1.0
 **/
public interface ApiController {
    /**
     * 創建成功的 API 響應，此方法為主要方法，可以根據需要自定義返回的數據
     *
     * @param path    請求的路徑
     * @param message 響應的消息
     * @param data    響應的數據
     *
     * @return API 響應 DTO {@link ApiResponseDTO}
     */
    default ApiResponseDTO createSuccessResponse(String path, String message, Object data) {
        return new ApiResponseDTO(LocalDateTime.now(), 200, path, message, data);
    }

    /**
     * 創建成功的 API 響應，不帶數據
     *
     * @param path    請求的路徑
     * @param message 響應的消息
     *
     * @return API 響應 DTO {@link ApiResponseDTO}
     */
    default ApiResponseDTO createSuccessResponse(String path, String message) {
        return createSuccessResponse(path, message, null);
    }

    /**
     * 創建失敗的 API 響應，此方法為主要方法，可以根據需要自定義返回的數據
     *
     * @param path    請求的路徑
     * @param message 響應的消息
     * @param status  響應的狀態碼
     *
     * @return API 響應 DTO {@link ApiResponseDTO}
     */
    default ApiResponseDTO createErrorResponse(String path, String message, int status) {
        return new ApiResponseDTO(LocalDateTime.now(), status, path, message, null);
    }

    /**
     * 創建失敗的 API 響應，狀態碼默認為 500
     *
     * @param path    請求的路徑
     * @param message 響應的消息
     *
     * @return API 響應 DTO {@link ApiResponseDTO}
     */
    default ApiResponseDTO createErrorResponse(String path, String message) {
        return createErrorResponse(path, message, 500);
    }


    /**
     * 根據 API 響應 DTO 創建 ResponseEntity
     *
     * @param response API 響應 DTO
     *
     * @return ResponseEntity 控制器返回的響應
     */
    default ResponseEntity<?> createResponseEntity(ApiResponseDTO response) {
        return ResponseEntity.status(response.getStatus()).body(response);
    }


}
