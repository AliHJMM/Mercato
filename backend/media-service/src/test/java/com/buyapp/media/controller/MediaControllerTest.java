package com.buyapp.media.controller;

import com.buyapp.media.dto.MediaDto;
import com.buyapp.media.entity.Media;
import com.buyapp.media.service.MediaService;
import com.buyapp.media.service.MinioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MediaController.class)
@Import(TestSecurityConfig.class)
class MediaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private MediaService mediaService;
    @MockBean  private MinioService minioService;

    private MediaDto buildDto() {
        MediaDto dto = new MediaDto();
        dto.setId("media-1");
        dto.setOriginalFilename("test.jpg");
        dto.setMimeType("image/jpeg");
        dto.setSize(1024L);
        dto.setUrl("http://minio/bucket/object-uuid.jpg");
        dto.setUploadedBy("seller-001");
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    private Media buildMedia() {
        Media m = new Media();
        m.setId("media-1");
        m.setFilename("object-uuid.jpg");
        m.setMimeType("image/jpeg");
        m.setUploadedBy("seller-001");
        return m;
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void uploadImage_asSeller_returns201() throws Exception {
        when(mediaService.uploadImage(any(), any())).thenReturn(buildDto());

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        mockMvc.perform(multipart("/media/images")
                        .file(file)
                        .param("productId", "prod-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("media-1"))
                .andExpect(jsonPath("$.mimeType").value("image/jpeg"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void uploadImage_asClient_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/media/images").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadImage_unauthenticated_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/media/images").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getImageInfo_public_returns200() throws Exception {
        when(mediaService.getMediaById("media-1")).thenReturn(buildDto());

        mockMvc.perform(get("/media/images/media-1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("media-1"))
                .andExpect(jsonPath("$.mimeType").value("image/jpeg"));
    }

    @Test
    void getImage_public_returns200WithCacheHeader() throws Exception {
        when(mediaService.getMediaEntity("media-1")).thenReturn(buildMedia());
        when(minioService.downloadFile("object-uuid.jpg"))
                .thenReturn(new ByteArrayInputStream(new byte[]{(byte) 0xFF, (byte) 0xD8}));

        mockMvc.perform(get("/media/images/media-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("max-age")));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void getMyMedia_asSeller_returns200() throws Exception {
        when(mediaService.getMyMedia()).thenReturn(List.of(buildDto()));

        mockMvc.perform(get("/media/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("media-1"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void getMyMedia_asClient_returns403() throws Exception {
        mockMvc.perform(get("/media/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyMedia_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/media/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void deleteMedia_asSeller_returns204() throws Exception {
        mockMvc.perform(delete("/media/images/media-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void deleteMedia_asClient_returns403() throws Exception {
        mockMvc.perform(delete("/media/images/media-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteMedia_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/media/images/media-1"))
                .andExpect(status().isUnauthorized());
    }
}
