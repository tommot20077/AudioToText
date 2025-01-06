package xyz.dowob.audiototext.component.filewriter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.type.OutputType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * 將處理後的文字內容輸出到pdf檔案
 * 此類實現了FileWriter接口，規範了將處理後的文字內容輸出到pdf檔案的方法
 *
 * @author yuan
 * @program PunctuationRestoration.py
 * @ClassName PdfWriter
 * @create 2025/1/6
 * @Version 1.0
 **/
@Log4j2
@Component
public class PdfWriter implements FileWriter {
    private final AudioProperties audioProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PdfWriter (AudioProperties audioProperties) {
        this.audioProperties = audioProperties;
    }

    /**
     * 生成標題
     *
     * @param title    標題
     * @param document PDF 文檔
     *
     * @throws DocumentException 文檔處理錯誤
     */
    private void generateTitle (String title, Document document) throws DocumentException {
        Paragraph segmentsTextTitle = new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA, 20, Font.BOLD));
        segmentsTextTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(segmentsTextTitle);
        document.add(new Paragraph("\n"));
    }

    /**
     * 取得輸出類型
     *
     * @return 輸出類型
     */
    public OutputType getType () {
        return OutputType.PDF;
    }

    /**
     * 將處理後的文字內容輸出到檔案
     *
     * @param result 處理後的文字內容
     * @param taskId 任務ID
     *
     * @throws IOException 檔案寫入時錯誤
     */
    public File outputToFile (String result, String taskId) throws IOException {
        Document document = new Document();
        Path outputPath = Path.of(audioProperties.getPath().getOutputDirectory(), String.format("%s_output.pdf", taskId));
        try (OutputStream ops = new FileOutputStream(outputPath.toFile())) {
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, ops);
            document.open();
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font timeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

            JsonNode rootNode = objectMapper.readTree(result);

            generateTitle("Transcription Result", document);


            Paragraph fullText = new Paragraph(rootNode.get("text").asText(), normalFont);
            document.add(fullText);
            document.add(new Paragraph("\n\n"));

            JsonNode segments = rootNode.get("segments");
            if (segments == null) {
                document.close();
                return outputPath.toFile();
            }

            generateTitle("Segments Timeline", document);
            document.add(new Paragraph("(Start time ~ End time)", normalFont));

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
            return outputPath.toFile();
        } catch (DocumentException e) {
            log.error("檔案寫入錯誤: ", e);
            throw new IOException(e);
        }
    }
}
