package xyz.dowob.audiototext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 程式的啟動類，用於啟動 Spring Boot 應用
 * 啟動後會自動掃描並加載所有的組件
 * 開啟定時任務
 *
 * @author yuan
 * @program AudioToText
 * @ClassName AudioToTextApplication
 * @description
 * @create 2024-12-18 13:38
 * @Version 1.0
 **/


@EnableScheduling
@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
public class AudioToTextApplication {
    public static void main(String[] args) {
        SpringApplication.run(AudioToTextApplication.class, args);
    }
}
