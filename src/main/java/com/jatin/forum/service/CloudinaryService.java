package com.jatin.forum.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jatin.forum.repository.PostRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final PostRepo postRepo;

    public CloudinaryService(Cloudinary cloudinary, PostRepo postRepo) {
        this.cloudinary = cloudinary;
        this.postRepo = postRepo;
    }

    public Map upload(MultipartFile file) throws IOException {
        // Determine resource type from content type
        String contentType = file.getContentType();
        String resourceType = "image"; // default
        if (contentType != null && contentType.startsWith("video")) {
            resourceType = "video";
        }

        return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", resourceType,
                "folder", "forum_posts"
        ));
    }


    public void delete(String publicId, String resourceType) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                "resource_type", resourceType != null ? resourceType : "image"
        ));
    }
}
