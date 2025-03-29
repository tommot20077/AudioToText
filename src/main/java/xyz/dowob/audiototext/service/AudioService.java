package xyz.dowob.audiototext.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import ws.schild.jave.EncoderException;
import xyz.dowob.audiototext.dto.ModelInfoDTO;
import xyz.dowob.audiototext.type.ModelType;
import xyz.dowob.audiototext.type.OutputType;

import java.io.IOException;
import java.util.List;

/**
 * 用於規範有關音訊檔案轉換的介面
 *
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
     * @param file 音訊檔案
     *
     * @return 轉換後的物件
     */
    Object audioToText(MultipartFile file, ModelType modelType, OutputType outputType, boolean isNeedSegment, HttpServletRequest request) throws EncoderException, IOException;

    /**
     * 取得目前可用的轉換模型列表
     *
     * @return 可用的轉換模型列表
     */
    List<ModelInfoDTO> getAvailableModels();

    /**
     * 取得目前可用的輸出格式列表
     *
     * @return 可用的輸出格式列表
     */
    List<OutputType> getAvailableOutputTypes();
}
