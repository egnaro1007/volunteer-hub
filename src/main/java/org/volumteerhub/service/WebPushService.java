package org.volumteerhub.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;

import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import org.volumteerhub.config.VapidConfig;
import org.volumteerhub.model.PushSubscription;
import org.volumteerhub.model.User;
import org.volumteerhub.repository.PushSubscriptionRepository;


import java.security.Security;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserService userService;
    private final VapidConfig vapidConfig;
    private PushService pushService = null;

    @PostConstruct
    private void init() throws Exception {
        boolean isEnable = vapidConfig.isEnabled();
        if (!isEnable) {
            log.warn("Web Push Service is disabled");
            return;
        }
        Security.addProvider(new BouncyCastleProvider());
        pushService = new PushService(
                vapidConfig.getPublicKey(),
                vapidConfig.getPrivateKey(),
                vapidConfig.getSubject()
        );
    }

    public void subscribe(Subscription subscription) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        pushSubscriptionRepository.findByEndpoint(subscription.endpoint)
                .ifPresentOrElse(
                        existing -> {},
                        () -> {
                            PushSubscription newSub = PushSubscription.builder()
                                    .endpoint(subscription.endpoint)
                                    .p256dh(subscription.keys.p256dh)
                                    .auth(subscription.keys.auth)
                                    .user(currentUser)
                                    .build();
                            pushSubscriptionRepository.save(newSub);
                            log.debug("Successfully saved new subscription to database");
                            this.sendNotificationToCurrentUser("Subscribed!", "Device registered successfully.");
                        }
                );

//        this.sendToBrowser(subscription, "{\"title\": \"Subscribed!\", \"body\": \"Device registered successfully.\"}");
    }

    public void sendTestNotification() {
        log.debug("Send Test Push Notification");
        this.sendNotificationToCurrentUser("Push Notification Test", "Push notification works normally!!!","127.0.0.1");
    }

    public void sendNotificationToCurrentUser (String title, String body, String url) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        String payload = String.format(
                "{\"title\": \"%s\", \"body\": \"%s\", \"url\": \"%s\"}",
                title, body, url
        );

        this.sendNotificationToUser(currentUser, payload);
    }
    public void sendNotificationToCurrentUser (String title, String body) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        String payload = String.format(
                "{\"title\": \"%s\", \"body\": \"%s\"}",
                title, body
        );

        this.sendNotificationToUser(currentUser, payload);
    }


    public void sendNotificationToCurrentUser (String messageJson){
        User currentUser = userService.getCurrentAuthenticatedUser();
        this.sendNotificationToUser(currentUser, messageJson);
    }

    public void sendNotificationToUser(User user, String title, String body, String url) {
        String payload = String.format(
                "{\"title\": \"%s\", \"body\": \"%s\", \"url\": \"%s\"}",
                title, body, url
        );

        this.sendNotificationToUser(user, payload);
    }

    public void sendNotificationToUser(User user, String payload) {
        List<PushSubscription> subs = user.getPushSubscriptions();

        if (subs == null || subs.isEmpty()) {
            log.error("No subscriptions found for user: {}", user.getUsername());
            return;
        }

        for (PushSubscription subEntity : subs) {
            Subscription browserSub = new Subscription(
                    subEntity.getEndpoint(),
                    new Subscription.Keys(subEntity.getP256dh(), subEntity.getAuth())
            );
            this.sendToBrowser(browserSub, payload);
        }
    }

    private void sendToBrowser(Subscription subscription, String payload) {
        if (pushService == null) {
            return;
        }
        try {
            Notification notification = new Notification(subscription, payload);
            HttpResponse response = pushService.send(notification);

            int statusCode = response.getStatusLine().getStatusCode();
            switch (statusCode) {
                case 201 -> log.debug("Push sent successfully (201)");
                case 403 -> log.error("403 Forbidden: Check VAPID keys/Subject");
                case 410 -> {
                    log.warn("410 Gone: User unsubscribed. Removing from DB.");
                    pushSubscriptionRepository.findByEndpoint(subscription.endpoint)
                            .ifPresent(pushSubscriptionRepository::delete);
                }
                default -> log.error("Unexpected Push Service Response: {}", statusCode);
            }
        } catch (Exception e) {
            log.error("Error sending push: {}", e.getMessage());
        }
    }
}
