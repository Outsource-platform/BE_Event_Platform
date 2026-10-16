package org.example.eventplatform.event.repository;

import org.example.eventplatform.event.entity.ContentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {

    Page<ContentReport> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<ContentReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<ContentReport> findByTargetTypeAndTargetIdAndStatus(String targetType, Long targetId, String status);

    Optional<ContentReport> findFirstByReporterUserIdAndTargetTypeAndTargetIdAndStatus(Long reporterUserId, String targetType,
                                                                                      Long targetId, String status);
}
