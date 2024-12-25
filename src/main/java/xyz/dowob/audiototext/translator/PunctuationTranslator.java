package xyz.dowob.audiototext.translator;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

/**
 * 用於實現模型添加標點符號的轉換器
 * 實現 Translator 介面，實現添加標點符號的功能
 * 利用 HuggingFaceTokenizer 將模型的輸出轉換為文本
 * 模型使用 DeepPunctuationStrategy 策略模式
 *
 * @author yuan
 * @program AudioToText
 * @ClassName PunctuationTranslator
 * @description
 * @create 2024-12-18 14:14
 * @Version 1.0
 **/
@Log4j2
@RequiredArgsConstructor
public class PunctuationTranslator implements Translator<String, String> {

    private final HuggingFaceTokenizer tokenizer;

    /**
     * @param ctx    轉譯的上下文
     * @param ndList 模型的輸出矩陣
     *
     * @return 處理後的輸出
     *
     * @throws RuntimeException 處理模型輸出時發生錯誤
     */
    @Override
    public String processOutput(TranslatorContext ctx, NDList ndList) {
        try {
            NDArray logits = ndList.getFirst();
            NDArray predictions = logits.argMax(2);
            long[] predictedIndices = predictions.get(0).toLongArray();

            return tokenizer.decode(predictedIndices, true);
        } catch (Exception e) {
            throw new RuntimeException("處理模型輸出時發生錯誤", e);
        }
    }

    /**
     * @param ctx   轉譯的上下文
     * @param input 轉譯文字輸入
     *
     * @return 處理後的輸入
     */
    // todo 轉譯模型輸入格式尚未匹配，待修正
    @Override
    public NDList processInput(TranslatorContext ctx, String input) {
        Encoding encoding = tokenizer.encode(input);
        long[] inputIds = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();

        log.debug("原始輸入大小: {}", inputIds.length);

        final int maxSequenceLength = 512;

        if (inputIds.length > maxSequenceLength) {
            inputIds = Arrays.copyOf(inputIds, maxSequenceLength);
            attentionMask = Arrays.copyOf(attentionMask, maxSequenceLength);
            log.warn("輸入被截斷至最大長度: {}", maxSequenceLength);
        }

        if (inputIds.length < maxSequenceLength) {
            long[] paddedInputIds = new long[maxSequenceLength];
            long[] paddedAttentionMask = new long[maxSequenceLength];
            System.arraycopy(inputIds, 0, paddedInputIds, 0, inputIds.length);
            System.arraycopy(attentionMask, 0, paddedAttentionMask, 0, attentionMask.length);

            Arrays.fill(paddedInputIds, inputIds.length, maxSequenceLength, 0);
            Arrays.fill(paddedAttentionMask, attentionMask.length, maxSequenceLength, 0);

            inputIds = paddedInputIds;
            attentionMask = paddedAttentionMask;
        }

        log.debug("填充後大小: {}", inputIds.length);

        NDManager manager = ctx.getNDManager();
        NDArray inputIdsArray = manager.create(inputIds).reshape(1, maxSequenceLength);
        NDArray attentionArray = manager.create(attentionMask).reshape(1, maxSequenceLength);

        log.debug("最終形狀: inputIds={}, attentionMask={}", inputIdsArray.getShape(), attentionArray.getShape());

        log.debug("前10個token: {}", Arrays.toString(Arrays.copyOfRange(inputIds, 0, 10)));

        return new NDList(inputIdsArray, attentionArray);
    }
}
