package org.volumteerhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.volumteerhub.common.enumeration.ReactionType;
import org.volumteerhub.common.exception.BadRequestException;
import org.volumteerhub.common.exception.ResourceNotFoundException;
import org.volumteerhub.dto.PostDto;
import org.volumteerhub.model.*;
import org.volumteerhub.repository.EventRepository;
import org.volumteerhub.repository.PostMediaRepository;
import org.volumteerhub.repository.PostRepository;
import org.volumteerhub.repository.ReactionRepository;

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
    private final ReactionRepository reactionRepository;
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
                    .map(PostMedia::getPath)
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

        Post post = Post.builder()
                .content(dto.getContent())
                .user(currentUser)
                .event(event)
                .build();

        post = postRepository.save(post);

        handleMediaUploads(post, event.getId(), dto.getMediaFilenames());

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

        if (dto.getMediaFilenames() != null && !dto.getMediaFilenames().isEmpty()) {
            handleMediaUploads(post, post.getEvent().getId(), dto.getMediaFilenames());
        }

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

    /**
     * Extracted method to handle moving files and saving Media entities
     */
    private void handleMediaUploads(Post post, UUID eventId, List<String> tempFileNames) {
        if (tempFileNames == null || tempFileNames.isEmpty()) return;

        List<PostMedia> mediaList = new ArrayList<>();

        for (String fileName : tempFileNames) {
            try {
                // 1. Extract the UUID from the filename (removing the extension)
                // Example: "uuid.jpg" -> "uuid"
                String uuidString = fileName.contains(".")
                        ? fileName.substring(0, fileName.lastIndexOf("."))
                        : fileName;

                UUID tempFileId = UUID.fromString(uuidString);

                // 2. Move file using your hierarchical structure: /uploads/{eventId}/{postId}/
                // This preserves the extension as seen in StorageService.java
                String path = storageService.moveTempFileToPermanent(fileName, eventId, post.getId());

                // 3. Create DB record
                PostMedia media = PostMedia.builder()
                        .post(post)
                        .resourceId(tempFileId)
                        .path(path)
                        .build();

                mediaList.add(media);
            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID format in filename: {}", fileName);
                throw new BadRequestException("Invalid media ID format");
            } catch (IOException e) {
                log.error("Failed to move media file {}", fileName, e);
                throw new RuntimeException("Failed to attach media", e);
            }
        }

        postMediaRepository.saveAll(mediaList);

        // Sync the post's media list for the DTO conversion
        if (post.getMedias() == null) {
            post.setMedias(mediaList);
        } else {
            post.getMedias().addAll(mediaList);
        }
    }


    // REACTION
    /**
     * Applies a new reaction or updates an existing one for the current user.
     */
    @Transactional
    public void react(UUID postId, ReactionType newReactionType) {
        if (newReactionType == ReactionType.NONE) {
            // Treat NONE as a request to delete the reaction
            this.deleteReaction(postId);
            return;
        }

        User currentUser = userService.getCurrentAuthenticatedUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        // 1. Check for existing reaction
        reactionRepository.findByPostIdAndUserId(postId, currentUser.getId())
                .ifPresentOrElse(
                        // 2. Update existing reaction
                        reaction -> {
                            reaction.setReactionType(newReactionType);
                            reactionRepository.save(reaction);
                        },
                        // 3. Create new reaction
                        () -> {
                            PostReaction reaction = PostReaction.builder()
                                    .post(post)
                                    .user(currentUser)
                                    .reactionType(newReactionType)
                                    .build();
                            reactionRepository.save(reaction);
                        }
                );
    }

    /**
     * Gets the current user's reaction from a post.
     */
    public ReactionType getReaction(UUID postId) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        return reactionRepository.findByPostIdAndUserId(postId, currentUser.getId())
                .map(PostReaction::getReactionType)
                .orElse(ReactionType.NONE);
    }

    /**
     * Removes the current user's reaction from a post.
     */
    @Transactional
    public void deleteReaction(UUID postId) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        reactionRepository.findByPostIdAndUserId(postId, currentUser.getId())
                .ifPresent(reactionRepository::delete);

    }
}
