package org.example.eventplatform.identity.dto.user;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Các ô để trưởng đoàn cân nhắc và gán tỉ lệ cho thành viên. Để trống một ô là xoá giá trị đó. */
@Getter
@Setter
public class UpdateFinanceProfileRequest {

    private LocalDate joinedDate;

    @Min(value = 0, message = "Số năm không được âm")
    @Max(value = 80, message = "Số năm tối đa 80")
    private Integer seniority;

    @DecimalMin(value = "0.0", message = "% quỹ đoàn từ 0 đến 100")
    @DecimalMax(value = "100.0", message = "% quỹ đoàn từ 0 đến 100")
    private BigDecimal teamFundPercent;

    @DecimalMin(value = "0.0", message = "% hoa hồng từ 0 đến 100")
    @DecimalMax(value = "100.0", message = "% hoa hồng từ 0 đến 100")
    private BigDecimal commissionRate;
}
