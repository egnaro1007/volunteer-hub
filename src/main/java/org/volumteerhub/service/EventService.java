package org.volumteerhub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.volumteerhub.common.EventStatus;
import org.volumteerhub.dto.EventDto;
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

    // Convert Entity -> DTO
    private EventDto toDto(Event event) {
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

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

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
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return toDto(event);
    }

    // LIST + FILTER + PAGE
    public Page<EventDto> list(EventStatus status, UUID ownerId, String search, Pageable pageable) {

        Specification<Event> spec = Specification.allOf(
                EventSpecifications.hasStatus(status),
                EventSpecifications.hasOwnerId(ownerId),
                EventSpecifications.nameContains(search)
        );

        return eventRepository.findAll(spec, pageable).map(this::toDto);
    }

    // UPDATE
    public EventDto update(UUID id, EventDto dto) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (dto.getName() != null) event.setName(dto.getName());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getDateDeadline() != null) event.setDateDeadline(dto.getDateDeadline());
        if (dto.getStartDate() != null) event.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) event.setEndDate(dto.getEndDate());
        if (dto.getStatus() != null) event.setStatus(dto.getStatus());

        return toDto(eventRepository.save(event));
    }

    // DELETE
    public void delete(UUID id) {
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Event not found");
        }
        eventRepository.deleteById(id);
    }
}
