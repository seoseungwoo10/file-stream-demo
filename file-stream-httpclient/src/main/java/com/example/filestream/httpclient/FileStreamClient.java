package com.example.filestream.httpclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URLEncoder;
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
            System.err.println("Usage: java -jar file-stream-client-1.0.0.jar --file.path=\"<file_path>\" --target.url=\"<target_url>\"");
            System.err.println("Example: java -jar file-stream-client-1.0.0.jar --file.path=\"C:/data/backup.zip\" --target.url=\"http://localhost:8080/api/v1/files/upload\"");
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

        // HTTP 클라이언트 설정 (타임아웃 적용)
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(connectionTimeout)
            .setSocketTimeout(readTimeout)
            .build();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPost httpPost = new HttpPost(finalUrl);
            httpPost.setHeader("Content-Type", "application/octet-stream");
            httpPost.setHeader("Content-Length", String.valueOf(fileSize));
            httpPost.setConfig(requestConfig);

            // 청크 기반 스트리밍 엔티티 생성
            ChunkedStreamingEntity entity = new ChunkedStreamingEntity(path, fileSize, chunkSize);
            httpPost.setEntity(entity);

            System.out.println("Sending request to: " + finalUrl);
            printMemoryUsage("During upload");

            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            
            printMemoryUsage("After upload");
            
            HttpEntity responseEntity = response.getEntity();
            String responseBody = responseEntity != null ? EntityUtils.toString(responseEntity) : "";

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
                System.err.println("Error uploading file. Server response: " + statusCode + " " + response.getStatusLine().getReasonPhrase());
                System.err.println("Response body: " + responseBody);
                throw new RuntimeException("Server returned HTTP " + statusCode);
            }
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
    
    // 청크 기반 스트리밍 엔티티 클래스
    private static class ChunkedStreamingEntity extends AbstractHttpEntity {
        private final Path filePath;
        private final long fileSize;
        private final int chunkSize;
        
        public ChunkedStreamingEntity(Path filePath, long fileSize, int chunkSize) {
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.chunkSize = chunkSize;
            setContentType("application/octet-stream");
        }
        
        @Override
        public boolean isRepeatable() {
            return true;
        }
        
        @Override
        public long getContentLength() {
            return fileSize;
        }
        
        @Override
        public InputStream getContent() throws IOException {
            return Files.newInputStream(filePath);
        }
        
        @Override
        public void writeTo(OutputStream outStream) throws IOException {
            try (InputStream inStream = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[chunkSize];
                int bytesRead;
                long totalBytesRead = 0;
                long lastProgressUpdate = 0;
                
                while ((bytesRead = inStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    // 진행률 출력 (10% 단위)
                    long currentProgress = (totalBytesRead * 100) / fileSize;
                    if (currentProgress >= lastProgressUpdate + 10) {
                        System.out.printf("Upload progress: %d%% (%d/%d bytes)%n", 
                            currentProgress, totalBytesRead, fileSize);
                        lastProgressUpdate = currentProgress;
                        
                        // 메모리 사용량 체크 (진행률 출력 시점에만)
                        if (currentProgress % 20 == 0) {
                            FileStreamClient.printMemoryUsage("Progress " + currentProgress + "%");
                        }
                    }
                }
                
                outStream.flush();
                System.out.println("Upload progress: 100% (completed)");
            }
        }
        
        @Override
        public boolean isStreaming() {
            return true;
        }
    }
}
