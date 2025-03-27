package xyz.dowob.audiototext.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.dowob.audiototext.component.filter.CorsFilter;

/**
 * 此類用於設定請求的連接方式以及跨域請求的設定
 * 實現 WebMvcConfigurer 用於設定請求的連接方式
 *
 * @author yuan
 * @program AudioToText
 * @ClassName WebConfig
 * @create 2025/1/7
 * @Version 1.0
 **/
@Configuration
public class WebConfig implements WebMvcConfigurer {
    /**
     * 安全設定
     */
    private final SecurityProperties securityProperties;

    /**
     * 用於構造 WebConfig 對象
     *
     * @param securityProperties 安全設定
     */
    public WebConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        FilterRegistrationBean<CorsFilter> registrationBean = new FilterRegistrationBean<>();
        CorsFilter corsFilter = new CorsFilter(securityProperties);
        registrationBean.setFilter(corsFilter);
        registrationBean.addUrlPatterns("/**");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }
}
