package org.volumteerhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.volumteerhub.common.exception.ResourceNotFoundException;
import org.volumteerhub.dto.PostDto;
import org.volumteerhub.model.Event;
import org.volumteerhub.model.Post;
import org.volumteerhub.model.PostMedia;
import org.volumteerhub.model.User;
import org.volumteerhub.repository.EventRepository;
import org.volumteerhub.repository.PostMediaRepository;
import org.volumteerhub.repository.PostRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final EventRepository eventRepository;
    private final PostMediaRepository postMediaRepository;
    private final UserService userService;
    private final StorageService storageService;


    private PostDto toDto(Post post) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setContent(post.getContent());
        dto.setAuthorId(post.getUser().getId());
        dto.setAuthorName(post.getUser().getFirstname() + " " + post.getUser().getLastname());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());

        if (post.getMedias() != null) {
            List<String> urls = post.getMedias().stream()
                    .map(media -> "/uploads/" + post.getId() + "/" + media.getResourceId())
                    .collect(Collectors.toList());
            dto.setMediaUrls(urls);
        }

        return dto;
    }

    // LIST (Pagination handled here)
    @Transactional(readOnly = true)
    public Page<PostDto> listByEvent(UUID eventId, Pageable pageable) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }
        // Map Page<Entity> to Page<Dto>
        return postRepository.findByEventId(eventId, pageable).map(this::toDto);
    }

    // CREATE
    @Transactional
    public PostDto create(UUID eventId, PostDto dto) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // 1. Save the Post first to get an ID
        Post post = Post.builder()
                .content(dto.getContent())
                .user(currentUser)
                .event(event)
                .build();

        post = postRepository.save(post);

        // 2. Handle Media (Move from Temp -> Permanent)
        if (dto.getMediaIds() != null && !dto.getMediaIds().isEmpty()) {
            List<PostMedia> mediaList = new ArrayList<>();

            for (UUID tempFileId : dto.getMediaIds()) {
                try {
                    // Move file on disk
                    storageService.moveTempFileToPermanent(tempFileId, post.getId());

                    // Create DB record
                    PostMedia media = PostMedia.builder()
                            .post(post)
                            .resourceId(tempFileId) // The file keeps its UUID name
                            .build();

                    mediaList.add(media);
                } catch (IOException e) {
                    log.error("Failed to move media file {}", tempFileId, e);
                    // Optional: Throw exception to rollback transaction
                    throw new RuntimeException("Failed to attach media", e);
                }
            }
            // Save all media records
            postMediaRepository.saveAll(mediaList);
            post.setMedias(mediaList); // Update reference for DTO conversion
        }

        return toDto(post);
    }

    // GET
    @Transactional(readOnly = true)
    public PostDto get(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return toDto(post);
    }

    // UPDATE
    @Transactional
    public PostDto update(UUID postId, PostDto dto) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        User currentUser = userService.getCurrentAuthenticatedUser();

        userService.validateOwnerOrAdmin(post.getUser(), currentUser);

        post.setContent(dto.getContent());

        return toDto(postRepository.save(post));
    }

    // DELETE
    @Transactional
    public void delete(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        User currentUser = userService.getCurrentAuthenticatedUser();

        // Event Owner can also delete posts on their wall
        boolean isEventOwner = post.getEvent().getOwner().getId().equals(currentUser.getId());
        if (!isEventOwner) {
            userService.validateOwnerOrAdmin(post.getUser(), currentUser);
        }

        postRepository.delete(post);
    }
}
