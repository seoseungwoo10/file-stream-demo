package com.example.filestream.multipart.controller;

import com.example.filestream.multipart.dto.ErrorResponse;
import com.example.filestream.multipart.dto.FileUploadMetadata;
import com.example.filestream.multipart.dto.FileUploadResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/multipart")
public class MultipartFileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(MultipartFileUploadController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${file.upload.directory:./uploads}")
    private String uploadDirectory;

    @Value("${file.stream.buffer.size:8192}")
    private int bufferSize;

    /**
     * 다중 파일 업로드 (JSON 메타데이터 포함)
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("metadata") String metadataJson) {

        printMemoryUsage("Upload start");

        try {
            // JSON 메타데이터 파싱
            FileUploadMetadata metadata;
            try {
                metadata = objectMapper.readValue(metadataJson, FileUploadMetadata.class);
            } catch (JsonProcessingException e) {
                logger.error("Invalid JSON metadata: {}", e.getMessage());
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid JSON", "Metadata JSON is malformed: " + e.getMessage(), 400));
            }

            // 메타데이터 검증
            if (!StringUtils.hasText(metadata.getDescription()) || !StringUtils.hasText(metadata.getCategory())) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Validation Error", "Description and category are required", 400));
            }

            // 파일 검증
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("No Files", "At least one file must be provided", 400));
            }

            logger.info("Uploading {} files with metadata: {}", files.length, metadata);

            // 업로드 디렉토리 생성
            Path uploadPath = createUploadDirectory();
            
            // 파일들 처리
            List<FileUploadResponse.FileInfo> fileInfos = new ArrayList<>();
            long totalSize = 0;

            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    logger.warn("Skipping empty file: {}", file.getOriginalFilename());
                    continue;
                }

                FileUploadResponse.FileInfo fileInfo = processFile(file, uploadPath, metadata);
                fileInfos.add(fileInfo);
                totalSize += fileInfo.getSize();

                printMemoryUsage("File processed: " + file.getOriginalFilename());
            }

            // 응답 생성
            FileUploadResponse response = new FileUploadResponse(
                    String.format("Successfully uploaded %d files", fileInfos.size()),
                    fileInfos,
                    metadata
            );

            printMemoryUsage("Upload completed");

            logger.info("Upload completed successfully. Files: {}, Total size: {} bytes", 
                       fileInfos.size(), totalSize);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("Error during file upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Upload Error", "Failed to save files: " + e.getMessage(), 500));
        }
    }

    /**
     * 단일 파일 업로드 (간단한 버전)
     */
    @PostMapping("/upload/single")
    public ResponseEntity<?> uploadSingleFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) String metadataJson) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Empty File", "File cannot be empty", 400));
            }

            // 기본 메타데이터 생성
            FileUploadMetadata metadata;
            if (StringUtils.hasText(metadataJson)) {
                try {
                    metadata = objectMapper.readValue(metadataJson, FileUploadMetadata.class);
                } catch (JsonProcessingException e) {
                    metadata = createDefaultMetadata(file);
                }
            } else {
                metadata = createDefaultMetadata(file);
            }

            Path uploadPath = createUploadDirectory();
            FileUploadResponse.FileInfo fileInfo = processFile(file, uploadPath, metadata);

            FileUploadResponse response = new FileUploadResponse(
                    "File uploaded successfully: " + file.getOriginalFilename(),
                    Collections.singletonList(fileInfo),
                    metadata
            );

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("Error during single file upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Upload Error", "Failed to save file: " + e.getMessage(), 500));
        }
    }

    /**
     * 업로드된 파일 목록 조회
     */
    @GetMapping("/files")
    public ResponseEntity<?> listUploadedFiles() {
        try {
            Path uploadPath = Paths.get(uploadDirectory);
            if (!Files.exists(uploadPath)) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<String> files = new ArrayList<>();
            Files.list(uploadPath)
                    .filter(Files::isRegularFile)
                    .forEach(path -> files.add(path.getFileName().toString()));

            return ResponseEntity.ok(files);

        } catch (IOException e) {
            logger.error("Error listing files: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("List Error", "Failed to list files: " + e.getMessage(), 500));
        }
    }

    /**
     * 파일 처리 (저장)
     */
    private FileUploadResponse.FileInfo processFile(MultipartFile file, Path uploadPath, FileUploadMetadata metadata) throws IOException {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        
        // 고유한 파일명 생성 (타임스탬프 + UUID)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String savedFilename = String.format("%s_%s_%s%s", 
                timestamp, uniqueId, sanitizeFilename(originalFilename), fileExtension);

        Path filePath = uploadPath.resolve(savedFilename);

        // 파일 저장 (스트리밍 방식)
        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Failed to save file {}: {}", savedFilename, e.getMessage());
            throw e;
        }

        logger.info("File saved: {} -> {} ({} bytes)", originalFilename, savedFilename, file.getSize());

        return new FileUploadResponse.FileInfo(
                originalFilename,
                savedFilename,
                file.getContentType(),
                file.getSize(),
                filePath.toString()
        );
    }

    /**
     * 업로드 디렉토리 생성
     */
    private Path createUploadDirectory() throws IOException {
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("Created upload directory: {}", uploadPath.toAbsolutePath());
        }
        return uploadPath;
    }

    /**
     * 기본 메타데이터 생성
     */
    private FileUploadMetadata createDefaultMetadata(MultipartFile file) {
        FileUploadMetadata metadata = new FileUploadMetadata();
        metadata.setDescription("Single file upload: " + file.getOriginalFilename());
        metadata.setCategory("general");
        metadata.setUploadedBy("anonymous");
        return metadata;
    }

    /**
     * 파일명 정리 (특수문자 제거)
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        
        // 확장자 제거
        int lastDotIndex = filename.lastIndexOf('.');
        String nameWithoutExtension = lastDotIndex > 0 ? filename.substring(0, lastDotIndex) : filename;
        
        // 특수문자를 언더스코어로 치환
        return nameWithoutExtension.replaceAll("[^a-zA-Z0-9가-힣._-]", "_");
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) return "";
        
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }

    /**
     * 메모리 사용량 출력
     */
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
            logger.warn("⚠️ WARNING: Memory usage exceeds 20MB limit! Used: {} MB",
                    String.format("%.2f", usedMemory / 1024.0 / 1024.0));
        }
    }
}
