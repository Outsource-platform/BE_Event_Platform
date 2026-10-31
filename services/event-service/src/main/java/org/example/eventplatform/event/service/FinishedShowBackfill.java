package org.example.eventplatform.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Chốt các show mọi người đã check-out nhưng còn kẹt ở trạng thái đang diễn. */
@Component
@RequiredArgsConstructor
public class FinishedShowBackfill implements ApplicationRunner {

    private final EventService eventService;

    @Override
    public void run(ApplicationArguments args) {
        eventService.completeFinishedShows();
    }
}
