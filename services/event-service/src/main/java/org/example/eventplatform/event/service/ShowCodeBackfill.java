package org.example.eventplatform.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShowCodeBackfill {

    private final ShowCodeService showCodeService;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            showCodeService.backfillMissing();
        } catch (Exception ex) {
            log.error("Không cấp mã cho show cũ", ex);
        }
    }
}
