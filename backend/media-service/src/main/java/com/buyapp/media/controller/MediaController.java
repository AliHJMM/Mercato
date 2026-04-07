package com.buyapp.media.controller;

import com.buyapp.media.dto.MediaDto;
import com.buyapp.media.entity.Media;
import com.buyapp.media.service.MediaService;
import com.buyapp.media.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final MinioService minioService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<MediaDto> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "productId", required = false) String productId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaService.uploadImage(file, productId));
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<InputStreamResource> serveImage(@PathVariable String id) {
        Media media = mediaService.getMediaEntity(id);
        InputStream stream = minioService.downloadFile(media.getFilename());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.getMimeType()))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + media.getOriginalFilename() + "\"")
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/images/{id}/info")
    public ResponseEntity<MediaDto> getMediaInfo(@PathVariable String id) {
        return ResponseEntity.ok(mediaService.getMediaById(id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<MediaDto>> getMyMedia() {
        return ResponseEntity.ok(mediaService.getMyMedia());
    }

    @DeleteMapping("/images/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> deleteMedia(@PathVariable String id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.noContent().build();
    }
}
