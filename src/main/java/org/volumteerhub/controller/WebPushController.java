package org.volumteerhub.controller;

import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Subscription;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.volumteerhub.service.WebPushService;

@RestController
@RequestMapping("/api/webpush")
@RequiredArgsConstructor
public class WebPushController {
    private final WebPushService webPushService;

    @PostMapping("/subscribe")
    public void subscribe(@RequestBody Subscription subscription) {
        webPushService.subscribe(subscription);
    }

    @PostMapping("/test")
    public void sendTest() {
        webPushService.sendTestNotification();
    }
}
