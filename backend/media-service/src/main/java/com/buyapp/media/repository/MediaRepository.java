package com.buyapp.media.repository;

import com.buyapp.media.entity.Media;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MediaRepository extends MongoRepository<Media, String> {
    List<Media> findByUploadedBy(String uploadedBy);
    List<Media> findByProductId(String productId);
    Optional<Media> findByIdAndUploadedBy(String id, String uploadedBy);
}
