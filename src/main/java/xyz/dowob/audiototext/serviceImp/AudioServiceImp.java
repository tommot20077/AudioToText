package xyz.dowob.audiototext.serviceImp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.vosk.Recognizer;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.dto.ModelInfoDTO;
import xyz.dowob.audiototext.dto.TaskStatusDTO;
import xyz.dowob.audiototext.entity.Task;
import xyz.dowob.audiototext.entity.TranscriptionSegment;
import xyz.dowob.audiototext.event.TaskUpdateEvent;
import xyz.dowob.audiototext.service.AudioService;
import xyz.dowob.audiototext.service.ProcessingService;
import xyz.dowob.audiototext.service.TaskService;
import xyz.dowob.audiototext.strategy.SpeechRecognitionStrategy;
import xyz.dowob.audiototext.type.ModelType;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 實現音檔處理的具體方法，實現AudioService接口
 *
 * @author yuan
 * @program AudioToText
 * @ClassName AudioServiceImp
 * @description
 * @create 2024-12-12 14:51
 * @Version 1.0
 **/
@Service
@RequiredArgsConstructor
@Log4j2
public class AudioServiceImp implements AudioService {

    /**
     * 音訊的配置類
     */
    private final AudioProperties audioProperties;

    /**
     * Jackson ObjectMapper 類，用於將對象轉換為 JSON 字符串
     */
    private final ObjectMapper objectMapper;

    /**
     * 音訊轉換策略
     */
    private final SpeechRecognitionStrategy speechRecognitionStrategy;

    /**
     * 音訊處理服務類
     */
    private final ProcessingService processingService;

    /**
     * 任務服務類
     */
    private final TaskService taskService;

    /**
     * 事件發布者
     */
    private final ApplicationEventPublisher publisher;


