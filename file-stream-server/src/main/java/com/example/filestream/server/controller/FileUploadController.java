package com.example.filestream.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    @Value("${file.upload.directory:./uploads}")
    private String uploadDirectory;
    
    @Value("${file.stream.buffer.size:8192}")
    private int bufferSize;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam(name = "filename") String filename,
            HttpServletRequest request) {
        
        // 업로드 시작 시 메모리 사용량 출력
        printMemoryUsage("Upload start for file: " + filename);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(uploadDirectory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            }
            
            // 파일 경로 설정
            Path filePath = uploadPath.resolve(filename);
            logger.info("Saving file to: {}", filePath.toAbsolutePath());
            logger.info("Using buffer size: {} bytes", bufferSize);
            
            printMemoryUsage("Before file processing");
            
            // 스트림으로 파일 저장
            long bytesWritten = 0;
            try (InputStream inputStream = request.getInputStream();
                 OutputStream outputStream = Files.newOutputStream(filePath)) {
                
                byte[] buffer = new byte[bufferSize]; // 설정 가능한 버퍼 크기
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    bytesWritten += bytesRead;
                }
                
                outputStream.flush();
            }
            
            logger.info("File uploaded successfully: {} ({} MB)", filename, bytesWritten / 1024 / 1024);
            printMemoryUsage("After file processing");
            
            response.put("message", "File uploaded successfully: " + filename);
            response.put("size", bytesWritten);
            
            printMemoryUsage("Upload completed");
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            logger.error("Failed to save file: {}", filename, e);
            
            response.put("error", "Failed to save file on server.");
            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            logger.error("Unexpected error during file upload: {}", filename, e);
            
            response.put("error", "Unexpected error occurred.");
            return ResponseEntity.status(500).body(response);
        }
    }
    
    private void printMemoryUsage(String phase) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        logger.info("[{}] Memory Usage - Used: {} MB / Max: {} MB", 
            phase, 
            String.format("%.2f", usedMemory / 1024.0 / 1024.0), 
            String.format("%.2f", maxMemory / 1024.0 / 1024.0));
            
        // 20MB 이상 사용 시 경고
        if (usedMemory > 20 * 1024 * 1024) {
            logger.warn("⚠️  WARNING: Memory usage exceeds 20MB limit! Used: {} MB", 
                String.format("%.2f", usedMemory / 1024.0 / 1024.0));
        }
    }
    
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(
            org.springframework.web.bind.MissingServletRequestParameterException ex) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Filename parameter is missing.");
        
        logger.warn("Missing filename parameter in upload request");
        
        return ResponseEntity.status(400).body(response);
    }
}
