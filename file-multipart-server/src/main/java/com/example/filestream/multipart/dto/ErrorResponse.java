package com.example.filestream.multipart.dto;

/**
 * 에러 응답 정보
 */
public class ErrorResponse {
    
    private String error;
    private String message;
    private int status;
    private String timestamp;
    
    // 기본 생성자
    public ErrorResponse() {
        this.timestamp = java.time.LocalDateTime.now().toString();
    }
    
    // 생성자
    public ErrorResponse(String error, String message, int status) {
        this();
        this.error = error;
        this.message = message;
        this.status = status;
    }
    
    // Getters and Setters
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
