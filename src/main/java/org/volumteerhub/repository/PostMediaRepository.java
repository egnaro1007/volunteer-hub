package org.volumteerhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.volumteerhub.model.PostMedia;

import java.util.List;
import java.util.UUID;

public interface PostMediaRepository extends JpaRepository<PostMedia, UUID> {
    List<PostMedia> findByPostId(UUID postId);
}
