package xyz.dowob.audiototext.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.controller.ApiController;
import xyz.dowob.audiototext.dto.TaskStatusDTO;
import xyz.dowob.audiototext.entity.Task;
import xyz.dowob.audiototext.service.TaskService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 全局異常處理器，用於處理全局的異常，返回統一的異常信息
 * 實現 ApiController 接口，提供了一些常用的方法
 * @author yuan
 * @program AudioToText
 * @ClassName GlobalExceptionHandler
 * @create 2025/1/10
 * @Version 1.0
 **/

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ApiController {

    /**
     * 任務服務類，用於操作任務的增刪改查
     */
    private final TaskService taskService;

    /**
     * 音檔配置類: 包含音檔的路徑、格式、閾值等設定
     */
    private final AudioProperties audioProperties;

    /**
     * 日期時間格式化器
     */
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 正則表達式模式，用於匹配文件路徑
     */
    Pattern pattern = Pattern.compile("/files/|_output\\.[^/]+");

    /**
     * 全局異常處理器，處理 NoResourceFoundException 異常
     * 當請求的路徑不存在時，返回 404 狀態碼
     *
     * @param request HTTP 請求
     *
     * @return 響應實體
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoHandlerFoundException (HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/file")) {
            String taskId = pattern.matcher(requestURI).replaceAll("");
            Task task = taskService.findTaskByTaskId(taskId);
            if (task == null || task.getStatus() == TaskStatusDTO.Status.FAILED) {
                String errorMessage = String.format("沒有找到此任務的檔案: %s", taskId);
                return createResponseEntity(createErrorResponse(requestURI, errorMessage, 404));
            } else {
                LocalDateTime expireTime = task.getFinishTime().plusHours(audioProperties.getService().getOutputFileExpiredTime());
                String formatTime = expireTime.format(formatter);
                String errorMessage = String.format("此文件: %s 已在 %s 過期", taskId, formatTime);
                return createResponseEntity(createErrorResponse(requestURI, errorMessage, 404));
            }
        } else {
            return createResponseEntity(createErrorResponse(requestURI, "沒有找到請求的路徑", 404));
        }
    }

    /**
     * 全局異常處理器，處理 Exception 異常
     * 當發生異常時，返回 500 狀態碼
     * @param ex 異常
     * @param request HTTP 請求
     * @return 響應實體
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException (Exception ex, HttpServletRequest request) {
        return createResponseEntity(createErrorResponse(request.getRequestURI(), ex.getMessage()));
    }

}
