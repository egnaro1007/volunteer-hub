package org.volumteerhub.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.hateoas.server.core.Relation;
import org.volumteerhub.common.RegistrationStatus;
import org.volumteerhub.common.validation.OnCreate;

import java.util.UUID;

@Data
@Relation(collectionRelation = "registrations")
public class RegistrationDto {

    private UUID id;

    @NotNull(groups = OnCreate.class)
    private UUID userId;

    private String username;

    @NotNull(groups = OnCreate.class)
    private UUID eventId;

    private String eventName;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus status;
}
