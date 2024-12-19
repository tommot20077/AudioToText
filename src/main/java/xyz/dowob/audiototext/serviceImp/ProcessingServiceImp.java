package xyz.dowob.audiototext.serviceImp;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.service.ProcessingService;
import xyz.dowob.audiototext.strategy.PunctuationStrategy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * @author yuan
 * @program AudioToText
 * @ClassName ProcessingServiceImp
 * @description
 * @create 2024-12-12 14:12
 * @Version 1.0
 **/
@Service
@Log4j2
@RequiredArgsConstructor
public class ProcessingServiceImp implements ProcessingService {
    private final AudioProperties audioProperties;
    private final PunctuationStrategy punctuationStrategy;

    /**
     * 儲存音訊檔案
     *
     * @param audioFile 音訊檔案
     * @param taskId    任務ID
     *
     * @return 儲存後的音訊檔案
     *
     * @throws IOException 檔案讀取、建立時錯誤
     */
    @Override
    public File saveAudio(MultipartFile audioFile, String taskId) throws IOException {
        File tempFileDirectory = Path.of(audioProperties.getPath().getTempFileDirectory()).toFile();
        if (!tempFileDirectory.exists() && !tempFileDirectory.mkdirs()) {
            log.error("無法建立暫存檔案目錄");
            throw new IOException("無法建立暫存檔案目錄");
        }
        File tempInputFile = File.createTempFile(String.format("%s_input_audio_", taskId), null, tempFileDirectory);
        audioFile.transferTo(tempInputFile);
        return tempInputFile;
    }

    /**
     * 轉換輸入的音訊檔案成可以被處理的格式
     *
     * @param tempAudioFile 音訊檔案
     * @param taskId    任務ID
     *
     * @return 處理後的音訊檔案
     *
     * @throws IOException      檔案讀取、建立時錯誤
     * @throws EncoderException 轉換音檔時編譯器錯誤
     */
    @Override
    public File standardizeAudio(File tempAudioFile, String taskId) throws IOException, EncoderException {
        File standardizeAudio = File.createTempFile(String.format("%s_standardize_", taskId), ".wav", Path.of(audioProperties.getPath().getTempFileDirectory()).toFile());
        EncodingAttributes encoderAttributes = getEncodingAttributes();
        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(tempAudioFile), standardizeAudio, encoderAttributes);

        return standardizeAudio;
    }

    private EncodingAttributes getEncodingAttributes() {
        EncodingAttributes encoderAttributes = new EncodingAttributes();
        AudioAttributes audioAttributes = new AudioAttributes();

        audioAttributes.setCodec("pcm_s16le");
        audioAttributes.setSamplingRate(audioProperties.getStandardFormat().getSampleRate());
        audioAttributes.setChannels(audioProperties.getStandardFormat().getChannel());
        audioAttributes.setBitRate(audioProperties.getStandardFormat().getBitRate());
        encoderAttributes.setAudioAttributes(audioAttributes);
        return encoderAttributes;
    }

    /**
     * 刪除伺服器上的暫存檔案
     *
     * @param taskId 任務ID
     */
    @Override
    public void deleteTempFile(String taskId) {
        File tempFileDirectory = Path.of(audioProperties.getPath().getTempFileDirectory()).toFile();
        File[] tempFiles = tempFileDirectory.listFiles((dir, name) -> name.contains(taskId));
        if (tempFiles != null) {
            for (File tempFile : tempFiles) {
                if (!tempFile.delete()) {
                    log.error("無法刪除暫存檔案: {}", tempFile.getName());
                }
                log.info("刪除暫存檔案: {}", tempFile.getName());
            }
        }
    }

    /**
     * 處理轉譯後的文字內容，將標點符號還原
     *
     * @param text 轉譯後的原始文字內容
     *
     * @return 復原標點符號後的文字內容
     */
    @Override
    public String punctuationRestore(String text) throws Exception {
        return punctuationStrategy.addPunctuation(text);
    }
}
