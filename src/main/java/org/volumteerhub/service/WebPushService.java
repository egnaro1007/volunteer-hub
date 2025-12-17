package org.volumteerhub.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;

import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.volumteerhub.model.PushSubscription;
import org.volumteerhub.model.User;
import org.volumteerhub.repository.PushSubscriptionRepository;


import java.security.Security;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushService {

    @Value("${vapid.public.key}")
    private String publicKey;

    @Value("${vapid.private.key}")
    private String privateKey;

    @Value("${vapid.subject}")
    private String subject;

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserService userService;
    private PushService pushService;

    @PostConstruct
    private void init() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        pushService = new PushService(publicKey, privateKey, subject);
    }

    public void subscribe(Subscription subscription) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        pushSubscriptionRepository.findByEndpoint(subscription.endpoint)
                .ifPresentOrElse(
                        existing -> log.info("Subscription already exists for this endpoint"),
                        () -> {
                            PushSubscription newSub = PushSubscription.builder()
                                    .endpoint(subscription.endpoint)
                                    .p256dh(subscription.keys.p256dh)
                                    .auth(subscription.keys.auth)
                                    .user(currentUser)
                                    .build();
                            pushSubscriptionRepository.save(newSub);
                            log.info("Successfully saved new subscription to database");
                        }
                );

        this.sendToBrowser(subscription, "{\"title\": \"Subscribed!\", \"body\": \"Device registered successfully.\"}");
    }

    public void sendTestNotification() {
        log.info("Send Test Push Notification");
        this.sendNotificationToCurrentUser("{\"title\": \"Push Notification Test\", \"body\": \"Push notification works normally!!!\"}");
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

        sendNotificationToUser(user, payload);
    }

    public void sendNotificationToUser(User user, String payload) {
        List<PushSubscription> subs = user.getPushSubscriptions();

        if (subs == null || subs.isEmpty()) {
            log.warn("No subscriptions found for user: {}", user.getUsername());
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
        try {
            Notification notification = new Notification(subscription, payload);
            HttpResponse response = pushService.send(notification);

            int statusCode = response.getStatusLine().getStatusCode();
            switch (statusCode) {
                case 201 -> log.info("Push sent successfully (201)");
                case 403 -> log.error("403 Forbidden: Check your VAPID keys/Subject");
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
