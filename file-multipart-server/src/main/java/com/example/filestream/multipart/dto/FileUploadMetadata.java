package com.example.filestream.multipart.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 파일 업로드 요청에 포함되는 메타데이터 정보
 */
public class FileUploadMetadata {
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    private String uploadedBy;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private List<String> tags;
    
    private Map<String, Object> customFields;
    
    private Boolean compressed;
    
    private String encoding;
    
    // 기본 생성자
    public FileUploadMetadata() {
        this.timestamp = LocalDateTime.now();
        this.compressed = false;
        this.encoding = "UTF-8";
    }
    
    // Getters and Setters
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getUploadedBy() {
        return uploadedBy;
    }
    
    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getCustomFields() {
        return customFields;
    }
    
    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
    
    public Boolean getCompressed() {
        return compressed;
    }
    
    public void setCompressed(Boolean compressed) {
        this.compressed = compressed;
    }
    
    public String getEncoding() {
        return encoding;
    }
    
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    @Override
    public String toString() {
        return "FileUploadMetadata{" +
                "description='" + description + '\'' +
                ", category='" + category + '\'' +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", timestamp=" + timestamp +
                ", tags=" + tags +
                ", customFields=" + customFields +
                ", compressed=" + compressed +
                ", encoding='" + encoding + '\'' +
                '}';
    }
}
