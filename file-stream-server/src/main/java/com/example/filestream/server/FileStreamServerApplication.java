package com.example.filestream.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FileStreamServerApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(FileStreamServerApplication.class);
    
    public static void main(String[] args) {
        // 서버 시작 시 메모리 사용량 출력
        printMemoryUsage("Server startup");
        
        SpringApplication.run(FileStreamServerApplication.class, args);
        
        // 서버 시작 완료 후 메모리 사용량 출력
        printMemoryUsage("Server ready");
    }
    
    private static void printMemoryUsage(String phase) {
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
}
