package xyz.dowob.audiototext.config;

import lombok.Data;
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
        public String tempFileDirectory = "./temp/audio/";
        public String modelDirectory = "./model/";
        public String modelInfoPath = "./model/model-info.json";
    }

    @Data
    public static class StandardFormat {
        public int sampleRate = 16000;
        public int channel = 1;
        public int bitRate = 16000;
    }

    @Data
    public static class Threshold {
        private int chunkBufferSize = 2048;
    }
}
