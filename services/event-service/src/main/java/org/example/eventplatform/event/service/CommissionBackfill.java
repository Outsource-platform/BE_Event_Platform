package org.example.eventplatform.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.event.repository.EventRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Show cũ có hoa hồng theo người tạo: coi người tạo là người thầu để ví vẫn cộng đúng (không đổi số tiền).
 * Chạy lại nhiều lần vẫn an toàn vì chỉ chạm các dòng chưa có người thầu.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommissionBackfill implements ApplicationRunner {

    private final EventRepository eventRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int n = eventRepository.backfillContractFromCreator();
        if (n > 0) {
            log.info("Đã gán người thầu cho {} show cũ có hoa hồng theo người tạo", n);
        }
    }
}
