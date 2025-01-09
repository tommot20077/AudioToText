package xyz.dowob.audiototext.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 此類用於設定請求的連接方式以及跨域請求的設定
 *
 * @author yuan
 * @program AudioToText
 * @ClassName WebConfig
 * @create 2025/1/7
 * @Version 1.0
 **/
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings (CorsRegistry registry) {
        registry.addMapping("/**").allowedOriginPatterns("*") //todo 之後要改成前端的網址
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowedHeaders("*").allowCredentials(true);
    }
}