    /**
     * 將音訊檔案轉換成文字
     * 預先回傳任務ID，並進行非同步處理，可利用WebSocket進行任務狀態的更新
     * 利用 CompletableFuture 非同步處理，提高處理效率
     * 先行將音訊檔案上傳至伺服器，再將音訊檔案標準化，最後進行音訊轉換
     * 轉換成功後，將結果進行格式化，並生成 PDF 檔案
     * 最後更新任務狀態，通知前端進行任務狀態的更新
     *
     * @param audioFile 音訊檔案
     *
     * @return 任務ID
     */
    @Override
    public Object audioToText(MultipartFile audioFile, ModelType type, HttpServletRequest request) {
        String taskId = UUID.randomUUID().toString();
        try {
            File tempInputFile = processingService.saveAudio(audioFile, taskId);
            log.debug("檔案上傳成功: {}", tempInputFile.getName());

            File standardizedAudioFile = processingService.standardizeAudio(tempInputFile, taskId);
            log.debug("音訊檔案標準化成功: {}", standardizedAudioFile.getName());

            String downloadUrl = generateFileUrl(request, taskId);

            TaskStatusDTO taskStatusDTO = new TaskStatusDTO(taskId);
            Task task = taskStatusDTO.toTask(false);
            taskService.updateTaskStatus(taskStatusDTO, false);
            taskService.saveTaskStatus(task);

            Map<String, Object> result = new HashMap<>();
            CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> convertMap = convertTranscriptionSegments(transcribe(standardizedAudioFile,
                                                                                             type,
                                                                                             taskStatusDTO,
                                                                                             downloadUrl));

                    result.put("segments", convertMap.get("segments"));
                    result.put("text", processingService.punctuationRestore(convertMap.get("text").toString(), taskId));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    log.error("轉換失敗: ", throwable);
                    task.setStatus(TaskStatusDTO.Status.FAILED);
                    task.setResult(throwable.getMessage());
                } else {
                    String formatResult = processingService.formatToJson(result);
                    task.setStatus(TaskStatusDTO.Status.SUCCESS);
                    task.setResult(formatResult);
                    try {
                        processingService.outputToFile(formatResult, taskId);
                    } catch (Exception e) {
                        log.error("生成 PDF 失敗: ", e);
                        task.setStatus(TaskStatusDTO.Status.FAILED);
                        task.setResult("PDF 生成失敗: " + e.getMessage());
                    }
                    task.setDownloadUrl(downloadUrl);
                }
                taskService.updateTaskStatus(taskStatusDTO, true);
                processingService.deleteTempFile(taskId);

                task.setFinishTime(LocalDateTime.now());
                taskService.saveTaskStatus(task);
                log.debug("已清理任務: {}", taskId);
            });
            return Map.of("taskId", taskId);
        } catch (Exception e) {
            log.error("轉換失敗: ", e);
            throw new RuntimeException(e);
        }
    }


    /**
     * 取得目前可用的轉換模型列表
     */
    @Override
    public List<ModelInfoDTO> getAvailableModels() {
        return speechRecognitionStrategy.getAvailableModels();
    }

    /**
     * 將音訊檔案轉換成文字
     * 使用 Vosk 進行音訊轉換，引入 AudioProperties 配置類，設定音訊檔案的格式、路徑、閾值等
     * 使用 Recognizer 進行音訊轉換，設定模型類型、音訊檔案的取樣率
     * 當片段轉換成功時，將結果加入到 TranscriptionSegment 中
     * 最後更新任務狀態，發送任務狀態更新事件 {@link TaskUpdateEvent}
     *
     * @param audioFile 標準化音訊檔案
     * @param type      模型類型
     * @param task      任務狀態
     *
     * @return 轉換後的文字內容
     *
     * @throws RuntimeException 音訊檔案轉換失敗時拋出異常
     */
    private List<TranscriptionSegment> transcribe(File audioFile, ModelType type, TaskStatusDTO task, String downloadUrl) {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
             Recognizer recognizer = new Recognizer(speechRecognitionStrategy.getModel(type),
                                                    audioProperties.getStandardFormat().sampleRate)) {
            recognizer.setWords(true);
            List<TranscriptionSegment> segments = new ArrayList<>();
            byte[] buffer = new byte[audioProperties.getThreshold().getChunkBufferSize()];
            long totalBytes = audioFile.length();
            long processedBytes = 0;
            int bytesRead;

            while ((bytesRead = audioInputStream.read(buffer)) != -1) {
                processedBytes += bytesRead;
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    addResultToSegments(segments, recognizer.getResult());
                    updateProgressAndNotify(task, (double) processedBytes * 100 / totalBytes);
                }
            }
            addResultToSegments(segments, recognizer.getFinalResult());

            HashMap<String, Object> result = new HashMap<>();
            result.put("segments", segments);
            result.put("downloadUrl", downloadUrl);
            updateProgressAndNotify(task, 100.0, TaskStatusDTO.Status.SUCCESS, result);
            return segments;
        } catch (Exception e) {
            log.error("音訊檔案轉譯失敗: ", e);
            updateProgressAndNotify(task, 0.0, TaskStatusDTO.Status.FAILED, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 更新任務狀態，並發送任務狀態更新事件，通知前端進行任務狀態的更新
     *
     * @param task     任務狀態
     * @param progress 進度
     * @param status   狀態
     * @param result   結果
     */
    private void updateProgressAndNotify(TaskStatusDTO task, Double progress, TaskStatusDTO.Status status, Object result) {
        if (status != null) {
            task.setStatus(status);
        }
        if (result != null) {
            task.setResult(result);
        }
        if (progress != null && progress >= 0 && progress <= 100) {
            BigDecimal progressBigDecimal = BigDecimal.valueOf(progress).setScale(2, RoundingMode.HALF_UP);
            task.setProgress(progressBigDecimal);
        }
        publisher.publishEvent(new TaskUpdateEvent(this, task));
    }

    /**
     * 更新任務狀態，並發送任務狀態更新事件，通知前端進行任務狀態的更新
     *
     * @param task     任務狀態
     * @param progress 進度
     */
    private void updateProgressAndNotify(TaskStatusDTO task, double progress) {
        updateProgressAndNotify(task, progress, null, null);
    }

    /**
     * 更新任務狀態，並發送任務狀態更新事件，通知前端進行任務狀態的更新
     *
     * @param task   任務狀態
     * @param result 解析結果
     */
    private void updateProgressAndNotify(TaskStatusDTO task, String result) {
        updateProgressAndNotify(task, null, null, result);
    }

    /**
     * 更新任務狀態，並發送任務狀態更新事件，通知前端進行任務狀態的更新
     *
     * @param segments 解析分段字句
     * @param result   解析結果
     */
    private void addResultToSegments(List<TranscriptionSegment> segments, String result) {
        if (result != null) {
            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(result);
                if (!jsonNode.isEmpty() && jsonNode.has("result") && !jsonNode.get("result").isEmpty()) {
                    JsonNode words = jsonNode.get("result");
                    segments.add(createTranscriptionSegment(jsonNode.get("text").asText(),
                                                            (words.get(0).get("start").asDouble()),
                                                            (words.get(words.size() - 1).get("end").asDouble())));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 判斷是否為有效的靜音
     * 當音訊緩衝區的平均值小於閾值時，判斷為有效的靜音
     * 此閾值設定於 {@link AudioProperties#getThreshold()}
     *
     * @param buffer    音訊緩衝區
     * @param bytesRead 讀取的位元組數
     *
     * @return 是否為有效的靜音
     */
    @Deprecated
    private boolean isSignificantSilence(byte[] buffer, int bytesRead) {
        int threshold = 500;
        int sum = 0;
        for (int i = 0; i < bytesRead; i++) {
            sum += Math.abs(buffer[i]);
        }
        return (sum / bytesRead) < threshold;
    }

    /**
     * 將解析分段字句轉換成 Map 格式，並加入完整的文字內容
     *
     * @param transcriptionSegments 解析分段字句
     *
     * @return Map 格式的解析分段字句
     */
    private Map<String, Object> convertTranscriptionSegments(List<TranscriptionSegment> transcriptionSegments) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> segments = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (TranscriptionSegment segment : transcriptionSegments) {
            if (segment != null) {
                Map<String, Object> segmentMap = new HashMap<>();
                segmentMap.put("text", segment.getText());
                segmentMap.put("start_time", segment.getStartTime());
                segmentMap.put("end_time", segment.getEndTime());
                segments.add(segmentMap);
                text.append(segment.getText()).append(" ");
            }
        }
        result.put("segments", segments);
        result.put("text", text.toString());
        return result;
    }


    /**
     * 創建解析分段字句
     *
     * @param text                 文字內容
     * @param segmentStartTime     分段開始時間
     * @param currentTimeInSeconds 當前時間
     *
     * @return 解析分段字句
     */
    private TranscriptionSegment createTranscriptionSegment(
            String text, double segmentStartTime, double currentTimeInSeconds) {
        if (!text.isEmpty()) {
            TranscriptionSegment transcriptionSegment = new TranscriptionSegment();
            transcriptionSegment.setText(text);
            transcriptionSegment.setStartTime(segmentStartTime);
            transcriptionSegment.setEndTime(currentTimeInSeconds);
            return transcriptionSegment;
        }
        return null;
    }

    /**
     * 生成檔案下載的 URL
     *
     * @param request 請求
     * @param taskId  任務ID
     *
     * @return 檔案下載的 URL
     */
    private String generateFileUrl(HttpServletRequest request, String taskId) {
        String serverHost = request.getServerName();
        int serverPort = request.getServerPort();
        String scheme = "http".equals(request.getScheme()) ? "http" : "https";

        return scheme + "://" + serverHost + ":" + serverPort + "/files/" + taskId + "_output.pdf";
    }
}
