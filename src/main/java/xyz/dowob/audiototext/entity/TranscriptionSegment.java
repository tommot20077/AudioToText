package xyz.dowob.audiototext.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 轉譯片段，包含文字、開始時間、結束時間
 * 用於返回音訊轉文字的結果
 *
 * @author yuan
 * @program AudioToText
 * @ClassName TranscriptionSegment
 * @description
 * @create 2024-12-12 19:53
 * @Version 1.0
 **/
@Data
public class TranscriptionSegment {
    /**
     * 轉譯的文字
     */
    private String text;

    /**
     * 開始時間
     */
    @JsonProperty("start_time")
    private Double startTime;

    /**
     * 結束時間
     */
    @JsonProperty("end_time")
    private Double endTime;
}
