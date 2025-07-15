package com.example.filestream.multipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class FileMultipartServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(FileMultipartServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(FileMultipartServerApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        printMemoryUsage("Server startup");
        
        logger.info("=========================================");
        logger.info("File Multipart Server started successfully!");
        logger.info("=========================================");
        logger.info("Server URL: http://localhost:8081");
        logger.info("API Base: http://localhost:8081/api/v1/multipart");
        logger.info("Health Check: http://localhost:8081/actuator/health");
        logger.info("=========================================");
        logger.info("Endpoints:");
        logger.info("  POST /api/v1/multipart/upload - Multi-file upload with JSON metadata");
        logger.info("  POST /api/v1/multipart/upload/single - Single file upload");
        logger.info("  GET  /api/v1/multipart/files - List uploaded files");
        logger.info("=========================================");
    }

    private void printMemoryUsage(String phase) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        logger.info("[{}] Memory Usage - Used: {:.2f} MB / Max: {:.2f} MB", 
            phase, 
            usedMemory / 1024.0 / 1024.0, 
            maxMemory / 1024.0 / 1024.0);
    }
}
