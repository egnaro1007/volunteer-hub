package org.volumteerhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.volumteerhub.common.RegistrationStatus;
import org.volumteerhub.model.Registration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    List<Registration> findByUserId(UUID userId);

    List<Registration> findByEventId(UUID eventId);

    Optional<Registration> findByUserIdAndEventId(UUID userId, UUID eventId);

    List<Registration> findByStatus(RegistrationStatus status);

    List<Registration> findByStatusAndEventId(RegistrationStatus status, UUID eventId);
}
