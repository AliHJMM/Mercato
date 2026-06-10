package com.buyapp.media.consumer;

import com.buyapp.media.entity.Media;
import com.buyapp.media.repository.MediaRepository;
import com.buyapp.media.service.MinioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final MediaRepository mediaRepository;
    private final MinioService minioService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-events", groupId = "media-service")
    public void handleUserEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.path("eventType").asText();

            if ("USER_DELETED".equals(eventType)) {
                String userId = event.path("userId").asText();
                if (!userId.isBlank()) {
                    List<Media> mediaList = mediaRepository.findByUploadedBy(userId);
                    for (Media media : mediaList) {
                        deleteMediaFile(media, userId);
                    }
                    if (!mediaList.isEmpty()) {
                        log.info("Deleted {} media file(s) for deleted user {}", mediaList.size(), userId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process user event: {}", e.getMessage());
        }
    }

    private void deleteMediaFile(Media media, String userId) {
        try {
            minioService.deleteFile(media.getFilename());
        } catch (Exception e) {
            log.warn("Failed to delete MinIO file {} for deleted user {}: {}", media.getFilename(), userId, e.getMessage());
        }
        mediaRepository.deleteById(media.getId());
    }
}
