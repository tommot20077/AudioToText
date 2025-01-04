package xyz.dowob.audiototext.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 有關於音檔處理、閾值的設定，並以audio為前綴配置在 application 中
 *
 * @author yuan
 * @program AudioToText
 * @ClassName AudioProperties
 * @description
 * @create 2024-12-12 15:22
 * @Version 1.0
 **/

@Data
@ConfigurationProperties(prefix = "audio")
@Configuration
public class AudioProperties {
    /**
     * 路徑設定: 設定音檔的暫存、模型、輸出等路徑
     */
    private Path path = new Path();

    /**
     * 規範處理音檔的格式: 包含取樣率、聲道、位元率
     */
    private StandardFormat standardFormat = new StandardFormat();

    /**
     * 閾值設定: 包含音檔處理的緩衝區大小、最大序列長度
     */
    private Threshold threshold = new Threshold();

    @Data
    public static class Path {
        /**
         * 處理音檔的暫存路徑
         */
        public String tempFileDirectory = "./temp/audio/";

        /**
         * 轉譯模型的路徑
         */
        public String modelDirectory = "./model/";

        /**
         * 需要使用的模型資訊的路徑
         */
        public String modelInfoPath = "./model/model-info.json";

        /**
         * 轉譯文字檔的路徑
         */
        public String outputDirectory = "./output/";
    }

    @Data
    public static class StandardFormat {
        /**
         * 音檔標準採樣率，單位為Hz 預設為 16000 (不建議更改)
         */
        public int sampleRate = 16000;

        /**
         * 音檔標準聲道 預設為 1 (不建議更改)
         */
        public int channel = 1;

        /**
         * 音檔標準位元率，單位為bps 預設為 16000 (不建議更改)
         */
        public int bitRate = 16000;
    }

    @Data
    public static class Threshold {
        /**
         * 音檔處理的緩衝區大小，單位為byte 預設為 2048
         */
        private int chunkBufferSize = 2048;

        /**
         * 音檔處理的最大序列長度 (超過此長度則會進行分割)，單位為byte 預設為 512
         */
        private int maxSequenceLength = 512;

        /**
         * 最大 Python 處理進程數量
         */
        private int maxPythonProcess = 3;

        /**
         * 最大處理時間，單位為秒 預設為 300
         */
        private int maxProcessingTime = 300;
    }
}
