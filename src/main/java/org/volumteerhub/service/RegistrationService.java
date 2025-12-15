package org.volumteerhub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.volumteerhub.common.enumeration.EventStatus;
import org.volumteerhub.common.enumeration.RegistrationStatus;
import org.volumteerhub.common.exception.ResourceNotFoundException;
import org.volumteerhub.common.exception.UnauthorizedAccessException;
import org.volumteerhub.dto.RegistrationDto;
import org.volumteerhub.model.Event;
import org.volumteerhub.model.Registration;
import org.volumteerhub.model.User;
import org.volumteerhub.repository.EventRepository;
import org.volumteerhub.repository.RegistrationRepository;
import org.volumteerhub.specification.RegistrationSpecifications;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserService securityService;

    // --- MAPPERS ---

    private RegistrationDto toDto(Registration registration) {
        RegistrationDto dto = new RegistrationDto();
        dto.setId(registration.getId());
        dto.setUserId(registration.getUser().getId());
        dto.setEventId(registration.getEvent().getId());
        dto.setStatus(registration.getStatus());

        dto.setEventName(registration.getEvent().getName());
        dto.setUsername(registration.getUser().getUsername());
        return dto;
    }


    // --- COMMON ---

    /**
     * Get a Registration
     */
    public RegistrationDto getRegistration(UUID id) {
        Registration registration = findRegistrationById(id);
        User currentUser = securityService.getCurrentAuthenticatedUser();

        // Access Rule: Admin, The Volunteer, or The Event Manager
        boolean isVolunteer = registration.getUser().equals(currentUser);
        boolean isEventManager = registration.getEvent().getOwner().equals(currentUser);
        boolean isAdmin = securityService.isCurrentUserAdmin();

        if (!isVolunteer && !isEventManager && !isAdmin) {
            throw new UnauthorizedAccessException("You do not have permission to view this registration.");
        }

        return toDto(registration);
    }

    /**
     * List all Registration meet the criteria.
     * @param status Filter by status
     * @param eventId Filter by event id
     * @param userId Filter by volunteer id
     * @param pageable Pageable
     * @return All the registration DTOs.
     */
    @Transactional(readOnly = true)
    public Page<RegistrationDto> list(RegistrationStatus status, UUID eventId, UUID userId, Pageable pageable) {
        User currentUser = securityService.getCurrentAuthenticatedUser();

        // 1. Build Base Filter (User-provided criteria)
        Specification<Registration> baseFilter = Specification.allOf(
                RegistrationSpecifications.hasStatus(status),
                RegistrationSpecifications.hasEventId(eventId),
                RegistrationSpecifications.hasUserId(userId)
        );

        // 2. Rule: Admin can see everything
        if (securityService.isCurrentUserAdmin()) {
            return registrationRepository.findAll(baseFilter, pageable).map(this::toDto);
        }

        // 3. Define Security Specification (Non-Admin visibility rules)
        Specification<Registration> securitySpec = (root, query, criteriaBuilder) -> {
            // Rule A: The registration belongs to the current user (I am the Volunteer)
            jakarta.persistence.criteria.Predicate isVolunteer = criteriaBuilder.equal(
                    root.get("user").get("id"), currentUser.getId()
            );

            // Rule B: The registration belongs to an event owned by the current user (I am the Event Manager)
            jakarta.persistence.criteria.Predicate isEventManager = criteriaBuilder.equal(
                    root.get("event").get("owner").get("id"), currentUser.getId()
            );

            // Access is allowed if Rule A OR Rule B is met
            return criteriaBuilder.or(isVolunteer, isEventManager);
        };

        // 4. Combine Base Filter and Security Specification
        Specification<Registration> finalSpec = baseFilter.and(securitySpec);

        return registrationRepository.findAll(finalSpec, pageable).map(this::toDto);
    }


    // --- VOLUNTEER ACTIONS ---

    /**
     * Volunteer joins an event.
     */
    @Transactional
    public RegistrationDto joinEvent(UUID eventId) {
        User volunteer = securityService.getCurrentAuthenticatedUser();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        // 1. Validation: Event must be approved to receive joins
        if (event.getStatus() != EventStatus.APPROVED) {
            throw new ResourceNotFoundException("Event not found or not approved.");
        }

        // 2. Validation: Check deadline
        if (event.getDateDeadline() != null && Instant.now().isAfter(event.getDateDeadline())) {
            throw new IllegalArgumentException("Registration deadline has passed for this event.");
        }

        Optional<Registration> registration = registrationRepository.getByUserAndEvent(volunteer, event);

        return registration.map(this::toDto)
                .orElseGet(() -> {
                    Registration newRegistration = Registration.builder()
                            .user(volunteer)
                            .event(event)
                            .status(RegistrationStatus.PENDING)
                            .build();
                    return toDto(registrationRepository.save(newRegistration));
                });
    }

    /**
     * Volunteer cancels their join request by Event ID.
     */
    @Transactional
    public void cancelJoinByEventId(UUID eventId) {
        User volunteer = securityService.getCurrentAuthenticatedUser();
        Event event = eventRepository.getReferenceById(eventId);

        Registration registration = registrationRepository.findByUserIdAndEventId(volunteer.getId(), event.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found for this event."));

        // Prevent cancelling if already COMPLETED
        if (registration.getStatus() == RegistrationStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed participation.");
        }

        registrationRepository.delete(registration);
    }

    /**
     * Delete registration by Registration ID (admin or owner).
     */
    @Transactional
    public void deleteRegistration(UUID registrationId) {
        Registration registration = findRegistrationById(registrationId);
        User currentUser = securityService.getCurrentAuthenticatedUser();

        securityService.validateOwnerOrAdmin(registration.getUser(), currentUser);

        registrationRepository.delete(registration);
    }


    // --- EVENT MANAGER ACTIONS ---

    /**
     * Event Manager approves a volunteer.
     */
    public RegistrationDto approveRegistration(UUID registrationId) {
        return updateRegistrationStatus(registrationId, RegistrationStatus.APPROVED);
    }

    /**
     * Event Manager rejects a volunteer.
     */
    public RegistrationDto rejectRegistration(UUID registrationId) {
        return updateRegistrationStatus(registrationId, RegistrationStatus.REJECTED);
    }

    /**
     * Event Manager marks volunteer work as completed.
     */
    public RegistrationDto completeRegistration(UUID registrationId) {
        // Logic: Can only complete if it was previously APPROVED
        Registration registration = findRegistrationById(registrationId);
        validateEventManagerAccess(registration);

        if (registration.getStatus() != RegistrationStatus.APPROVED) {
            throw new IllegalStateException("Only approved registrations can be marked as completed.");
        }

        registration.setStatus(RegistrationStatus.COMPLETED);
        return toDto(registrationRepository.save(registration));
    }



    // --- HELPERS ---

    private Registration findRegistrationById(UUID id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + id));
    }

    /**
     * Generic status updater for Event Managers
     */
    private RegistrationDto updateRegistrationStatus(UUID registrationId, RegistrationStatus newStatus) {
        Registration registration = findRegistrationById(registrationId);

        validateEventManagerAccess(registration);

        registration.setStatus(newStatus);
        return toDto(registrationRepository.save(registration));
    }

    private void validateEventManagerAccess(Registration registration) {
        User currentUser = securityService.getCurrentAuthenticatedUser();
        User eventOwner = registration.getEvent().getOwner();

        // Check if current user is the Event Owner or Admin
        securityService.validateOwnerOrAdmin(eventOwner, currentUser);
    }
}