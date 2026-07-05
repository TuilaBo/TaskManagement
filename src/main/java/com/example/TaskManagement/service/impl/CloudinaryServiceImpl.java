package com.example.TaskManagement.service.impl;

import com.example.TaskManagement.exception.BadRequestException;
import com.example.TaskManagement.service.CloudinaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final Pattern SECURE_URL_PATTERN =
            Pattern.compile("\"secure_url\"\\s*:\\s*\"([^\"]+)\"");

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Proof image is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Image size must not exceed 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Only JPEG, PNG, WEBP, GIF images are allowed");
        }

        try {
            long timestamp = Instant.now().getEpochSecond();
            String signature = generateSignature(timestamp);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "proof.jpg";
                }
            });
            body.add("api_key", apiKey);
            body.add("timestamp", String.valueOf(timestamp));
            body.add("signature", signature);
            body.add("folder", "task-management/proofs");

            String response = RestClient.create()
                    .post()
                    .uri("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            String url = extractSecureUrl(response);
            if (url != null) {
                log.info("Image uploaded to Cloudinary: {}", url);
                return url;
            }
            throw new BadRequestException("Failed to upload image to Cloudinary");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new BadRequestException("Failed to upload image: " + e.getMessage());
        }
    }

    private String extractSecureUrl(String response) {
        if (response == null) {
            return null;
        }
        Matcher matcher = SECURE_URL_PATTERN.matcher(response);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String generateSignature(long timestamp) throws Exception {
        String params = "folder=task-management/proofs&timestamp=" + timestamp + apiSecret;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(params.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
