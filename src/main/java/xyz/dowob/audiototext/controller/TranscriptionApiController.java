package xyz.dowob.audiototext.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.dowob.audiototext.dto.ModelInfoDTO;
import xyz.dowob.audiototext.dto.TaskStatusDTO;
import xyz.dowob.audiototext.entity.Task;
import xyz.dowob.audiototext.service.AudioService;
import xyz.dowob.audiototext.service.TaskService;
import xyz.dowob.audiototext.type.ModelType;
import xyz.dowob.audiototext.type.OutputType;

import java.util.HashMap;
import java.util.List;

/**
 * 音訊轉文字的 API 控制器，處理音訊轉文字的請求
 * 提供了音訊轉文字的 API 接口
 * 實現 ApiController 接口，提供了一些常用的方法
 *
 * @author yuan
 * @program AudioToText
 * @ClassName TranscriptionApiController
 * @description
 * @create 2024-12-12 23:54
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Log4j2
public class TranscriptionApiController implements ApiController {

    /**
     * 音訊服務類，處理音訊轉文字的相關邏輯
     */
    private final AudioService audioService;

    /**
     * 任務服務類，處理任務的相關邏輯
     */
    private final TaskService taskService;


    /**
     * 音訊轉文字的 API 接口，接收音訊檔案和模型類型，返回音訊轉文字的結果
     *
     * @param file      音訊檔案
     * @param modelType 模型類型
     * @param isNeedSegment 是否需要分段
     * @param formatType 輸出格式類型
     * @param request   HTTP 請求
     *
     * @return 音訊轉文字的結果
     */
    @PostMapping("/transcription")
    public ResponseEntity<?> transcribeAudio (
            @RequestParam("file") MultipartFile file,
            @RequestParam("model") String modelType,
            @RequestParam(value = "is_need_segment", required = false, defaultValue = "true") boolean isNeedSegment,
            @RequestParam(value = "format_type") String formatType, HttpServletRequest request) {
        try {
            if (file.isEmpty()) {
                return createResponseEntity(createErrorResponse(request.getRequestURI(), "檔案為空", 400));
            } else if (modelType == null || modelType.isEmpty()) {
                return createResponseEntity(createErrorResponse(request.getRequestURI(), "模型類型為空", 400));
            }
            ModelType type = ModelType.getModelTypeByCode(modelType);
            OutputType outputType = OutputType.getOutputTypeByType(formatType);
            Object result = audioService.audioToText(file, type, outputType, isNeedSegment, request);
            return createResponseEntity(createSuccessResponse(request.getRequestURI(), "轉換請求成功", result));
        } catch (Exception e) {
            log.error("轉換失敗: ", e);
            return createResponseEntity(createErrorResponse(request.getRequestURI(), String.format("轉換失敗: %s", e.getMessage()), 400));
        }
    }

    /**
     * 取得可用的模型列表
     *
     * @param request HTTP 請求
     *
     * @return 可用的模型列表
     */
    @GetMapping("/getAvailableModels")
    public ResponseEntity<?> getAvailableModels (HttpServletRequest request) {
        List<ModelInfoDTO> availableModels = audioService.getAvailableModels();
        if (availableModels.isEmpty()) {
            return createResponseEntity(createSuccessResponse(request.getRequestURI(), "無可用模型"));
        }
        return createResponseEntity(createSuccessResponse(request.getRequestURI(), "取得可用模型成功", availableModels));
    }

    /**
     * 取得可用的轉換檔案格式的列表
     *
     * @param request HTTP 請求
     *
     * @return 可用的模型列表
     */
    @GetMapping("/getAvailableOutputTypes")
    public ResponseEntity<?> getAvailableOutputTypes (HttpServletRequest request) {
        List<OutputType> availableOutputTypes = audioService.getAvailableOutputTypes();
        if (availableOutputTypes.isEmpty()) {
            return createResponseEntity(createSuccessResponse(request.getRequestURI(), "無可用輸出格式"));
        }
        return createResponseEntity(createSuccessResponse(request.getRequestURI(), "取得可用輸出格式成功", availableOutputTypes));
    }

    /**
     * 取得任務狀態
     *
     * @param taskId  任務 ID
     * @param request HTTP 請求
     *
     * @return 任務狀態
     */
    @GetMapping("/getTaskStatus")
    public ResponseEntity<?> getTaskStatus(@RequestParam("task_id") String taskId, HttpServletRequest request) {
        try {
            HashMap<String, Object> result = new HashMap<>();
            Task task = taskService.findTaskByTaskId(taskId, TaskStatusDTO.Status.SUCCESS, TaskStatusDTO.Status.FAILED);

            if (task != null) {
                result.put("taskId", task.getTaskId());
                result.put("downloadUrl", task.getDownloadUrl());
                result.put("status", task.getStatus());
                result.put("result", task.getResult());
                return createResponseEntity(createSuccessResponse(request.getRequestURI(), "取得任務狀態成功", result));
            } else {
                TaskStatusDTO taskStatus = taskService.getTaskStatus(taskId).orElseThrow(() -> new RuntimeException("任務不存在"));
                return createResponseEntity(createSuccessResponse(request.getRequestURI(), "取得任務狀態成功", taskStatus));
            }
        } catch (Exception e) {
            log.error("取得任務狀態失敗: ", e);
            return createResponseEntity(createErrorResponse(request.getRequestURI(), String.format("取得任務狀態失敗: %s", e.getMessage()), 400));
        }
    }
}
