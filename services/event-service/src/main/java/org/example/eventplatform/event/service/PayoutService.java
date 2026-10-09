package org.example.eventplatform.event.service;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.client.IdentityServiceClient;
import org.example.eventplatform.event.entity.Event;
import org.example.eventplatform.event.entity.UserEvent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Chốt tiền khi show hoàn thành:
 * <ul>
 *   <li>Mỗi người: thực nhận = cát-xê - cát-xê × % quỹ đoàn của người đó (% do trưởng đoàn gán trên hồ sơ).</li>
 *   <li>Người tạo show (thành viên): hoa hồng = tổng tiền show × % hoa hồng trên hồ sơ, cộng thêm vào ví
 *       dù họ có đi diễn hay không; cát-xê đi diễn vẫn tính riêng như mọi người.</li>
 * </ul>
 * Các con số được chốt vào bản ghi tại thời điểm đó để đổi % về sau không làm thay đổi lịch sử.
 */
@Service
@RequiredArgsConstructor
public class PayoutService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final IdentityServiceClient identityServiceClient;

    /** Tính phần quỹ bị trừ và số thực nhận cho một suất diễn đã hoàn thành. Gọi lại được khi cát-xê bị sửa. */
    public void settleAssignment(UserEvent ue) {
        IdentityServiceClient.UserContact user = identityServiceClient.findUser(ue.getUserId());
        BigDecimal percent = user == null ? null : user.teamFundPercent();
        BigDecimal gross = ue.getSalary() == null ? BigDecimal.ZERO : ue.getSalary();
        BigDecimal fund = percent == null || percent.signum() <= 0
                ? BigDecimal.ZERO
                : gross.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        ue.setFundPercent(percent);
        ue.setFundAmount(fund);
        ue.setNetAmount(gross.subtract(fund));
    }

    /** Chốt hoa hồng của người tạo show theo % hiện tại trên hồ sơ của họ. Không có % thì giữ nguyên số cũ. */
    public void settleCommission(Event event) {
        if (event.getCreatedByUserId() == null || event.getTotalAmount() == null) {
            return;
        }
        IdentityServiceClient.UserContact creator = identityServiceClient.findUser(event.getCreatedByUserId());
        BigDecimal rate = creator == null ? null : creator.commissionRate();
        if (rate == null || rate.signum() <= 0) {
            return;
        }
        event.setCreatorCommissionAmount(event.getTotalAmount().multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP));
    }
}
