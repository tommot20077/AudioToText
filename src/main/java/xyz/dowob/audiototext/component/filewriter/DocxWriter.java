package xyz.dowob.audiototext.component.filewriter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.type.OutputType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Microsoft Word 文檔寫入器，用於將結果寫入到 docx 文件中
 * 實現了 FileWriter 接口，規範了將處理後的文字內容輸出到 docx 文件的方法
 *
 * @author yuan
 * @program AudioToText
 * @ClassName DocxWriter
 * @create 2025/1/6
 * @Version 1.0
 */
@Log4j2
@Component
public class DocxWriter implements FileWriter {
    /**
     * 音檔處理的配置文件
     */
    private final AudioProperties audioProperties;

    /**
     * 物件映射器
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 用於構造 DocxWriter 對象
     *
     * @param audioProperties 音檔處理的配置文件
     */
    public DocxWriter (AudioProperties audioProperties) {
        this.audioProperties = audioProperties;
    }

    /**
     * 取得此類型的輸出類型
     *
     * @return 輸出類型 {@link OutputType}
     */
    @Override
    public OutputType getType () {
        return OutputType.DOCX;
    }

    /**
     * 將處理後的文字內容輸出到 docx 文件
     *
     * @param result 處理後的文字內容
     * @param taskId 任務ID
     *
     * @return 輸出的 docx 文件
     *
     * @throws IOException 檔案寫入時錯誤
     */
    @Override
    public File outputToFile (String result, String taskId) throws IOException {
        Path outputPath = Path.of(audioProperties.getPath().getOutputDirectory(), String.format("%s_output.docx", taskId));

        try (XWPFDocument document = new XWPFDocument(); FileOutputStream out = new FileOutputStream(outputPath.toFile())) {

            JsonNode rootNode = objectMapper.readTree(result);

            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("Transcription Result");
            titleRun.setBold(true);
            titleRun.setFontSize(20);

            XWPFParagraph fullText = document.createParagraph();
            XWPFRun fullTextRun = fullText.createRun();
            fullTextRun.setText(rootNode.get("text").asText());
            fullTextRun.setFontSize(12);

            JsonNode segments = rootNode.get("segments");
            if (segments == null) {
                return outputPath.toFile();
            }

            document.createParagraph();
            XWPFParagraph timelineTitle = document.createParagraph();
            timelineTitle.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun timelineTitleRun = timelineTitle.createRun();
            timelineTitleRun.setText("Segments Timeline");
            timelineTitleRun.setBold(true);
            timelineTitleRun.setFontSize(20);

            XWPFParagraph timelineSubtitle = document.createParagraph();
            XWPFRun timelineSubtitleRun = timelineSubtitle.createRun();
            timelineSubtitleRun.setText("(Start time ~ End time)");
            timelineSubtitleRun.setFontSize(12);

            for (JsonNode segment : segments) {
                XWPFParagraph timeInfo = document.createParagraph();
                XWPFRun timeInfoRun = timeInfo.createRun();
                timeInfoRun.setText(String.format("(%.2f ~ %.2f)",
                                                  segment.get("start_time").asDouble(),
                                                  segment.get("end_time").asDouble()
                ));
                timeInfoRun.setBold(true);
                timeInfoRun.setFontSize(11);

                XWPFParagraph segmentText = document.createParagraph();
                XWPFRun segmentTextRun = segmentText.createRun();
                segmentTextRun.setText(segment.get("text").asText());
                segmentTextRun.setFontSize(12);
            }

            document.write(out);
            return outputPath.toFile();
        }
    }
} 