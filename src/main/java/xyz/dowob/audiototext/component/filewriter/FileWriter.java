package xyz.dowob.audiototext.component.filewriter;

import xyz.dowob.audiototext.type.OutputType;

import java.io.File;
import java.io.IOException;

/**
 * 將處理後的文字內容輸出到檔案
 * 此類實現了FileWriter接口，規範了將處理後的文字內容輸出到檔案的方法
 *
 * @author yuan
 * @program PunctuationRestoration.py
 * @ClassName OutputFileStrategy
 * @create 2025/1/6
 * @Version 1.0
 **/

public interface FileWriter {
    /**
     * 取得輸出類型
     *
     * @return 輸出類型
     */
    OutputType getType ();

    /**
     * 將處理後的文字內容輸出到檔案
     *
     * @param result 處理後的文字內容
     * @param taskId 任務ID
     *
     * @throws IOException 檔案寫入時錯誤
     */
    File outputToFile (String result, String taskId) throws IOException;
}
