package org.volumteerhub.repository;

import org.volumteerhub.common.ReactionType;

public interface ReactionCountProjection {
    ReactionType getReactionType();
    Long getCount();
}
