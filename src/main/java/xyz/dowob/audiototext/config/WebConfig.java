package xyz.dowob.audiototext.config;

import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
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
@EnableWebSecurity
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

    /**
     * 過濾器鏈設定
     * 1. 關閉 CSRF
     * 2. 添加自定義跨域過濾器 {@link CorsFilter}於原有的 CsrfFilter 之前
     * 3. 設定所有請求均可訪問
     *
     * @param http HttpSecurity
     *
     * @return SecurityFilterChain
     *
     * @throws Exception 錯誤
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new CorsFilter(securityProperties), CsrfFilter.class)
                .authorizeHttpRequests((authorize) -> authorize.anyRequest().permitAll())
                .build();
    }

    /**
     * 設定跨域請求
     * 1. 如果未開啟跨域請求，則設定所有請求均可訪問
     * 2. 如果開啟跨域請求，則設定跨域請求的設定
     *
     * @param registry CorsRegistry
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        String[] allowedOriginPatterns;
        String[] allowOrigins;
        String[] allowedMethods;
        String[] allowedHeaders;
        boolean allowCredentials;

        if (!securityProperties.getCors().isCors()) {
            allowedOriginPatterns = new String[]{"*"};
            allowOrigins = new String[]{};
            allowedMethods = new String[]{"*"};
            allowedHeaders = new String[]{"*"};
            allowCredentials = false;
        } else {
            allowedOriginPatterns = securityProperties.getCors().getAllowedOriginPatterns().toArray(new String[0]);
            allowOrigins = securityProperties.getCors().getAllowedOrigins().toArray(new String[0]);
            allowedMethods = securityProperties.getCors().getAllowedMethods().toArray(new String[0]);
            allowedHeaders = securityProperties.getCors().getAllowedHeaders().toArray(new String[0]);
            allowCredentials = securityProperties.getCors().isAllowCredentials();
        }

        registry
                .addMapping("/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedOrigins(allowOrigins)
                .allowedMethods(allowedMethods)
                .allowedHeaders(allowedHeaders)
                .allowCredentials(allowCredentials);
    }
}
