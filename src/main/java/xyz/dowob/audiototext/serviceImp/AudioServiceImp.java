package xyz.dowob.audiototext.serviceImp;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;
import ws.schild.jave.EncoderException;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.entity.TranscriptionSegment;
import xyz.dowob.audiototext.service.AudioService;
import xyz.dowob.audiototext.service.PreprocessingService;

import javax.sound.sampled.AudioFormat;
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

    private static final double SILENCE_THRESHOLD = 100.0;

    private static final int SILENCE_DURATION_MS = 500;

    private final PreprocessingService preprocessingService;

    private final AudioProperties audioProperties;

    private final Model model;

    /**
     * 將音訊檔案轉換成文字
     *
     * @param audioFile 音訊檔案
     *
     * @return 轉換後的文字
     */
    @Override
    public List<Map<String, Object>> audioToText(File audioFile, String taskId) throws EncoderException, IOException {
        File standardizedAudioFile = preprocessingService.standardizeAudio(audioFile, taskId);
        List<Map<String, Object>> result = convertTranscriptionSegments(transcribe(standardizedAudioFile));
        preprocessingService.deleteTempFile(taskId);
        return result;
    }

    private List<TranscriptionSegment> transcribe(File audioFile) {
        List<TranscriptionSegment> transcriptionSegments = new ArrayList<>();
        long currentTimeInSeconds = 0L;
        long lastProcessedTime = 0L;
        StringBuilder pendingText = new StringBuilder();


        try (AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat format = ais.getFormat();
            float frameRate = format.getFrameRate();
            int frameSize = format.getFrameSize();
            int channels = format.getChannels();
            int bitDepth = format.getSampleSizeInBits();

            Recognizer recognizer = new Recognizer(model, audioProperties.getStandardFormat().sampleRate);
            byte[] buffer = new byte[audioProperties.getThreshold().getChunkBufferSize()];
            int bytesRead;
            Long firstTextStartTime = null;

            while ((bytesRead = ais.read(buffer)) != -1) {
                long bufferTimeInSeconds = (long) (bytesRead / (frameRate * frameSize / 8.0 / channels / bitDepth));

                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    JSONObject result = new JSONObject(recognizer.getResult());
                    String text = result.getString("text").trim();

                    if (!text.isEmpty()) {
                        // 記錄第一個有效文本的開始時間
                        if (firstTextStartTime == null) {
                            firstTextStartTime = currentTimeInSeconds;
                        } //todo 這裡有問題，firstTextStartTime跟currentTimeInSeconds一直相同

                        // 添加到待處理文本
                        if (!pendingText.isEmpty()) {
                            pendingText.append(" ");
                        }
                        pendingText.append(text);

                        // 如果累積了足夠的文本或距離上一次處理已經過了一定時間
                        if (currentTimeInSeconds - lastProcessedTime >= 2.0 ||
                                pendingText.length() >= 100) {  // 可以調整這些閾值

                            TranscriptionSegment segment = createTranscriptionSegment(pendingText.toString(), firstTextStartTime, currentTimeInSeconds);

                            transcriptionSegments.add(segment);
                            // 重置狀態
                            pendingText.setLength(0);
                            lastProcessedTime = currentTimeInSeconds;
                            firstTextStartTime = null;
                        }
                    }
                }
                currentTimeInSeconds += bufferTimeInSeconds;
            }

            // 處理最後剩餘的文本
            if (!pendingText.isEmpty()) {
                String finalText = pendingText.toString();
                if (!finalText.trim().isEmpty()) {
                    TranscriptionSegment segment = createTranscriptionSegment(finalText, firstTextStartTime, currentTimeInSeconds);
                    transcriptionSegments.add(segment);
                }
            }

        } catch (Exception e) {
            log.error("音頻轉換失敗", e);
            throw new RuntimeException("音頻轉換失敗: " + e.getMessage(), e);
        }
        return transcriptionSegments;
    }


    private String formatTime(long milliseconds) {
        return String.format("%.3f", milliseconds / 1000.0);
    }

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
            String text, long segmentStartTime, long currentTimeInSeconds) {
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
