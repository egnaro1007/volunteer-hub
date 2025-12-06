package org.volumteerhub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.volumteerhub.common.EventStatus;
import org.volumteerhub.common.validation.OnCreate;
import org.volumteerhub.common.validation.OnUpdate;
import org.volumteerhub.dto.EventDto;
import org.volumteerhub.service.EventService;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // CREATE
    @PostMapping
    public ResponseEntity<EventDto> create(
            @Validated(OnCreate.class) @RequestBody EventDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(dto));
    }

    // GET BY ID
    @GetMapping("/{id}")
    public EventDto get(@PathVariable UUID id) {
        return eventService.get(id);
    }

    // LIST (page + filter)
    @GetMapping
    public Page<EventDto> list(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        return eventService.list(status, ownerId, search, pageable);
    }

    // UPDATE
    @PatchMapping("/{id}")
    public EventDto update(
            @PathVariable UUID id,
            @Validated(OnUpdate.class) @RequestBody EventDto dto) {
        return eventService.update(id, dto);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        eventService.delete(id);
    }
}
