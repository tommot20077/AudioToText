package xyz.dowob.audiototext.serviceImp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.vosk.Recognizer;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.dto.ModelInfoDTO;
import xyz.dowob.audiototext.entity.TranscriptionSegment;
import xyz.dowob.audiototext.service.AudioService;
import xyz.dowob.audiototext.strategy.SpeechRecognitionStrategyStrategy;
import xyz.dowob.audiototext.type.ModelType;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * 將音訊檔案轉換成文字
     *
     * @param audioFile 音訊檔案
     *
     * @return 轉換後的文字
     */
    @Override
    public List<Map<String, Object>> audioToText(File audioFile, File standardizedAudioFile, ModelType type) {
        return convertTranscriptionSegments(transcribe(standardizedAudioFile, type));
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

    private List<Map<String, Object>> convertTranscriptionSegments(List<TranscriptionSegment> transcriptionSegments) {
        List<Map<String, Object>> segments = new ArrayList<>();
        for (TranscriptionSegment segment : transcriptionSegments) {
            if (segment != null) {
                Map<String, Object> segmentMap = new HashMap<>();
                segmentMap.put("text", segment.getText());
                segmentMap.put("start_time", segment.getStartTime());
                segmentMap.put("end_time", segment.getEndTime());
                segments.add(segmentMap);
            }
        }
        return segments;
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
