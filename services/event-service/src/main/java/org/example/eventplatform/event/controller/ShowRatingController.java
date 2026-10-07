package org.example.eventplatform.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.dto.RatingDtos;
import org.example.eventplatform.event.service.ShowRatingService;
import org.example.eventplatform.shared.security.JwtPrincipal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class ShowRatingController {

    private final ShowRatingService ratingService;

    /** Công khai: ai cũng xem được đánh giá của show đã đăng. */
    @GetMapping("/api/public/shows/{id}/ratings")
    public ResponseEntity<RatingDtos.Page> list(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(15)).cachePublic())
                .body(ratingService.list(id, page, size));
    }

    @PostMapping("/api/customer/shows/{id}/rating")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RatingDtos.Summary> rate(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id,
                                                   @Valid @RequestBody RatingDtos.Request request) {
        return ResponseEntity.ok(ratingService.rate(id, principal.userId(), request));
    }
}
