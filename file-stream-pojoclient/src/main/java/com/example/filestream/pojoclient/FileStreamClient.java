package com.example.filestream.pojoclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class FileStreamClient {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Properties config = new Properties();
    
    // Default values
    private static int chunkSize = 8192; // 8KB
    private static long maxFileSize = 1073741824L; // 1GB
    private static int connectionTimeout = 30000; // 30 seconds
    private static int readTimeout = 60000; // 60 seconds
    
    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        try (InputStream input = FileStreamClient.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                config.load(input);
                
                // Load configuration values
                chunkSize = Integer.parseInt(config.getProperty("file.stream.chunk.size", "8192"));
                maxFileSize = Long.parseLong(config.getProperty("file.stream.max.size", "1073741824"));
                connectionTimeout = Integer.parseInt(config.getProperty("file.stream.connection.timeout", "30000"));
                readTimeout = Integer.parseInt(config.getProperty("file.stream.read.timeout", "60000"));
                
                System.out.println("Configuration loaded:");
                System.out.println("- Chunk size: " + chunkSize + " bytes");
                System.out.println("- Max file size: " + (maxFileSize / 1024 / 1024) + " MB");
                System.out.println("- Connection timeout: " + connectionTimeout + " ms");
                System.out.println("- Read timeout: " + readTimeout + " ms");
            } else {
                System.out.println("Using default configuration values");
            }
        } catch (Exception e) {
            System.err.println("Failed to load configuration, using defaults: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String filePath = null;
        String targetUrl = null;

        // 커맨드 라인 인자 파싱
        for (String arg : args) {
            if (arg.startsWith("--file.path=")) {
                filePath = arg.substring("--file.path=".length());
                // 따옴표 제거
                if (filePath.startsWith("\"") && filePath.endsWith("\"")) {
                    filePath = filePath.substring(1, filePath.length() - 1);
                }
            } else if (arg.startsWith("--target.url=")) {
                targetUrl = arg.substring("--target.url=".length());
                // 따옴표 제거
                if (targetUrl.startsWith("\"") && targetUrl.endsWith("\"")) {
                    targetUrl = targetUrl.substring(1, targetUrl.length() - 1);
                }
            }
        }

        if (filePath == null || targetUrl == null) {
            System.err.println("Usage: java -jar file-stream-pojoclient-1.0.0.jar --file.path=\"<file_path>\" --target.url=\"<target_url>\"");
            System.err.println("Example: java -jar file-stream-pojoclient-1.0.0.jar --file.path=\"C:/data/backup.zip\" --target.url=\"http://localhost:8080/api/v1/files/upload\"");
            System.exit(1);
        }

        try {
            uploadFile(filePath, targetUrl);
        } catch (Exception e) {
            System.err.println("Error uploading file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void uploadFile(String filePath, String targetUrl) throws Exception {
        // 초기 메모리 사용량 출력
        printMemoryUsage("Client startup");
        
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Path is not a regular file: " + filePath);
        }

        String filename = path.getFileName().toString();
        long fileSize = Files.size(path);
        
        // 파일 크기 제한 검사
        if (fileSize > maxFileSize) {
            throw new IllegalArgumentException(
                String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)", 
                fileSize, maxFileSize));
        }
        
        System.out.println("Uploading file: " + filename + " (" + fileSize + " bytes)");
        System.out.println("Using chunk size: " + chunkSize + " bytes");
        printMemoryUsage("Before upload");

        // URL 인코딩된 파일명으로 최종 URL 생성
        String encodedFilename = URLEncoder.encode(filename, "UTF-8");
        String finalUrl = targetUrl + "?filename=" + encodedFilename;
        
        System.out.println("Sending request to: " + finalUrl);

        // URL 연결 설정
        URL url = new URL(finalUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // HTTP 메서드 및 헤더 설정
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Content-Length", String.valueOf(fileSize));
            connection.setDoOutput(true);
            connection.setDoInput(true);
            
            // 타임아웃 설정
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);
            
            // 청킹 전송 활성화 (메모리 효율성을 위해)
            connection.setChunkedStreamingMode(chunkSize);
            
            printMemoryUsage("During upload");
            
            // 파일 업로드 (스트리밍 방식)
            try (InputStream fileInputStream = Files.newInputStream(path);
                 OutputStream outputStream = connection.getOutputStream()) {
                
                streamFileWithProgress(fileInputStream, outputStream, fileSize);
            }
            
            printMemoryUsage("After upload");
            
            // 응답 처리
            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection);
            
            if (statusCode == 200) {
                System.out.println("File '" + filename + "' uploaded successfully. Server response: " + statusCode + " OK");
                
                // JSON 응답 파싱하여 추가 정보 출력
                try {
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    if (jsonResponse.has("message")) {
                        System.out.println("Server message: " + jsonResponse.get("message").asText());
                    }
                    if (jsonResponse.has("size")) {
                        System.out.println("Uploaded size: " + jsonResponse.get("size").asLong() + " bytes");
                    }
                } catch (Exception e) {
                    System.out.println("Response body: " + responseBody);
                }
                
                printMemoryUsage("Upload completed");
            } else {
                System.err.println("Error uploading file. Server response: " + statusCode + " " + connection.getResponseMessage());
                System.err.println("Response body: " + responseBody);
                throw new RuntimeException("Server returned HTTP " + statusCode);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    private static void streamFileWithProgress(InputStream inputStream, OutputStream outputStream, long fileSize) throws IOException {
        byte[] buffer = new byte[chunkSize];
        int bytesRead;
        long totalBytesRead = 0;
        long lastProgressUpdate = 0;
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
            
            // 진행률 출력 (10% 단위)
            long currentProgress = (totalBytesRead * 100) / fileSize;
            if (currentProgress >= lastProgressUpdate + 10) {
                System.out.printf("Upload progress: %d%% (%d/%d bytes)%n", 
                    currentProgress, totalBytesRead, fileSize);
                lastProgressUpdate = currentProgress;
                
                // 메모리 사용량 체크 (진행률 출력 시점에만)
                if (currentProgress % 20 == 0) {
                    printMemoryUsage("Progress " + currentProgress + "%");
                }
            }
        }
        
        outputStream.flush();
        System.out.println("Upload progress: 100% (completed)");
    }
    
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
    
    private static void printMemoryUsage(String phase) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        System.out.printf("[%s] Memory Usage - Used: %.2f MB / Max: %.2f MB%n", 
            phase, 
            usedMemory / 1024.0 / 1024.0, 
            maxMemory / 1024.0 / 1024.0);
            
        // 20MB 이상 사용 시 경고
        if (usedMemory > 20 * 1024 * 1024) {
            System.err.printf("⚠️  WARNING: Memory usage exceeds 20MB limit! Used: %.2f MB%n", 
                usedMemory / 1024.0 / 1024.0);
        }
    }
}
