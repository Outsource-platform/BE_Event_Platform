package org.example.eventplatform.event.repository;

import org.example.eventplatform.event.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findFirstByTenantIdAndCustomerUserIdAndEventId(Long tenantId, Long customerUserId, Long eventId);

    Optional<ChatConversation> findFirstByTenantIdAndCustomerUserIdAndEventIdIsNull(Long tenantId, Long customerUserId);

    List<ChatConversation> findByCustomerUserIdOrderByLastMessageAtDesc(Long customerUserId);

    /**
     * Hộp thư của một người: cuộc mình là bên ngoài (khách, hoặc thành viên nhắn trưởng đoàn), cuộc mình là người nhận,
     * và nếu là trưởng đoàn thì cả các cuộc chưa gán người nhận của đoàn.
     */
    @org.springframework.data.jpa.repository.Query("SELECT c FROM ChatConversation c WHERE c.customerUserId = :me OR c.troupeUserId = :me " +
            "OR (:admin = true AND c.troupeUserId IS NULL AND c.tenantId = :tenantId) ORDER BY c.lastMessageAt DESC")
    List<ChatConversation> findInbox(@org.springframework.data.repository.query.Param("me") Long me,
                                     @org.springframework.data.repository.query.Param("admin") boolean admin,
                                     @org.springframework.data.repository.query.Param("tenantId") Long tenantId);
}
