package com.buyapp.media.config;

import com.buyapp.media.entity.Media;
import com.buyapp.media.repository.MediaRepository;
import com.buyapp.media.service.MinioService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaCleanupRunner implements ApplicationRunner {

    private final MongoClient mongoClient;
    private final MediaRepository mediaRepository;
    private final MinioService minioService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking for legacy client media to clean up...");

        // Read CLIENT user IDs directly from userdb (same MongoDB server)
        MongoCollection<Document> users =
                mongoClient.getDatabase("userdb").getCollection("users");

        List<String> clientIds = new ArrayList<>();
        for (Document user : users.find(new Document("role", "CLIENT"))) {
            Object id = user.get("_id");
            if (id != null) {
                clientIds.add(id.toString());
            }
        }

        if (clientIds.isEmpty()) {
            log.info("No CLIENT users found — nothing to clean up.");
            return;
        }

        log.info("Found {} CLIENT user(s). Scanning for orphaned media...", clientIds.size());

        int deleted = 0;
        for (String clientId : clientIds) {
            List<Media> clientMedia = mediaRepository.findByUploadedBy(clientId);
            for (Media media : clientMedia) {
                try {
                    minioService.deleteFile(media.getFilename());
                    log.info("Deleted MinIO object: {}", media.getFilename());
                } catch (Exception e) {
                    log.warn("Could not delete MinIO object {} (may already be gone): {}",
                            media.getFilename(), e.getMessage());
                }
                mediaRepository.deleteById(media.getId());
                deleted++;
            }
        }

        if (deleted > 0) {
            log.info("Client media cleanup complete — removed {} record(s).", deleted);
        } else {
            log.info("Client media cleanup complete — no orphaned records found.");
        }
    }
}
