package xyz.dowob.audiototext.service;

import org.springframework.web.multipart.MultipartFile;
import ws.schild.jave.EncoderException;

import java.io.File;
import java.io.IOException;

/**
 * 用於規範音訊檔案成可以被轉換以及優化處理內容的介面
 *
 * @author yuan
 * @program AudioToText
 * @ClassName ProcessingServiceImp
 * {@code @description}
 * {@code @create} 2024-12-12 14:07
 * {@code @Version} 1.0
 **/
public interface PreprocessingService {

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
    File saveAudio(MultipartFile audioFile, String taskId) throws IOException;

    /**
     * 轉換輸入的音訊檔案成可以被處理的格式
     *
     * @param audioFile 音訊檔案
     *
     * @return 處理後的音訊檔案
     */
    File standardizeAudio(File audioFile, String taskId) throws IOException, EncoderException;

    /**
     * 刪除伺服器上的暫存檔案
     *
     * @param taskId 任務ID
     */
    void deleteTempFile(String taskId);


}