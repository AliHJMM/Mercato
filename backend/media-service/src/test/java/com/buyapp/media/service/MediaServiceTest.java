package com.buyapp.media.service;

import com.buyapp.media.dto.MediaDto;
import com.buyapp.media.entity.Media;
import com.buyapp.media.exception.AppException;
import com.buyapp.media.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock private MediaRepository mediaRepository;
    @Mock private MinioService minioService;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks private MediaService mediaService;

    private static final String SELLER_ID = "seller-001";

    @BeforeEach
    void setUp() {
        var auth = new UsernamePasswordAuthenticationToken(
                SELLER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private byte[] jpegBytes() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46};
    }

    private byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }

    private Media buildMedia(String id) {
        Media m = new Media();
        m.setId(id);
        m.setFilename("object-uuid.jpg");
        m.setOriginalFilename("test.jpg");
        m.setMimeType("image/jpeg");
        m.setSize(1024L);
        m.setUrl("http://minio/bucket/object-uuid.jpg");
        m.setUploadedBy(SELLER_ID);
        m.setCreatedAt(LocalDateTime.now());
        return m;
    }

    @Test
    void uploadImage_validJpeg_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", jpegBytes());

        when(minioService.uploadFile(any())).thenReturn("object-uuid.jpg");
        when(mediaRepository.save(any())).thenAnswer(i -> {
            Media m = i.getArgument(0);
            m.setId("media-1");
            return m;
        });

        MediaDto result = mediaService.uploadImage(file, "prod-1");

        assertThat(result.getMimeType()).isEqualTo("image/jpeg");
        assertThat(result.getUploadedBy()).isEqualTo(SELLER_ID);
        verify(kafkaTemplate).send(eq("media-events"), any());
    }

    @Test
    void uploadImage_validPng_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", pngBytes());

        when(minioService.uploadFile(any())).thenReturn("object-uuid.png");
        when(mediaRepository.save(any())).thenAnswer(i -> {
            Media m = i.getArgument(0);
            m.setId("media-2");
            return m;
        });

        MediaDto result = mediaService.uploadImage(file, null);

        assertThat(result.getMimeType()).isEqualTo("image/png");
    }

    @Test
    void uploadImage_invalidMimeType_throws400() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46});

        assertThatThrownBy(() -> mediaService.uploadImage(file, null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("type");
    }

    @Test
    void uploadImage_fileTooLarge_throws400() {
        byte[] largeContent = new byte[3 * 1024 * 1024]; // 3MB
        largeContent[0] = (byte) 0xFF;
        largeContent[1] = (byte) 0xD8;
        largeContent[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", largeContent);

        assertThatThrownBy(() -> mediaService.uploadImage(file, null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("2MB");
    }

    @Test
    void uploadImage_mimeSpoofing_throws400() {
        // Claims to be JPEG but bytes are plain text
        byte[] spoofedBytes = "not-a-real-image".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.jpg", "image/jpeg", spoofedBytes);

        assertThatThrownBy(() -> mediaService.uploadImage(file, null))
                .isInstanceOf(AppException.class);
    }

    @Test
    void getMediaById_exists_returnsDto() {
        when(mediaRepository.findById("media-1")).thenReturn(Optional.of(buildMedia("media-1")));

        MediaDto result = mediaService.getMediaById("media-1");

        assertThat(result.getId()).isEqualTo("media-1");
        assertThat(result.getUploadedBy()).isEqualTo(SELLER_ID);
    }

    @Test
    void getMediaById_notFound_throws404() {
        when(mediaRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.getMediaById("missing"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getMyMedia_returnsList() {
        when(mediaRepository.findByUploadedBy(SELLER_ID)).thenReturn(List.of(buildMedia("m1"), buildMedia("m2")));

        List<MediaDto> result = mediaService.getMyMedia();

        assertThat(result).hasSize(2);
    }

    @Test
    void deleteMedia_ownedBySeller_success() {
        Media media = buildMedia("media-1");
        when(mediaRepository.findByIdAndUploadedBy("media-1", SELLER_ID))
                .thenReturn(Optional.of(media));

        mediaService.deleteMedia("media-1");

        verify(minioService).deleteFile("object-uuid.jpg");
        verify(mediaRepository).deleteById("media-1");
    }

    @Test
    void deleteMedia_notOwnedBySeller_throws404() {
        when(mediaRepository.findByIdAndUploadedBy("media-1", SELLER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.deleteMedia("media-1"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void uploadImage_validGif_success() {
        byte[] gifBytes = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "anim.gif", "image/gif", gifBytes);

        when(minioService.uploadFile(any())).thenReturn("object-uuid.gif");
        when(mediaRepository.save(any())).thenAnswer(i -> {
            Media m = i.getArgument(0);
            m.setId("media-3");
            return m;
        });

        MediaDto result = mediaService.uploadImage(file, "prod-1");

        assertThat(result.getMimeType()).isEqualTo("image/gif");
    }

    @Test
    void uploadImage_validWebp_success() {
        byte[] webpBytes = new byte[]{0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "image.webp", "image/webp", webpBytes);

        when(minioService.uploadFile(any())).thenReturn("object-uuid.webp");
        when(mediaRepository.save(any())).thenAnswer(i -> {
            Media m = i.getArgument(0);
            m.setId("media-4");
            return m;
        });

        MediaDto result = mediaService.uploadImage(file, null);

        assertThat(result.getMimeType()).isEqualTo("image/webp");
    }

    @Test
    void uploadImage_validSvg_success() {
        byte[] svgBytes = "<svg xmlns".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "icon.svg", "image/svg+xml", svgBytes);

        when(minioService.uploadFile(any())).thenReturn("object-uuid.svg");
        when(mediaRepository.save(any())).thenAnswer(i -> {
            Media m = i.getArgument(0);
            m.setId("media-5");
            return m;
        });

        MediaDto result = mediaService.uploadImage(file, null);

        assertThat(result.getMimeType()).isEqualTo("image/svg+xml");
    }

    @Test
    void uploadImage_emptyFile_throws400() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> mediaService.uploadImage(file, null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("required");
    }

    @Test
    void uploadImage_kafkaFailure_stillReturnsDto() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegBytes());

        when(minioService.uploadFile(any())).thenReturn("object-uuid.jpg");
        when(mediaRepository.save(any())).thenAnswer(i -> {
            Media m = i.getArgument(0);
            m.setId("media-6");
            return m;
        });
        doThrow(new RuntimeException("Kafka down")).when(kafkaTemplate).send(anyString(), any());

        MediaDto result = mediaService.uploadImage(file, "prod-1");

        assertThat(result).isNotNull();
        assertThat(result.getMimeType()).isEqualTo("image/jpeg");
    }

    @Test
    void getMediaEntity_exists_returnsMedia() {
        Media media = buildMedia("media-1");
        when(mediaRepository.findById("media-1")).thenReturn(Optional.of(media));

        Media result = mediaService.getMediaEntity("media-1");

        assertThat(result.getId()).isEqualTo("media-1");
    }

    @Test
    void getMediaEntity_notFound_throws404() {
        when(mediaRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.getMediaEntity("missing"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not found");
    }
}
