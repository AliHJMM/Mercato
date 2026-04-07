package com.buyapp.media.service;

import com.buyapp.media.dto.MediaDto;
import com.buyapp.media.entity.Media;
import com.buyapp.media.event.MediaEvent;
import com.buyapp.media.exception.AppException;
import com.buyapp.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    private final MediaRepository mediaRepository;
    private final MinioService minioService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MediaDto uploadImage(MultipartFile file, String productId) {
        validateFile(file);

        String sellerId = getCurrentUserId();
        String objectName = minioService.uploadFile(file);

        Media media = Media.builder()
                .filename(objectName)
                .originalFilename(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .size(file.getSize())
                .uploadedBy(sellerId)
                .productId(productId)
                .build();

        media = mediaRepository.save(media);
        media.setUrl(buildMediaUrl(media.getId()));
        media = mediaRepository.save(media);

        // Publish IMAGE_UPLOADED event
        try {
            kafkaTemplate.send("media-events", MediaEvent.builder()
                    .eventType("IMAGE_UPLOADED")
                    .mediaId(media.getId())
                    .filename(objectName)
                    .url(media.getUrl())
                    .uploadedBy(sellerId)
                    .productId(productId)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish media event: {}", e.getMessage());
        }

        return MediaDto.from(media);
    }

    public Media getMediaEntity(String id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new AppException("Media not found", HttpStatus.NOT_FOUND));
    }

    public MediaDto getMediaById(String id) {
        return MediaDto.from(getMediaEntity(id));
    }

    public List<MediaDto> getMyMedia() {
        String sellerId = getCurrentUserId();
        return mediaRepository.findByUploadedBy(sellerId)
                .stream()
                .map(MediaDto::from)
                .collect(Collectors.toList());
    }

    public void deleteMedia(String id) {
        String sellerId = getCurrentUserId();
        Media media = mediaRepository.findByIdAndUploadedBy(id, sellerId)
                .orElseThrow(() -> new AppException("Media not found or not owned by you", HttpStatus.NOT_FOUND));

        minioService.deleteFile(media.getFilename());
        mediaRepository.deleteById(id);

        log.info("Media {} deleted by seller {}", id, sellerId);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException("File is required", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(
                    "File size " + (file.getSize() / 1024 / 1024) + "MB exceeds the 2MB limit",
                    HttpStatus.BAD_REQUEST
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new AppException(
                    "Invalid file type. Only image files are allowed (jpeg, png, gif, webp, svg)",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Verify by sniffing file header bytes (defense against MIME spoofing)
        try {
            byte[] header = new byte[8];
            int read = file.getInputStream().read(header);
            if (read > 0 && !isImageByMagicBytes(header)) {
                throw new AppException("File content does not match image format", HttpStatus.BAD_REQUEST);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not verify file magic bytes: {}", e.getMessage());
        }
    }

    private boolean isImageByMagicBytes(byte[] header) {
        // JPEG: FF D8 FF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) return true;
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) return true;
        // GIF: 47 49 46 38
        if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38) return true;
        // WebP: starts with RIFF (52 49 46 46)
        if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46) return true;
        // SVG starts with <? or <s — allow content-type declaration to pass through
        if (header[0] == '<') return true;
        return false;
    }

    private String buildMediaUrl(String mediaId) {
        return "/api/media/images/" + mediaId;
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
