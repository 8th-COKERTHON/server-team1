package com.example.hackathon.domain.image.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String uploadMissionImage(Long missionLogId, MultipartFile image);

    void delete(String imageUrl);
}
