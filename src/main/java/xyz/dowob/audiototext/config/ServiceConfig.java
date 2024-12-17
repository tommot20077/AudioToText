package xyz.dowob.audiototext.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vosk.Model;

import java.io.File;
import java.io.IOException;

/**
 * @author yuan
 * @program AudioToText
 * @ClassName ServiceConfig
 * @description
 * @create 2024-12-13 23:38
 * @Version 1.0
 **/
@Configuration
@RequiredArgsConstructor
public class ServiceConfig {
    private final AudioProperties audioProperties;

    @Bean
    public Model model() {
        if (!new File(audioProperties.getPath().getModelPath()).exists()) {
            throw new RuntimeException("請下載Vosk模型並放置於: " + splitPath(audioProperties.getPath().getModelPath()));
        }
        try {
            return new Model(audioProperties.getPath().getModelPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private String splitPath(String path) {
        if (path == null || !path.contains("/")) {
            throw new IllegalArgumentException("無效的路徑: " + path);
        }
        return path.substring(0, path.lastIndexOf("/"));
    }

}
