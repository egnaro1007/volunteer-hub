package org.volumteerhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.volumteerhub.common.enumeration.RegistrationStatus;
import org.volumteerhub.model.Event;
import org.volumteerhub.model.Registration;
import org.volumteerhub.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, UUID>, JpaSpecificationExecutor<Registration> {

    List<Registration> findByUserId(UUID userId);

    List<Registration> findByEventId(UUID eventId);

    Optional<Registration> findByUserIdAndEventId(UUID userId, UUID eventId);

    List<Registration> findByStatus(RegistrationStatus status);

    List<Registration> findByStatusAndEventId(RegistrationStatus status, UUID eventId);

    Optional<Registration> getByUserAndEvent(User volunteer, Event event);
}
