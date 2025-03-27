package xyz.dowob.audiototext.component.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.dowob.audiototext.config.SecurityProperties;

import java.io.IOException;
import java.util.Optional;

/**
 * 此類用於設定跨域請求的過濾器
 * 實現 Filter 用於設定跨域請求，根據配置文件中的配置進行設定 {@link SecurityProperties}
 *
 * @author yuan
 * @program AudioToText
 * @ClassName CorsFilter
 * @create 2025/3/27
 * @Version 1.0
 **/
public class CorsFilter implements Filter {

    /**
     * OPTIONS 請求
     */
    private static final String OPTIONS = "OPTIONS";
    /**
     * 安全設定
     */
    private final SecurityProperties securityProperties;
    /**
     * 允許跨域請求的來源
     */
    private final String allowedOriginPatterns;

    /**
     * 允許跨域請求的方法
     */
    private final String allowedMethods;

    /**
     * 允許跨域請求的標頭
     */
    private final String allowedHeaders;

    /**
     * 是否允許跨域請求攜帶認證信息
     */
    private final String allowCredentials;

    /**
     * 用於構造 CorsFilter 對象
     * 會將配置文件中的跨域請求配置進行設定
     *
     * @param securityProperties 安全設定
     */
    public CorsFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;

        this.allowedOriginPatterns = Optional
                .ofNullable(securityProperties.getCors().getAllowedOriginPatterns())
                .map(list -> list.stream().reduce((a, b) -> a + "," + b).orElse("*"))
                .orElse("*");

        this.allowedMethods = Optional
                .ofNullable(securityProperties.getCors().getAllowedMethods())
                .map(list -> list.stream().reduce((a, b) -> a + "," + b).orElse("*"))
                .orElse("*");
        this.allowedHeaders = Optional
                .ofNullable(securityProperties.getCors().getAllowedHeaders())
                .map(list -> list.stream().reduce((a, b) -> a + "," + b).orElse("*"))
                .orElse("*");
        this.allowCredentials = String.valueOf(securityProperties.getCors().isAllowCredentials());
    }

    /**
     * 設定過濾器的過濾方法
     * 若配置文件中的跨域請求配置為關閉，則直接放行
     * 否則進行跨域請求的設定，並放行
     * 若請求為 OPTIONS 請求，則直接返回
     *
     * @param servletRequest  請求
     * @param servletResponse 響應
     * @param filterChain     過濾器鏈
     *
     * @throws IOException      IO異常
     * @throws ServletException Servlet異常
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse res = (HttpServletResponse) servletResponse;
        if (!securityProperties.getCors().isCors()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        res.addHeader("Access-Control-Allow-Origin", allowedOriginPatterns);
        res.addHeader("Access-Control-Allow-Methods", allowedMethods);
        res.addHeader("Access-Control-Allow-Headers", allowedHeaders);
        res.addHeader("Access-Control-Allow-Credentials", String.valueOf(allowCredentials));

        if (OPTIONS.equals(((HttpServletRequest) servletRequest).getMethod())) {
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
