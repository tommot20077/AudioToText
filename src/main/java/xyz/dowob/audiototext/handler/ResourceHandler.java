package xyz.dowob.audiototext.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.dowob.audiototext.config.AudioProperties;

/**
 * 資源處理器，用於處理靜態資源的請求，提供下載文字檔案的功能
 * 實現 WebMvcConfigurer 介面，重寫 addResourceHandlers 方法
 * 依賴於 serviceConfig 類對象，用於獲取音訊的配置信息
 *
 * @author yuan
 * @program AudioToText
 * @ClassName ResourceHandler
 * @description
 * @create 2024-12-24 23:09
 * @Version 1.0
 **/
@Configuration
@DependsOn("serviceConfig")
@RequiredArgsConstructor
@Log4j2
public class ResourceHandler implements WebMvcConfigurer {
    /**
     * 音訊的配置信息
     */
    private final AudioProperties audioProperties;

    /**
     * 重寫 addResourceHandlers 方法，用於添加資源處理器，提供靜態資源的請求
     * 設置 /files/** 的請求映射到音訊的輸出目錄，此配置設定於 {@link AudioProperties} 中
     *
     * @param registry 資源處理器註冊器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = audioProperties.getPath().getOutputDirectory();
        registry.addResourceHandler("/files/**").addResourceLocations(String.format("file:%s", path));
    }

}
