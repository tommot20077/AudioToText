package xyz.dowob.audiototext.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import xyz.dowob.audiototext.service.AudioService;
import xyz.dowob.audiototext.service.PreprocessingService;

import java.io.File;
import java.util.List;
import java.util.Map;
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

    private final PreprocessingService preprocessingService;

    @RequestMapping("/transcription")
    public ResponseEntity<?> transcribeAudio(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return createResponseEntity(createErrorResponse("/api/transcription", "檔案為空", 400));
        }
        String taskId = UUID.randomUUID().toString();
        try {
            File tempInputFile = preprocessingService.saveAudio(file, taskId);
            log.info("檔案上傳成功: {}", tempInputFile.getName());
            List<Map<String, Object>> result = audioService.audioToText(tempInputFile, taskId);
            log.info("轉換成功: {}", result);
            return createResponseEntity(createSuccessResponse("/api/transcription", "轉換成功", result));
        } catch (Exception e) {
            log.error("轉換失敗: ", e);
            preprocessingService.deleteTempFile(taskId);
            return createResponseEntity(createErrorResponse("/api/transcription", String.format("轉換失敗: %s", e.getMessage()), 400));
        }
    }
}
