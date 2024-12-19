package xyz.dowob.audiototext.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.dowob.audiototext.dto.ModelInfoDTO;
import xyz.dowob.audiototext.service.AudioService;
import xyz.dowob.audiototext.service.ProcessingService;
import xyz.dowob.audiototext.type.ModelType;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
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

    private final AudioService audioService;

    @PostMapping("/transcription")
    public ResponseEntity<?> transcribeAudio(@RequestParam("file") MultipartFile file, @RequestParam("model") String modelType) {

        try {
            if (file.isEmpty()) {
                return createResponseEntity(createErrorResponse("/api/transcription", "檔案為空", 400));
            } else if (modelType == null || modelType.isEmpty()) {
                return createResponseEntity(createErrorResponse("/api/transcription", "模型類型為空", 400));
            }
            ModelType type = ModelType.getModelTypeByCode(modelType);
            Object result = audioService.audioToText(file, type);
            return createResponseEntity(createSuccessResponse("/api/transcription", "轉換成功", result));
        } catch (Exception e) {
            log.error("轉換失敗: ", e);
            return createResponseEntity(createErrorResponse("/api/transcription", String.format("轉換失敗: %s", e.getMessage()), 400));
        }
    }

    @GetMapping("/getAvailableModels")
    public ResponseEntity<?> getAvailableModels() {
        List<ModelInfoDTO> availableModels = audioService.getAvailableModels();
        if (availableModels.isEmpty()) {
            return createResponseEntity(createSuccessResponse("/api/getAvailableModels", "無可用模型"));
        }
        return createResponseEntity(createSuccessResponse("/api/getAvailableModels", "取得可用模型成功", availableModels));
    }

}
