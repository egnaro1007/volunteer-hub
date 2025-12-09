package org.volumteerhub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.volumteerhub.dto.EventDto;
import org.volumteerhub.service.EventService;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<EventDto> approve(@PathVariable UUID id) {
        EventDto event = eventService.approve(id);
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<EventDto> reject(@PathVariable UUID id) {
        EventDto event = eventService.reject(id);
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }
}
