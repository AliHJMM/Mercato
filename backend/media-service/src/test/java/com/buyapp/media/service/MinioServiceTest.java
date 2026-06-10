package com.buyapp.media.service;

import com.buyapp.media.exception.AppException;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

    @Mock
    private MinioClient minioClient;

    private MinioService minioService;

    @BeforeEach
    void setUp() {
        minioService = new MinioService(minioClient);
        ReflectionTestUtils.setField(minioService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(minioService, "endpoint", "http://localhost:9000");
    }

    @Test
    void getPublicUrl_returnsCorrectFormat() {
        String url = minioService.getPublicUrl("my-object.jpg");

        assertThat(url).isEqualTo("http://localhost:9000/test-bucket/my-object.jpg");
    }

    @Test
    void uploadFile_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(minioClient.putObject(any())).thenReturn(mock(ObjectWriteResponse.class));

        String result = minioService.uploadFile(file);

        assertThat(result).endsWith("_photo.jpg");
        verify(minioClient).putObject(any());
    }

    @Test
    void uploadFile_minioFails_throwsAppException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        doThrow(new RuntimeException("MinIO unreachable")).when(minioClient).putObject(any());

        assertThatThrownBy(() -> minioService.uploadFile(file))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("store file");
    }

    @Test
    void uploadFile_filenameWithSpecialChars_sanitized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "my file (1).jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(minioClient.putObject(any())).thenReturn(mock(ObjectWriteResponse.class));

        String result = minioService.uploadFile(file);

        assertThat(result).doesNotContain(" ", "(", ")").endsWith("_my_file__1_.jpg");
    }

    @Test
    void uploadFile_nullFilename_usesDefault() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);
        when(file.getSize()).thenReturn(3L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(minioClient.putObject(any())).thenReturn(mock(ObjectWriteResponse.class));

        String result = minioService.uploadFile(file);

        assertThat(result).endsWith("_file");
    }

    @Test
    void deleteFile_success() throws Exception {
        doNothing().when(minioClient).removeObject(any());

        assertThatCode(() -> minioService.deleteFile("my-object.jpg")).doesNotThrowAnyException();
        verify(minioClient).removeObject(any());
    }

    @Test
    void deleteFile_minioFails_throwsAppException() throws Exception {
        doThrow(new RuntimeException("MinIO error")).when(minioClient).removeObject(any());

        assertThatThrownBy(() -> minioService.deleteFile("my-object.jpg"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("delete file");
    }

    @Test
    void downloadFile_minioFails_throwsAppException() throws Exception {
        doThrow(new RuntimeException("Object not found")).when(minioClient).getObject(any());

        assertThatThrownBy(() -> minioService.downloadFile("missing.jpg"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not found");
    }
}
