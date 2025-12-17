package org.volumteerhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.volumteerhub.model.PushSubscription;

import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

    Optional<PushSubscription> findByEndpoint(String endpoint);
}
