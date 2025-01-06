package xyz.dowob.audiototext.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * 輸出檔案類型，目前支援txt、doc、pdf
 * 並根據支援的檔案格式使用不同的策略進行輸出
 * {@link xyz.dowob.audiototext.component.filewriter.FileWriter}
 *
 * @author yuan
 * @program PunctuationRestoration.py
 * @ClassName OutputType
 * @create 2025/1/6
 * @Version 1.0
 **/
@Log4j2
@Getter
@AllArgsConstructor
public enum OutputType {
    TXT("txt"),
    DOCX("docx"),
    PDF("pdf");

    /**
     * 檔案類型
     */
    private final String type;

    /**
     * 根據檔案類型獲取對應的輸出類型，若無對應則返回null
     *
     * @param type 檔案類型
     *
     * @return 輸出類型
     */
    public static OutputType getOutputTypeByType (String type) {
        if (type == null) {
            return null;
        }

        String normalizedType = type.replace("\"", "").trim().toLowerCase();
        for (OutputType outputType : OutputType.values()) {
            if (outputType.getType().equalsIgnoreCase(normalizedType)) {
                return outputType;
            }
        }
        return null;
    }

}
