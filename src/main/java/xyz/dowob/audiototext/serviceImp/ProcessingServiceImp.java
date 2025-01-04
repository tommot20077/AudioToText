package xyz.dowob.audiototext.serviceImp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
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
import xyz.dowob.audiototext.provider.PythonServiceProvider;
import xyz.dowob.audiototext.service.ProcessingService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * 用於實現音訊檔案成可以被轉換以及優化處理內容的實現類
 * 實現 ProcessingService 介面，實現音訊檔案的儲存、轉換、處理、輸出等功能
 *
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
    /**
     * 音訊的配置信息
     */
    private final AudioProperties audioProperties;

    /**
     * Jackson ObjectMapper 類，用於將對象轉換為 JSON 字符串
     */
    private final ObjectMapper objectMapper;

    /**
     * PythonServiceInitializer 類，用於初始化 Python 服務
     */
    private final PythonServiceProvider pythonProvider;

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
     * @param taskId        任務ID
     *
     * @return 處理後的音訊檔案
     *
     * @throws IOException      檔案讀取、建立時錯誤
     * @throws EncoderException 轉換音檔時編譯器錯誤
     */
    @Override
    public File standardizeAudio(File tempAudioFile, String taskId) throws IOException, EncoderException {
        File standardizeAudio = File.createTempFile(String.format("%s_standardize_", taskId),
                                                    ".wav",
                                                    Path.of(audioProperties.getPath().getTempFileDirectory()).toFile());
        EncodingAttributes encoderAttributes = getEncodingAttributes();
        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(tempAudioFile), standardizeAudio, encoderAttributes);

        return standardizeAudio;
    }

    /**
     * 取得音訊編碼屬性，其中包含音訊編碼的相關設定
     * 根據AudioProperties中的標準格式設定，設定音訊編碼的相關屬性
     * {@link AudioProperties.StandardFormat}
     *
     * @return 音訊編碼屬性
     */
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
    public String punctuationRestore (String text, String taskId) throws Exception {
        return pythonProvider.getPunctuationResult(text, taskId);
    }

    /**
     * 將處理後的文字內容輸出到檔案
     *
     * @param result 處理後的文字內容
     * @param taskId 任務ID
     *
     * @throws IOException 檔案寫入時錯誤
     */
    @Override
    public void outputToFile(String result, String taskId) throws IOException {
        Document document = new Document();
        Path outputPath = Path.of(audioProperties.getPath().getOutputDirectory(), String.format("%s_output.pdf", taskId));
        try (OutputStream ops = new FileOutputStream(outputPath.toFile())) {
            PdfWriter.getInstance(document, ops);
            document.open();
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font timeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

            JsonNode rootNode = objectMapper.readTree(result);

            generateTitle("Transcription Result", document);


            Paragraph fullText = new Paragraph(rootNode.get("text").asText(), normalFont);
            document.add(fullText);
            document.add(new Paragraph("\n\n"));


            generateTitle("Segments Timeline", document);
            document.add(new Paragraph("(Start time ~ End time)", normalFont));

            JsonNode segments = rootNode.get("segments");
            for (JsonNode segment : segments) {
                String timeInfo = String.format("(%.2f ~ %.2f)", segment.get("start_time").asDouble(), segment.get("end_time").asDouble());
                Paragraph segmentText = new Paragraph(timeInfo, timeFont);
                document.add(segmentText);

                Paragraph segmentContent = new Paragraph(segment.get("text").asText(), normalFont);
                document.add(segmentContent);

                if (!segment.equals(segments.get(segments.size() - 1))) {
                    document.add(new Paragraph("\n"));
                }
            }

            document.close();
        } catch (DocumentException e) {
            log.error("檔案寫入錯誤: ", e);
            throw new IOException(e);
        }
    }

    /**
     * 生成標題
     *
     * @param title    標題
     * @param document PDF 文檔
     *
     * @throws DocumentException 文檔處理錯誤
     */
    private void generateTitle(String title, Document document) throws DocumentException {
        Paragraph segmentsTextTitle = new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA, 20, Font.BOLD));
        segmentsTextTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(segmentsTextTitle);
        document.add(new Paragraph("\n"));
    }

    /**
     * 將處理後的文字內容輸出成 JSON 格式
     * 此方法為默認實現，若處理後的文字內容無法轉換成 JSON 格式，則直接返回 toString() 結果
     *
     * @param result 處理後的文字內容
     *
     * @return 處理後的文字內容 JSON 格式
     */
    @Override
    public String formatToJson(Object result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("格式化結果錯誤: ", e);
            return result.toString();
        }
    }
}
