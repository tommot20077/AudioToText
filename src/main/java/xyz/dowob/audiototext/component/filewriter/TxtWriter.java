package xyz.dowob.audiototext.component.filewriter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.type.OutputType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 將處理後的文字內容輸出到txt檔案
 * 此類實現了FileWriter接口，規範了將處理後的文字內容輸出到txt檔案的方法
 *
 * @author yuan
 * @program PunctuationRestoration.py
 * @ClassName TxtWriter
 * @create 2025/1/6
 * @Version 1.0
 **/
@Component
@Log4j2
public class TxtWriter implements FileWriter {
    private final AudioProperties audioProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TxtWriter (AudioProperties audioProperties) {
        this.audioProperties = audioProperties;
    }

    /**
     * 取得輸出類型
     *
     * @return 輸出類型
     */
    @Override
    public OutputType getType () {
        return OutputType.TXT;
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
    public File outputToFile (String result, String taskId) throws IOException {
        Path outputPath = Path.of(audioProperties.getPath().getOutputDirectory(), String.format("%s_output.txt", taskId));
        try (BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(outputPath.toFile()))) {
            JsonNode rootNode = objectMapper.readTree(result);

            writer.write("Transcription Result");
            writer.write("\n\n");
            writer.write(rootNode.get("text").asText());
            writer.write("\n\n");

            JsonNode segments = rootNode.get("segments");
            if (segments == null) {
                return outputPath.toFile();
            }

            writer.write("Segments Timeline");
            writer.write("\n");
            writer.write("(Start time ~ End time)");
            writer.write("\n\n");


            for (JsonNode segment : segments) {

                String timeInfo = String.format("(%.2f ~ %.2f)", segment.get("start_time").asDouble(), segment.get("end_time").asDouble());
                writer.write(timeInfo);
                writer.write("\n");
                writer.write(segment.get("text").asText());
                writer.write("\n\n");
            }
            return outputPath.toFile();
        }
    }
}
