package xyz.dowob.audiototext.dto;

/**
 * 模型信息 DTO，用於返回模型的信息
 * 轉換為紀錄類型，包含模型的代碼、描述、語言
 *
 * @author yuan
 * @program AudioToText
 * @ClassName ModelInfoDTO
 * @description
 * @create 2024-12-17 17:16
 * @Version 1.0
 **/

public record ModelInfoDTO(String code, String description, String language) {}
