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

@Log4j2
@Component
public class DocxWriter implements FileWriter {
    private final AudioProperties audioProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocxWriter (AudioProperties audioProperties) {
        this.audioProperties = audioProperties;
    }

    @Override
    public OutputType getType () {
        return OutputType.DOCX;
    }

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

            document.createParagraph();


            JsonNode segments = rootNode.get("segments");
            if (segments == null) {
                return outputPath.toFile();
            }

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