package xyz.dowob.audiototext.strategy;

/**
 * 用於規範標點符號還原的介面
 * @author yuan
 * @program AudioToText
 * @ClassName PunctuationStrategy
 * @description
 * @create 2024-12-18 12:25
 * @Version 1.0
 **/
public interface PunctuationStrategy {
    /**
     * 添加標點符號到文本中
     * @param text 原始文本
     * @return 添加標點符號後的文本
     * @throws Exception 添加標點符號時發生錯誤
     */
    String addPunctuation(String text) throws Exception;

    /**
     * 取得模型名稱
     * @return 模型名稱
     */
    String getModelName();

}
