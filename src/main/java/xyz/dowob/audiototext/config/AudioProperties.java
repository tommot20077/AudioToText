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
@Configuration
@ConfigurationProperties(prefix = "audio")
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

    /**
     * 服務設定: 包含是否開啟標點符號還原功能
     */
    private Service service = new Service();

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

        /**
         * 音檔處理的 Python 腳本路徑
         */
        public String pythonScriptPath = "./python/";
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
         * 最大 Python 處理進程數量，預設為 1
         */
        private int maxPythonProcess = 1;

        /**
         * 最大處理時間，單位為秒 預設為 300
         */
        private int maxProcessingTime = 300;

        /**
         * 最大任務列隊數量 預設為 100
         * 當任務數量大於{@link #maxPythonProcess}時，會將任務加入列隊等待處理，當列隊數量大於此值時，則會拒絕處理
         */
        private int maxTaskQueue = 100;
    }

    @Data
    public static class Service {
        /**
         * 是否開啟標點符號還原功能
         * 預設為 true
         */
        private boolean enablePunctuationRestoration = true;

        /**
         * 預設檔案的輸出格式
         */
        private String defaultOutputFormat = "pdf";

        /**
         * 輸出檔案保存時間，單位為小時 預設為 72
         * 當設定為0時，則不會刪除任何檔案
         * 超過此時間的檔案將會被刪除
         */
        private int outputFileExpiredTime = 72;
    }
}
