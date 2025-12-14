package org.volumteerhub.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.hateoas.server.core.Relation;
import org.volumteerhub.common.validation.OnCreate;
import org.volumteerhub.common.validation.OnUpdate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Relation(collectionRelation = "posts")
public class PostDto {

    // Read-only
    private UUID id;
    private UUID authorId;
    private String authorName;
    private Instant createdAt;
    private Instant updatedAt;

    // Writable
    @NotBlank(groups = {OnCreate.class, OnUpdate.class}, message = "Content cannot be empty")
    private String content;

    // Input: UUIDs returned from upload API
    @JsonIgnore
    private List<UUID> mediaIds;

    // Response: List of media URLs or IDs
    private List<String> mediaUrls;
}
