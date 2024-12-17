package xyz.dowob.audiototext.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
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
    private Path path = new Path();
    private StandardFormat standardFormat = new StandardFormat();
    private Threshold threshold = new Threshold();

    @Data
    public static class Path {
        public String tempFilePath = "./temp/audio/";
        public String modelPath = "./model/vosk-model-en-us-0.22";
    }

    @Data
    public static class StandardFormat {
        public String format = "wav";
        public float sampleRate = 16000.0f;
        public int channel = 1;
        public int bitRate = 16;
    }

    @Data
    public static class Threshold {
        private int chunkBufferSize = 2048;
        private double silenceThreshold = 100.0;
        private int silenceDurationMs = 500;
        private int minSegmentDurationMs = 1000;
        private int maxSegmentDurationMs = 10000;
    }
}
