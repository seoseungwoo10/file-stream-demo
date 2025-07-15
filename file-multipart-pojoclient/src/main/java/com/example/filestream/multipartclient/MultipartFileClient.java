package com.example.filestream.multipartclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MultipartFileClient {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Properties config = new Properties();
    
    // Default values
    private static int connectionTimeout = 30000; // 30 seconds
    private static int readTimeout = 60000; // 60 seconds
    private static long maxFileSize = 104857600L; // 100MB
    private static long maxRequestSize = 524288000L; // 500MB
    private static int bufferSize = 8192; // 8KB
    
    // Multipart boundary
    private static final String BOUNDARY = "----WebKitFormBoundary" + System.currentTimeMillis();
    private static final String LINE_FEED = "\r\n";
    
    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        try (InputStream input = MultipartFileClient.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                config.load(input);
                
                // Load configuration values
                connectionTimeout = Integer.parseInt(config.getProperty("file.multipart.connection.timeout", "30000"));
                readTimeout = Integer.parseInt(config.getProperty("file.multipart.read.timeout", "60000"));
                maxFileSize = Long.parseLong(config.getProperty("file.multipart.max.file.size", "104857600"));
                maxRequestSize = Long.parseLong(config.getProperty("file.multipart.max.request.size", "524288000"));
                bufferSize = Integer.parseInt(config.getProperty("file.multipart.buffer.size", "8192"));
                
                System.out.println("Configuration loaded:");
                System.out.println("- Connection timeout: " + connectionTimeout + " ms");
                System.out.println("- Read timeout: " + readTimeout + " ms");
                System.out.println("- Max file size: " + (maxFileSize / 1024 / 1024) + " MB");
                System.out.println("- Max request size: " + (maxRequestSize / 1024 / 1024) + " MB");
                System.out.println("- Buffer size: " + bufferSize + " bytes");
            } else {
                System.out.println("Using default configuration values");
            }
        } catch (Exception e) {
            System.err.println("Failed to load configuration, using defaults: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        List<String> filePaths = new ArrayList<>();
        String targetUrl = null;
        String metadataJson = null;
        String metadataFilePath = null;

        // 커맨드 라인 인자 파싱
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--files=")) {
                String filesArg = arg.substring("--files=".length());
                // 따옴표 제거
                if (filesArg.startsWith("\"") && filesArg.endsWith("\"")) {
                    filesArg = filesArg.substring(1, filesArg.length() - 1);
                }
                // 쉼표로 분리된 파일 경로들
                String[] paths = filesArg.split(",");
                for (String path : paths) {
                    filePaths.add(path.trim());
                }
            } else if (arg.startsWith("--target.url=")) {
                targetUrl = arg.substring("--target.url=".length());
                // 따옴표 제거
                if (targetUrl.startsWith("\"") && targetUrl.endsWith("\"")) {
                    targetUrl = targetUrl.substring(1, targetUrl.length() - 1);
                }
            } else if (arg.startsWith("--metadata=")) {
                metadataJson = arg.substring("--metadata=".length());
                // 따옴표 제거
                if (metadataJson.startsWith("\"") && metadataJson.endsWith("\"")) {
                    metadataJson = metadataJson.substring(1, metadataJson.length() - 1);
                }
            } else if (arg.startsWith("--metadata-file=")) {
                metadataFilePath = arg.substring("--metadata-file=".length());
                // 따옴표 제거
                if (metadataFilePath.startsWith("\"") && metadataFilePath.endsWith("\"")) {
                    metadataFilePath = metadataFilePath.substring(1, metadataFilePath.length() - 1);
                }
            }
        }

        if (filePaths.isEmpty() || targetUrl == null) {
            System.err.println("Usage: java -jar file-multipart-pojoclient-1.0.0.jar --files=\"<file_path1>,<file_path2>\" --target.url=\"<target_url>\" [--metadata=\"<json_metadata>\"] [--metadata-file=\"<json_file_path>\"]");
            System.err.println("Examples:");
            System.err.println("  Single file: --files=\"C:/data/file1.txt\" --target.url=\"http://localhost:8081/api/v1/multipart/upload\"");
            System.err.println("  Multiple files: --files=\"file1.txt,file2.txt\" --target.url=\"http://localhost:8081/api/v1/multipart/upload\"");
            System.err.println("  With metadata: --metadata='{\"description\":\"Test upload\",\"category\":\"test\",\"uploadedBy\":\"user\"}'");
            System.err.println("  With metadata file: --metadata-file=\"metadata.json\"");
            System.exit(1);
        }

        // 메타데이터 처리 (파일에서 읽기 우선)
        if (metadataFilePath != null) {
            try {
                metadataJson = Files.readString(Paths.get(metadataFilePath));
                System.out.println("Metadata loaded from file: " + metadataFilePath);
            } catch (Exception e) {
                System.err.println("Failed to read metadata file: " + e.getMessage());
                System.exit(1);
            }
        }

        // 기본 메타데이터 생성 (제공되지 않은 경우)
        if (metadataJson == null) {
            metadataJson = createDefaultMetadata(filePaths);
        }

        try {
            uploadFiles(filePaths, targetUrl, metadataJson);
        } catch (Exception e) {
            System.err.println("Error uploading files: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void uploadFiles(List<String> filePaths, String targetUrl, String metadataJson) throws Exception {
        // 초기 메모리 사용량 출력
        printMemoryUsage("Client startup");
        
        // 파일들 검증
        List<Path> validPaths = new ArrayList<>();
        long totalSize = 0;
        
        for (String filePath : filePaths) {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                throw new FileNotFoundException("File not found: " + filePath);
            }

            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException("Path is not a regular file: " + filePath);
            }

            long fileSize = Files.size(path);
            if (fileSize > maxFileSize) {
                throw new IllegalArgumentException(
                    String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes): %s", 
                    fileSize, maxFileSize, filePath));
            }
            
            validPaths.add(path);
            totalSize += fileSize;
        }
        
        if (totalSize > maxRequestSize) {
            throw new IllegalArgumentException(
                String.format("Total request size (%d bytes) exceeds maximum allowed size (%d bytes)", 
                totalSize, maxRequestSize));
        }
        
        System.out.println("Uploading " + validPaths.size() + " files:");
        for (Path path : validPaths) {
            System.out.println("  - " + path.getFileName() + " (" + Files.size(path) + " bytes)");
        }
        System.out.println("Total size: " + totalSize + " bytes");
        System.out.println("Metadata: " + metadataJson);
        
        printMemoryUsage("Before upload");

        // URL 연결 설정
        URL url = new URL(targetUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // HTTP 메서드 및 헤더 설정
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            
            // 타임아웃 설정
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);
            
            // 청킹 전송 활성화 (메모리 효율성을 위해)
            connection.setChunkedStreamingMode(bufferSize);
            
            System.out.println("Sending multipart request to: " + targetUrl);
            printMemoryUsage("During upload");
            
            // Multipart 데이터 전송
            try (OutputStream outputStream = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {
                
                // 메타데이터 필드 추가
                addFormField(writer, "metadata", metadataJson);
                
                // 파일들 추가
                for (int i = 0; i < validPaths.size(); i++) {
                    Path filePath = validPaths.get(i);
                    addFilePart(writer, outputStream, "files", filePath, i + 1, validPaths.size());
                }
                
                // 마지막 boundary
                writer.append("--" + BOUNDARY + "--").append(LINE_FEED);
                writer.flush();
            }
            
            printMemoryUsage("After upload");
            
            // 응답 처리
            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection);
            
            if (statusCode == 200) {
                System.out.println("Files uploaded successfully. Server response: " + statusCode + " OK");
                
                // JSON 응답 파싱하여 추가 정보 출력
                try {
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    if (jsonResponse.has("message")) {
                        System.out.println("Server message: " + jsonResponse.get("message").asText());
                    }
                    if (jsonResponse.has("fileCount")) {
                        System.out.println("Uploaded files: " + jsonResponse.get("fileCount").asInt());
                    }
                    if (jsonResponse.has("totalSize")) {
                        System.out.println("Total uploaded size: " + jsonResponse.get("totalSize").asLong() + " bytes");
                    }
                    if (jsonResponse.has("files")) {
                        System.out.println("Saved files:");
                        JsonNode files = jsonResponse.get("files");
                        for (JsonNode file : files) {
                            System.out.println("  - " + file.get("originalFilename").asText() + 
                                             " -> " + file.get("savedFilename").asText());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Response body: " + responseBody);
                }
                
                printMemoryUsage("Upload completed");
            } else {
                System.err.println("Error uploading files. Server response: " + statusCode + " " + connection.getResponseMessage());
                System.err.println("Response body: " + responseBody);
                throw new RuntimeException("Server returned HTTP " + statusCode);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Multipart form field 추가
     */
    private static void addFormField(PrintWriter writer, String name, String value) {
        writer.append("--" + BOUNDARY).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
        writer.flush();
    }
    
    /**
     * Multipart file part 추가
     */
    private static void addFilePart(PrintWriter writer, OutputStream outputStream, 
                                    String fieldName, Path filePath, int currentFile, int totalFiles) throws IOException {
        String fileName = filePath.getFileName().toString();
        
        writer.append("--" + BOUNDARY).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"").append(LINE_FEED);
        writer.append("Content-Type: " + getContentType(fileName)).append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();
        
        // 파일 내용 스트리밍
        long fileSize = Files.size(filePath);
        long bytesRead = 0;
        long lastProgressUpdate = 0;
        
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[bufferSize];
            int bytesReadThisTime;
            while ((bytesReadThisTime = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesReadThisTime);
                bytesRead += bytesReadThisTime;
                
                // 진행률 출력 (10% 단위)
                long currentProgress = (bytesRead * 100) / fileSize;
                if (currentProgress >= lastProgressUpdate + 10) {
                    System.out.printf("File %d/%d (%s): %d%% (%d/%d bytes)%n", 
                        currentFile, totalFiles, fileName, currentProgress, bytesRead, fileSize);
                    lastProgressUpdate = currentProgress;
                }
            }
        }
        
        outputStream.flush();
        writer.append(LINE_FEED);
        writer.flush();
        
        System.out.printf("File %d/%d (%s): 100%% completed%n", currentFile, totalFiles, fileName);
    }
    
    /**
     * 파일의 Content-Type 추정
     */
    private static String getContentType(String fileName) {
        try {
            String contentType = URLConnection.guessContentTypeFromName(fileName);
            return contentType != null ? contentType : "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
    
    /**
     * 응답 본문 읽기
     */
    private static String readResponseBody(HttpURLConnection connection) throws IOException {
        InputStream responseStream;
        
        // 에러 응답인 경우 에러 스트림 사용
        if (connection.getResponseCode() >= 400) {
            responseStream = connection.getErrorStream();
        } else {
            responseStream = connection.getInputStream();
        }
        
        if (responseStream == null) {
            return "";
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString().trim();
        }
    }
    
    /**
     * 기본 메타데이터 생성
     */
    private static String createDefaultMetadata(List<String> filePaths) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("description", "Multipart upload from POJO client: " + filePaths.size() + " files");
        metadata.put("category", "multipart-upload");
        metadata.put("uploadedBy", "multipart-pojoclient");
        metadata.put("tags", Arrays.asList("pojo", "multipart", "java-net"));
        
        Map<String, Object> customFields = new HashMap<>();
        customFields.put("client", "file-multipart-pojoclient");
        customFields.put("fileCount", filePaths.size());
        metadata.put("customFields", customFields);
        
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            return "{\"description\":\"Default metadata\",\"category\":\"general\",\"uploadedBy\":\"pojoclient\"}";
        }
    }
    
    /**
     * 메모리 사용량 출력
     */
    private static void printMemoryUsage(String phase) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        

        System.out.printf("[{}] Memory Usage - Used: {} MB / Max: {} MB", 
            phase, 
            String.format("%.2f", usedMemory / 1024.0 / 1024.0), 
            String.format("%.2f", maxMemory / 1024.0 / 1024.0));
            
        // 20MB 이상 사용 시 경고
        if (usedMemory > 20 * 1024 * 1024) {
            System.err.printf("⚠️ WARNING: Memory usage exceeds 20MB limit! Used: {} MB", 
                String.format("%.2f", usedMemory / 1024.0 / 1024.0));
        }
    }
}
