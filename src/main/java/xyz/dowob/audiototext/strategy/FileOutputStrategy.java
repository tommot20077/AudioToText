package xyz.dowob.audiototext.strategy;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import xyz.dowob.audiototext.component.filewriter.FileWriter;
import xyz.dowob.audiototext.config.AudioProperties;
import xyz.dowob.audiototext.type.OutputType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用於規範檔案輸出策略的介面，規範檔案輸出策略的實作
 * 透過指定的格式類型來決定使用哪一種檔案輸出策略
 *
 * @author yuan
 * @program PunctuationRestoration.py
 * @ClassName FileOutputStrategy
 * @create 2025/1/6
 * @Version 1.0
 **/
@Log4j2
@Component
public class FileOutputStrategy {
    /**
     * 獲取檔案預設的輸出格式的設定類
     */
    private final AudioProperties audioProperties;
    /**
     * 用檔案輸出策略的映射表，用於根據輸出類型獲取對應的檔案輸出策略
     */
    Map<OutputType, FileWriter> fileWriterMap = new HashMap<>();

    /**
     * 初始化 FileOutputStrategy，並將檔案輸出策略列表和檔案輸出格式的設定類注入
     *
     * @param fileWriters     檔案輸出策略列表
     * @param audioProperties 檔案輸出格式的設定類
     */
    public FileOutputStrategy (List<FileWriter> fileWriters, AudioProperties audioProperties) {
        this.audioProperties = audioProperties;
        for (FileWriter fileWriter : fileWriters) {
            log.info("初始化檔案輸出策略：{}", fileWriter.getType());
            fileWriterMap.put(fileWriter.getType(), fileWriter);
        }
    }

    /**
     * 根據輸出類型獲取對應的檔案輸出策略
     * 當輸出類型為null時，則返回默認的檔案輸出策略
     *
     * @param type 輸出類型
     *
     * @return 檔案輸出策略
     *
     * @throws RuntimeException 沒有檔案輸出方法可使用
     */
    public FileWriter getFileWriter (OutputType type) {
        if (fileWriterMap.isEmpty()) {
            throw new RuntimeException("沒有檔案輸出方法可使用");
        } else if (type == null) {
            OutputType defaultType = OutputType.getOutputTypeByType(audioProperties.getService().getDefaultOutputFormat());
            return fileWriterMap.get(defaultType);
        }
        return fileWriterMap.get(type);
    }

    /**
     * 獲取目前可用的輸出格式列表
     *
     * @return 輸出格式列表
     */
    public List<OutputType> getAvailableOutputTypes () {
        return fileWriterMap.keySet().stream().toList();
    }
}
