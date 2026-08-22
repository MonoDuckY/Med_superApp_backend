package com.yourproject.backend.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.yourproject.backend.dtos.requests.NewsRequest;
import com.yourproject.backend.dtos.responses.NewsResponse;
import com.yourproject.backend.dtos.responses.NewsResponse.Attachment;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.models.News;
import com.yourproject.backend.models.NewsStatus;
import com.yourproject.backend.repositories.NewsRepository;
import com.yourproject.backend.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NewsService {
    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    public NewsResponse create(String staffId, NewsRequest request, MultipartFile coverPhoto) {
        NewsStatus status = request.getStatus() == null ? NewsStatus.DRAFT : request.getStatus();
        validatePublishedContent(status, request.getContent());
        Instant now = Instant.now();
        News news = News.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .uploadBy(staffId)
                .status(status)
                .uploadTime(now)
                .image(new ArrayList<>())
                .updateTime(now)
                .build();
        news = newsRepository.save(news);
        if (coverPhoto != null && !coverPhoto.isEmpty()) {
            news.setCoverPhoto(s3StorageService.uploadNewsCoverPhoto(news.getNewsId(), coverPhoto));
        }
        return toResponse(newsRepository.save(news));
    }

    public NewsResponse update(String staffId, String newsId, NewsRequest request, MultipartFile coverPhoto) {
        News news = find(newsId);
        news.setTitle(request.getTitle());
        news.setContent(request.getContent());
        if (request.getStatus() != null) {
            news.setStatus(request.getStatus());
        }
        validatePublishedContent(news.getStatus(), news.getContent());
        news.setUpdateTime(Instant.now());
        if (coverPhoto != null && !coverPhoto.isEmpty()) {
            if (news.getCoverPhoto() != null) {
                s3StorageService.deleteObject(news.getCoverPhoto());
            }
            news.setCoverPhoto(s3StorageService.uploadNewsCoverPhoto(news.getNewsId(), coverPhoto));
        }
        return toResponse(newsRepository.save(news));
    }

    public NewsResponse addContentImages(String newsId, List<MultipartFile> images) {
        News news = find(newsId);
        addAttachments(news, images);
        news.setUpdateTime(Instant.now());
        return toResponse(newsRepository.save(news));
    }

    public NewsResponse publish(String newsId) {
        News news = find(newsId);
        validatePublishedContent(NewsStatus.PUBLISHED, news.getContent());
        news.setStatus(NewsStatus.PUBLISHED);
        news.setUpdateTime(Instant.now());
        return toResponse(newsRepository.save(news));
    }

    public NewsResponse disable(String newsId) {
        News news = find(newsId);
        news.setStatus(NewsStatus.DISABLED);
        news.setUpdateTime(Instant.now());
        return toResponse(newsRepository.save(news));
    }

    public List<NewsResponse> getStaffNews() {
        return newsRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<NewsResponse> getPublishedNews() {
        return newsRepository.findAllByStatusOrderByUploadTimeDesc(NewsStatus.PUBLISHED)
                .stream().map(this::toResponse).toList();
    }

    public NewsResponse getPublished(String newsId) {
        News news = find(newsId);
        if (news.getStatus() != NewsStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Published news was not found.");
        }
        return toResponse(news);
    }

    private void addAttachments(News news, List<MultipartFile> attachments) {
        if (attachments == null) {
            return;
        }
        if (news.getImage() == null) {
            news.setImage(new ArrayList<>());
        }
        for (MultipartFile attachment : attachments) {
            if (attachment != null && !attachment.isEmpty()) {
                news.getImage().add(
                        s3StorageService.uploadNewsAttachment(news.getNewsId(), attachment));
            }
        }
    }

    private News find(String newsId) {
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("News article was not found."));
    }

    private NewsResponse toResponse(News news) {
        List<Attachment> attachments = news.getImage() == null
                ? List.of()
                : news.getImage().stream()
                        .map(key -> {
                            S3StorageService.PresignedObjectUrl signed = s3StorageService.createPresignedGetUrl(key);
                            return Attachment.builder().url(signed.url()).expiresAt(signed.expiresAt()).build();
                        }).toList();
        Attachment cover = null;
        if (news.getCoverPhoto() != null) {
            S3StorageService.PresignedObjectUrl signed = s3StorageService.createPresignedGetUrl(news.getCoverPhoto());
            cover = Attachment.builder().url(signed.url()).expiresAt(signed.expiresAt()).build();
        }
        return NewsResponse.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .content(news.getContent())
                .uploadBy(resolveUploaderName(news.getUploadBy()))
                .image(attachments)
                .coverPhoto(cover)
                .status(news.getStatus() == null ? null : news.getStatus().name())
                .uploadTime(news.getUploadTime())
                .updateTime(news.getUpdateTime() == null ? news.getUploadTime() : news.getUpdateTime())
                .build();
    }

    private String resolveUploaderName(String uploaderId) {
        if (uploaderId == null || uploaderId.isBlank()) {
            return uploaderId;
        }
        return userRepository.findById(uploaderId)
                .map(user -> user.getFullName() == null || user.getFullName().isBlank()
                        ? uploaderId
                        : user.getFullName())
                .orElse(uploaderId);
    }

    private void validatePublishedContent(NewsStatus status, String content) {
        if (status == NewsStatus.PUBLISHED && (content == null || content.isBlank())) {
            throw new BadRequestException("News content is required before publishing.");
        }
    }
}
