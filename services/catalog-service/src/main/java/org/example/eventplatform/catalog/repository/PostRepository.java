package org.example.eventplatform.catalog.repository;

import org.example.eventplatform.catalog.entity.Post;
import org.example.eventplatform.catalog.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);

    Optional<Post> findByIdAndTenantId(Long id, Long tenantId);

    /** Danh sách công khai: bài mới đăng hoặc mới đẩy tin nằm trên cùng. Pageable truyền vào không được kèm sort. */
    @Query("SELECT p FROM Post p WHERE p.status = :status ORDER BY COALESCE(p.pushedAt, p.publishedAt) DESC, p.id DESC")
    Page<Post> findPublished(@Param("status") PostStatus status, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status = :status AND p.tenantId = :tenantId " +
            "ORDER BY COALESCE(p.pushedAt, p.publishedAt) DESC, p.id DESC")
    Page<Post> findPublishedByTenant(@Param("status") PostStatus status, @Param("tenantId") Long tenantId, Pageable pageable);

    long countByTenantIdAndPushedAtAfter(Long tenantId, LocalDateTime since);

    /** Lần đẩy tin sớm nhất còn nằm trong cửa sổ hạn mức; dùng để báo khi nào được đẩy tiếp. */
    @Query("SELECT MIN(p.pushedAt) FROM Post p WHERE p.tenantId = :tenantId AND p.pushedAt > :since")
    LocalDateTime findEarliestPushSince(@Param("tenantId") Long tenantId, @Param("since") LocalDateTime since);

    Page<Post> findByTenantId(Long tenantId, Pageable pageable);

    List<Post> findByStatusOrderByPublishedAtDesc(PostStatus status);

    List<Post> findTop3ByStatusAndIdNotOrderByPublishedAtDesc(PostStatus status, Long id);

    /** Lọc cho màn quản trị của Super Admin; mọi tham số đều tuỳ chọn. */
    @Query("SELECT p FROM Post p WHERE (:status IS NULL OR p.status = :status) " +
            "AND (:tenantId IS NULL OR p.tenantId = :tenantId) " +
            "AND (:q IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Post> search(@Param("status") PostStatus status, @Param("tenantId") Long tenantId,
                      @Param("q") String q, Pageable pageable);
}
