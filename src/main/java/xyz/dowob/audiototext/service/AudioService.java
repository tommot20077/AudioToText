package xyz.dowob.audiototext.service;

import ws.schild.jave.EncoderException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 用於規範有關音訊檔案轉換的介面
 * @author yuan
 * @program AudioToText
 * @ClassName AudioService
 * @description
 * @create 2024-12-12 14:48
 * @Version 1.0
 **/
public interface AudioService {
    /**
     * 將音訊檔案轉換成文字
     *
     * @param audioFile 音訊檔案
     *
     * @return 轉換後的文字
     */
    List<Map<String, Object>> audioToText(File audioFile, String taskId) throws EncoderException, IOException;

}