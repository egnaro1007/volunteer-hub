package org.volumteerhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.volumteerhub.model.Post;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    Page<Post> findByEventId(UUID eventId, Pageable pageable);

    List<Post> getPostsByEventId(UUID eventId);
    List<Post> getPostsByUserId(UUID userId);
    List<Post> getPostsByEventIdAndUserId(UUID eventId, UUID userId);
}
