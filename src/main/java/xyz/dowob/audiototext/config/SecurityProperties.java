package xyz.dowob.audiototext.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 有關於安全設定的配置
 *
 * @author yuan
 * @program AudioToText
 * @ClassName SecurityProperties
 * @create 2025/3/27
 * @Version 1.0
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private Cors cors = new Cors();

    @Data
    public static class Cors {
        /**
         * 設定是否開啟跨域請求，默認為開啟，以下參數僅在開啟跨域請求時生效
         */
        private boolean cors = true;

        /**
         * 允許跨域請求的來源
         */
        private List<String> allowedOriginPatterns;

        /**
         * 允許跨域請求的方法
         */
        private List<String> allowedMethods;

        /**
         * 允許跨域請求的標頭
         */
        private List<String> allowedHeaders;

        /**
         * 是否允許跨域請求攜帶認證信息
         */
        private boolean allowCredentials = true;
    }
}
