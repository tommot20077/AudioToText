package xyz.dowob.audiototext.translator;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import xyz.dowob.audiototext.config.AudioProperties;

import java.util.Arrays;
import java.util.Map;

/**
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

    private final AudioProperties audioProperties;

    /**
     * @param ctx
     * @param ndList
     *
     * @return
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
     * @param ctx
     * @param input
     *
     * @return
     */
    @Override
    public NDList processInput(TranslatorContext ctx, String input) {
        // 使用 tokenizer 对输入进行编码
        Encoding encoding = tokenizer.encode(input);
        long[] inputIds = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();

        log.debug("原始輸入大小: {}", inputIds.length);

        // 設定最大序列長度
        final int maxSequenceLength = 512; // 根据模型支持的最大长度调整

        // 如果輸入長度超過最大序列長度，進行截斷
        if (inputIds.length > maxSequenceLength) {
            inputIds = Arrays.copyOf(inputIds, maxSequenceLength);
            attentionMask = Arrays.copyOf(attentionMask, maxSequenceLength);
            log.warn("輸入被截斷至最大長度: {}", maxSequenceLength);
        }

        // 如果輸入不足最大序列長度，則填充至指定長度
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

        // 創建 NDArray
        NDManager manager = ctx.getNDManager();
        NDArray inputIdsArray = manager.create(inputIds).reshape(1, maxSequenceLength);
        NDArray attentionArray = manager.create(attentionMask).reshape(1, maxSequenceLength);

        log.debug("最終形狀: inputIds={}, attentionMask={}", inputIdsArray.getShape(), attentionArray.getShape());

        // 打印前10個token用于调试
        log.debug("前10個token: {}", Arrays.toString(Arrays.copyOfRange(inputIds, 0, 10)));

        // 返回 NDList
        return new NDList(inputIdsArray, attentionArray);
    }
}
