package xyz.dowob.audiototext.entity;

import lombok.Data;

/**
 * @author yuan
 * @program AudioToText
 * @ClassName TranscriptionSegment
 * @description
 * @create 2024-12-12 19:53
 * @Version 1.0
 **/
@Data
public class TranscriptionSegment {
    private String text;
    private Double startTime;
    private Double endTime;
}
