package org.volumteerhub.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.hateoas.server.core.Relation;
import org.volumteerhub.common.EventStatus;
import org.volumteerhub.common.validation.OnCreate;

import java.time.Instant;
import java.util.UUID;

@Data
@Relation(collectionRelation = "events")
public class EventDto {

    // Read-only (use on GET)
    private UUID id;

    // CREATE required
    @NotBlank(groups = OnCreate.class)
    private String name;

    private String description;

    @NotNull(groups = OnCreate.class)
    private Instant dateDeadline;

    @NotNull(groups = OnCreate.class)
    private Instant startDate;

    @NotNull(groups = OnCreate.class)
    private Instant endDate;

    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    private EventStatus status;
}

