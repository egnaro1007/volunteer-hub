package org.volumteerhub.specification;

import org.springframework.data.jpa.domain.Specification;
import org.volumteerhub.common.EventStatus;
import org.volumteerhub.model.Event;

import java.util.UUID;

public class EventSpecifications {

    public static Specification<Event> hasStatus(EventStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Event> hasOwnerId(UUID ownerId) {
        return (root, query, cb) ->
                ownerId == null ? null : cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Event> nameContains(String search) {
        return (root, query, cb) ->
                search == null ? null : cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
    }
}

