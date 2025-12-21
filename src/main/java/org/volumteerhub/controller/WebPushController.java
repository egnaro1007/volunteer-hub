package org.volumteerhub.controller;

import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Subscription;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.volumteerhub.service.WebPushService;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/webpush")
@RequiredArgsConstructor
public class WebPushController {
    private final WebPushService webPushService;

    @GetMapping("/public-key")
    public Map<String, String> getPublicKey() {
        return webPushService.getPublicKey();
    }

    @PostMapping("/subscribe")
    public void subscribe(@RequestBody Subscription subscription) {
        webPushService.subscribe(subscription);
    }

    @PostMapping("/verify-subscription")
    public ResponseEntity<Map<String, Boolean>> verifySubscription(@RequestBody Subscription subscription) {
        boolean exists = webPushService.verifySubscription(subscription);
        return ResponseEntity.ok(Collections.singletonMap("exists", exists));
    }

    @GetMapping("/test")
    public void sendTest() {
        webPushService.sendTestNotification();
    }
}