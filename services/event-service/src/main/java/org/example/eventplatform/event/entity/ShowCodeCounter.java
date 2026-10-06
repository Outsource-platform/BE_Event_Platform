package org.example.eventplatform.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Số thứ tự mã show theo tiền tố (mã đoàn + năm tháng), khoá khi cấp số mới. */
@Entity
@Table(name = "show_code_counters")
@Getter
@Setter
@NoArgsConstructor
public class ShowCodeCounter {

    @Id
    @Column(length = 32)
    private String prefix;

    @Column(name = "last_number", nullable = false)
    private int lastNumber;
}
