package com.example.filestream.multipart.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 파일 업로드 성공 응답 정보
 */
public class FileUploadResponse {
    
    private String message;
    private List<FileInfo> files;
    private FileUploadMetadata metadata;
    private long totalSize;
    private int fileCount;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;
    
    // 기본 생성자
    public FileUploadResponse() {
        this.uploadedAt = LocalDateTime.now();
    }
    
    // 생성자
    public FileUploadResponse(String message, List<FileInfo> files, FileUploadMetadata metadata) {
        this();
        this.message = message;
        this.files = files;
        this.metadata = metadata;
        this.fileCount = files != null ? files.size() : 0;
        this.totalSize = files != null ? files.stream().mapToLong(FileInfo::getSize).sum() : 0;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<FileInfo> getFiles() {
        return files;
    }
    
    public void setFiles(List<FileInfo> files) {
        this.files = files;
        this.fileCount = files != null ? files.size() : 0;
        this.totalSize = files != null ? files.stream().mapToLong(FileInfo::getSize).sum() : 0;
    }
    
    public FileUploadMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(FileUploadMetadata metadata) {
        this.metadata = metadata;
    }
    
    public long getTotalSize() {
        return totalSize;
    }
    
    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }
    
    public int getFileCount() {
        return fileCount;
    }
    
    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    /**
     * 개별 파일 정보
     */
    public static class FileInfo {
        private String originalFilename;
        private String savedFilename;
        private String contentType;
        private long size;
        private String path;
        
        // 기본 생성자
        public FileInfo() {}
        
        // 생성자
        public FileInfo(String originalFilename, String savedFilename, String contentType, long size, String path) {
            this.originalFilename = originalFilename;
            this.savedFilename = savedFilename;
            this.contentType = contentType;
            this.size = size;
            this.path = path;
        }
        
        // Getters and Setters
        public String getOriginalFilename() {
            return originalFilename;
        }
        
        public void setOriginalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
        }
        
        public String getSavedFilename() {
            return savedFilename;
        }
        
        public void setSavedFilename(String savedFilename) {
            this.savedFilename = savedFilename;
        }
        
        public String getContentType() {
            return contentType;
        }
        
        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
        
        public long getSize() {
            return size;
        }
        
        public void setSize(long size) {
            this.size = size;
        }
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
    }
}
