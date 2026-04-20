package com.example.ytclone.infrastructure.web;

import com.example.ytclone.application.SubscriptionService;
import com.example.ytclone.infrastructure.web.dto.SubscriptionStatusDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionRestController {
    private final SubscriptionService subscriptionService;

    public SubscriptionRestController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/{creatorUsername}/toggle")
    public ResponseEntity<SubscriptionStatusDTO> toggleSubscription(@PathVariable String creatorUsername, @AuthenticationPrincipal Jwt jwt) {
        boolean subscribed = subscriptionService.toggleSubscription(jwt.getSubject(), creatorUsername);
        return ResponseEntity.ok(new SubscriptionStatusDTO(subscribed));
    }

    @GetMapping("/{creatorUsername}/status")
    public ResponseEntity<SubscriptionStatusDTO> getSubscriptionStatus(@PathVariable String creatorUsername, @AuthenticationPrincipal Jwt jwt) {
        boolean subscribed = subscriptionService.isSubscribed(jwt.getSubject(), creatorUsername);
        return ResponseEntity.ok(new SubscriptionStatusDTO(subscribed));
    }

    @GetMapping("/{creatorUsername}/count")
    public ResponseEntity<Long> getSubscriptionStatus(@PathVariable String creatorUsername) {
        return ResponseEntity.ok(subscriptionService.countSubscribers(creatorUsername));
    }
}
