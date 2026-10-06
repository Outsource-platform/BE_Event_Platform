package org.example.eventplatform.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.event.client.IdentityServiceClient;
import org.example.eventplatform.event.entity.Event;
import org.example.eventplatform.event.repository.EventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowCodeService {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyMM");

    private final JdbcTemplate jdbcTemplate;
    private final EventRepository eventRepository;
    private final IdentityServiceClient identityServiceClient;

    /** Gán mã dạng {mã đoàn}-{yyMM}-{số}, ví dụ abc-2610-0007. Không đổi mã đã có. */
    public void assign(Event event) {
        if (event.getShowCode() != null || event.getTenantId() == null || event.getEventDate() == null) {
            return;
        }
        String prefix = prefix(event.getTenantId(), event.getEventDate());
        int number = nextNumber(event.getTenantId(), prefix);
        event.setShowCode(prefix + String.format("%04d", number));
    }

    @Transactional
    public void backfillMissing() {
        List<Event> missing = eventRepository.findByShowCodeIsNullAndTenantIdIsNotNullAndEventDateIsNotNullOrderByIdAsc();
        if (missing.isEmpty()) {
            return;
        }
        for (Event event : missing) {
            assign(event);
        }
        eventRepository.saveAll(missing);
        log.info("Đã cấp mã cho {} show chưa có mã", missing.size());
    }

    private int nextNumber(Long tenantId, String prefix) {
        jdbcTemplate.update("INSERT IGNORE INTO show_code_counters (prefix, last_number) VALUES (?, 0)", prefix);
        Integer current = jdbcTemplate.queryForObject(
                "SELECT last_number FROM show_code_counters WHERE prefix = ? FOR UPDATE",
                Integer.class,
                prefix);
        int base = current == null ? 0 : current;
        if (base == 0) {
            base = maxExistingSuffix(tenantId, prefix);
        }
        int next = base + 1;
        jdbcTemplate.update("UPDATE show_code_counters SET last_number = ? WHERE prefix = ?", next, prefix);
        return next;
    }

    private int maxExistingSuffix(Long tenantId, String prefix) {
        int max = 0;
        for (String code : eventRepository.findShowCodesByPrefix(tenantId, prefix)) {
            if (code == null || code.length() <= prefix.length()) {
                continue;
            }
            try {
                max = Math.max(max, Integer.parseInt(code.substring(prefix.length())));
            } catch (NumberFormatException ignored) {
                // Mã cũ không đúng đuôi số thì bỏ qua.
            }
        }
        return max;
    }

    private String prefix(Long tenantId, LocalDate eventDate) {
        return domain(tenantId) + "-" + eventDate.format(YEAR_MONTH) + "-";
    }

    private String domain(Long tenantId) {
        IdentityServiceClient.TenantSummary tenant = identityServiceClient.findTenant(tenantId);
        String raw = tenant == null || tenant.domain() == null ? "" : tenant.domain().toLowerCase().replaceAll("[^a-z0-9]", "");
        if (raw.isEmpty()) {
            return "t" + tenantId;
        }
        return raw.length() > 20 ? raw.substring(0, 20) : raw;
    }
}
