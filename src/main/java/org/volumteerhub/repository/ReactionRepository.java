package org.volumteerhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.volumteerhub.common.ReactionType;
import org.volumteerhub.model.PostReaction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public interface ReactionRepository extends JpaRepository<PostReaction, UUID> {
    Optional<PostReaction> findByPostIdAndUserId(UUID postId, UUID userId);

    @Query("SELECT r.reactionType AS reactionType, COUNT(r) AS count " +
            "FROM PostReaction r " +
            "WHERE r.post.id = :postId " +
            "GROUP BY r.reactionType")
    List<ReactionCountProjection> countReactionsByPostIdGroupedByTypeProjection(@Param("postId") UUID postId);

    default Map<ReactionType, Long> countReactionsByPostIdGroupedByType(UUID postId) {
        List<ReactionCountProjection> projections =
                countReactionsByPostIdGroupedByTypeProjection(postId);

        return projections.stream()
                .collect(Collectors.toMap(
                        ReactionCountProjection::getReactionType,
                        ReactionCountProjection::getCount
                ));
    }
}
