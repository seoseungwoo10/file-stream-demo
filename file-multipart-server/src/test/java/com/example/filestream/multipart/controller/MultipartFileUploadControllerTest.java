package com.example.filestream.multipart.controller;

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

@SpringBootTest
@AutoConfigureMockMvc
class MultipartFileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("멀티파트 파일 업로드 성공 테스트")
    void uploadFiles_success() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "test1.txt", MediaType.TEXT_PLAIN_VALUE, "hello world".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "test2.txt", MediaType.TEXT_PLAIN_VALUE, "spring boot".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/multipart/upload")
                        .file(file1)
                        .file(file2))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.fileCount").value(2));
    }

    @Test
    @DisplayName("파일 없이 업로드 시 에러 반환 테스트")
    void uploadFiles_noFile() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/multipart/upload"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}

