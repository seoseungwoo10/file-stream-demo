package com.example.filestream.server.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;

@SpringBootTest
@AutoConfigureMockMvc
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("파일 업로드 성공 테스트")
    void uploadFile_success() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "test-upload.txt", MediaType.TEXT_PLAIN_VALUE, "hello world".getBytes(StandardCharsets.UTF_8));
        String content = "test file content";
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/files/upload")
                        .file(file1)
                        .param("filename", "test-upload.txt"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.fileCount").value(1));
    }

    @Test
    @DisplayName("파일명 없이 업로드 시 에러 반환 테스트")
    void uploadFile_noFilename() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "test-upload.txt", MediaType.TEXT_PLAIN_VALUE, "hello world".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/files/upload")
                        .file(file1))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}

