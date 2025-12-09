package org.volumteerhub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.volumteerhub.dto.EventDto;
import org.volumteerhub.common.exception.ResourceNotFoundException;
import org.volumteerhub.common.exception.UnauthorizedAccessException;
import org.volumteerhub.common.EventStatus;
import org.volumteerhub.common.UserRole;
import org.volumteerhub.model.Event;
import org.volumteerhub.model.User;
import org.volumteerhub.repository.EventRepository;
import org.volumteerhub.repository.UserRepository;
import org.volumteerhub.specification.EventSpecifications;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("User is not authenticated.");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    private boolean isCurrentUserAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }

    private void validateOwnership(Event event, User currentUser) {
        if (!event.getOwner().equals(currentUser)) {
            throw new UnauthorizedAccessException("User is not the owner of event " + event.getId());
        }
    }

    private Event findEventById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
    }


    private EventDto toDto(Event event) {
        // Use Lombok's @Builder on DTO or a proper mapper for cleaner code
        EventDto dto = new EventDto();
        dto.setId(event.getId());
        dto.setName(event.getName());
        dto.setDescription(event.getDescription());
        dto.setDateDeadline(event.getDateDeadline());
        dto.setStartDate(event.getStartDate());
        dto.setEndDate(event.getEndDate());
        dto.setStatus(event.getStatus());
        dto.setOwnerId(event.getOwner().getId());
        return dto;
    }

    // CREATE
    public EventDto create(EventDto dto) {
        User owner = getCurrentAuthenticatedUser();

        Event event = Event.builder()
                .owner(owner)
                .name(dto.getName())
                .description(dto.getDescription())
                .dateDeadline(dto.getDateDeadline())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(EventStatus.DRAFT)
                .build();

        return toDto(eventRepository.save(event));
    }

    // READ BY ID
    public EventDto get(UUID id) {
        Event event = findEventById(id);
        User currentUser = getCurrentAuthenticatedUser();

        if (event.getStatus() != EventStatus.APPROVED) {
            if (!isCurrentUserAdmin(currentUser)) {
                validateOwnership(event, currentUser);
            }
        }
        return toDto(event);
    }

    // LIST + FILTER + PAGE
    public Page<EventDto> list(EventStatus status, UUID ownerId, String search, Pageable pageable) {
        User currentUser = getCurrentAuthenticatedUser();

        Specification<Event> baseFilter = Specification.allOf(
                EventSpecifications.hasStatus(status),
                EventSpecifications.hasOwnerId(ownerId),
                EventSpecifications.nameContains(search)
        );

        // Rule 1: Admin can see everything
        if (isCurrentUserAdmin(currentUser)) {
            return eventRepository.findAll(baseFilter, pageable).map(this::toDto);
        }

        Specification<Event> securitySpec = ((root, query, criteriaBuilder) -> {
            // Rule 2: event.status == 'APPROVED' (Publicly visible)
            jakarta.persistence.criteria.Predicate approvedStatus = criteriaBuilder.equal(
                    root.get("status"), EventStatus.APPROVED
            );

            // Rule 3: event.ownerId == currentUser.id (Owner can see their own)
            jakarta.persistence.criteria.Predicate isOwner = criteriaBuilder.equal(
                    root.get("owner").get("id"), currentUser.getId()
            );

            return criteriaBuilder.or(approvedStatus, isOwner);
        });

        Specification<Event> finalSpec = baseFilter.and(securitySpec);

        return eventRepository.findAll(finalSpec, pageable).map(this::toDto);
    }

    // UPDATE
    public EventDto update(UUID id, EventDto dto) {
        Event event = findEventById(id);
        User currentUser = getCurrentAuthenticatedUser();
        validateOwnership(event, currentUser);

        if (dto.getName() != null) event.setName(dto.getName());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getDateDeadline() != null) event.setDateDeadline(dto.getDateDeadline());
        if (dto.getStartDate() != null) event.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) event.setEndDate(dto.getEndDate());

        return toDto(eventRepository.save(event));
    }

    // DELETE
    public void delete(UUID id) {
        Event event = findEventById(id);
        User currentUser = getCurrentAuthenticatedUser();
        validateOwnership(event, currentUser);

        eventRepository.deleteById(id);
    }

    // Submit to admin
    public EventDto submit(UUID id) {
        Event event = findEventById(id);
        User currentUser = getCurrentAuthenticatedUser();
        validateOwnership(event, currentUser);

        if (event.getStatus() == EventStatus.DRAFT || event.getStatus() == EventStatus.REJECTED) {
            event.setStatus(EventStatus.PENDING);
            eventRepository.save(event);
        }

        return toDto(event);
    }


    private EventDto updateEventStatus(UUID eventId, EventStatus requiredStatus, EventStatus newStatus) {
        Event event = findEventById(eventId);

        if (event.getStatus() == requiredStatus) {
            event.setStatus(newStatus);
            eventRepository.save(event);
        }

        return toDto(event);
    }

    // Approve
    public EventDto approve(UUID id) {
        User currentUser = getCurrentAuthenticatedUser();
        if (!isCurrentUserAdmin(currentUser)) {
            throw new UnauthorizedAccessException("Admin only operation.");
        }

        return updateEventStatus(id, EventStatus.PENDING, EventStatus.APPROVED);
    }

    // Reject
    public EventDto reject(UUID id) {
        User currentUser = getCurrentAuthenticatedUser();
        if (!isCurrentUserAdmin(currentUser)) {
            throw new UnauthorizedAccessException("Admin only operation.");
        }

        return updateEventStatus(id, EventStatus.PENDING, EventStatus.REJECTED);
    }
}
