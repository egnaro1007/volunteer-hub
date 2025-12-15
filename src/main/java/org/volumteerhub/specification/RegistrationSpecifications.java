package org.volumteerhub.specification;

import org.springframework.data.jpa.domain.Specification;
import org.volumteerhub.common.enumeration.RegistrationStatus;
import org.volumteerhub.model.Registration;

import java.util.UUID;

public class RegistrationSpecifications {

    public static Specification<Registration> hasStatus(RegistrationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Registration> hasEventId(UUID eventId) {
        return (root, query, cb) -> eventId == null ? null : cb.equal(root.get("event").get("id"), eventId);
    }

    public static Specification<Registration> hasUserId(UUID userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("user").get("id"), userId);
    }
}