package xyz.dowob.audiototext.serviceImp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.vosk.Recognizer;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.dto.ModelInfoDTO;
import xyz.dowob.audiototext.entity.TranscriptionSegment;
import xyz.dowob.audiototext.service.AudioService;
import xyz.dowob.audiototext.service.ProcessingService;
import xyz.dowob.audiototext.strategy.SpeechRecognitionStrategyStrategy;
import xyz.dowob.audiototext.type.ModelType;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.*;

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

    private final AudioProperties audioProperties;

    private final ObjectMapper objectMapper;

    private final SpeechRecognitionStrategyStrategy speechRecognitionStrategyStrategy;

    private final ProcessingService processingService;

    /**
     * 將音訊檔案轉換成文字
     *
     * @param audioFile 音訊檔案
     *
     * @return 轉換後的文字
     */
    @Override
    public Object audioToText(MultipartFile audioFile, ModelType type) {
        String taskId = UUID.randomUUID().toString();
        try {
            File tempInputFile = processingService.saveAudio(audioFile, taskId);
            log.debug("檔案上傳成功: {}", tempInputFile.getName());

            File standardizedAudioFile = processingService.standardizeAudio(tempInputFile, taskId);
            log.debug("音訊檔案標準化成功: {}", standardizedAudioFile.getName());

            Map<String, Object> result = new HashMap<>();
            Map<String, Object> convertMap = convertTranscriptionSegments(transcribe(standardizedAudioFile, type));
            log.debug("轉換成功: {}", convertMap);
            result.put("segments", convertMap.get("segments"));
            result.put("text", processingService.punctuationRestore(convertMap.get("text").toString()));
            log.debug("轉換成功: {}", result);
            return result;
        } catch (Exception e) {
            log.error("轉換失敗: ", e);
            throw new RuntimeException(e);
        } finally {
            processingService.deleteTempFile(taskId);
        }
    }

    /**
     * 取得目前可用的轉換模型列表
     */
    @Override
    public List<ModelInfoDTO> getAvailableModels() {
        return speechRecognitionStrategyStrategy.getAvailableModels();
    }

    private List<TranscriptionSegment> transcribe(File audioFile, ModelType type) {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
             Recognizer recognizer = new Recognizer(speechRecognitionStrategyStrategy.getModel(type),
                                                    audioProperties.getStandardFormat().sampleRate)) {
            recognizer.setWords(true);
            List<TranscriptionSegment> segments = new ArrayList<>();
            byte[] buffer = new byte[audioProperties.getThreshold().getChunkBufferSize()];
            int bytesRead;

            while ((bytesRead = audioInputStream.read(buffer)) != -1) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    addResultToSegments(segments, recognizer.getResult());
                }
            }
            addResultToSegments(segments, recognizer.getFinalResult());
            return segments;
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addResultToSegments(List<TranscriptionSegment> segments, String result) {
        if (result != null) {
            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(result);
                log.info("jsonNode: {}", jsonNode);
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

    @Deprecated
    private boolean isSignificantSilence(byte[] buffer, int bytesRead) {
        int threshold = 500;
        int sum = 0;
        for (int i = 0; i < bytesRead; i++) {
            sum += Math.abs(buffer[i]);
        }
        return (sum / bytesRead) < threshold;
    }

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
}
