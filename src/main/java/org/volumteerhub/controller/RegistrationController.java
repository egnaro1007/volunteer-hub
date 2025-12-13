package org.volumteerhub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.volumteerhub.dto.RegistrationDto;
import org.volumteerhub.service.RegistrationService;

import java.util.UUID;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    // --- Common Endpoints ---

    @GetMapping("/{id}")
    public ResponseEntity<RegistrationDto> getRegistration(@PathVariable UUID id) {
        RegistrationDto registration = registrationService.getRegistration(id);
        return ResponseEntity.ok(registration);
    }

    // --- Volunteer Endpoints ---

    @PostMapping("/{eventId}/join")
    public ResponseEntity<RegistrationDto> joinEvent(@PathVariable UUID eventId) {
        RegistrationDto registration = registrationService.joinEvent(eventId);
        return new ResponseEntity<>(registration, HttpStatus.CREATED);
    }

    @PostMapping("/{eventId}/cancel-join")
    public ResponseEntity<Void> cancelJoin(@PathVariable UUID eventId) {
        registrationService.cancelJoinByEventId(eventId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegistration(@PathVariable UUID id) {
        registrationService.deleteRegistration(id);
        return ResponseEntity.noContent().build();
    }

    // --- Event Manager Endpoints ---

    @PostMapping("/{id}/approve")
    public ResponseEntity<RegistrationDto> approveRegistration(@PathVariable UUID id) {
        RegistrationDto registration = registrationService.approveRegistration(id);
        return ResponseEntity.ok(registration);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<RegistrationDto> rejectRegistration(@PathVariable UUID id) {
        RegistrationDto registration = registrationService.rejectRegistration(id);
        return ResponseEntity.ok(registration);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<RegistrationDto> completeRegistration(@PathVariable UUID id) {
        RegistrationDto registration = registrationService.completeRegistration(id);
        return ResponseEntity.ok(registration);
    }
}
