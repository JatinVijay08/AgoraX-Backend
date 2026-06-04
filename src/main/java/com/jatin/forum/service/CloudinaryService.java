package com.jatin.forum.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jatin.forum.repository.PostRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final PostRepo postRepo;

    public CloudinaryService(Cloudinary cloudinary, PostRepo postRepo) {
        this.cloudinary = cloudinary;
        this.postRepo = postRepo;
    }

    public Map upload(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String resourceType = "image"; // default
        if (contentType != null && contentType.startsWith("video")) {
            resourceType = "video";
        }

        log.info("[SERVICE] Uploading file to Cloudinary. Original Name: {}, Content Type: {}, Resource Type: {}", 
                 file.getOriginalFilename(), contentType, resourceType);

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", resourceType,
                "folder", "forum_posts"
        ));
        
        log.info("[SERVICE] Cloudinary upload successful. Public ID: {}, URL: {}", 
                 uploadResult.get("public_id"), uploadResult.get("secure_url"));
        return uploadResult;
    }


    public void delete(String publicId, String resourceType) throws IOException {
        log.info("[SERVICE] Deleting file from Cloudinary. Public ID: {}, Resource Type: {}", publicId, resourceType);
        Map destroyResult = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                "resource_type", resourceType != null ? resourceType : "image"
        ));
        log.info("[SERVICE] Cloudinary destruction result for {}: {}", publicId, destroyResult);
    }
}
