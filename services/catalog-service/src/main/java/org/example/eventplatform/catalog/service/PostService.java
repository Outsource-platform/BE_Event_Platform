package org.example.eventplatform.catalog.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.catalog.client.IdentityClient;
import org.example.eventplatform.catalog.dto.PageResult;
import org.example.eventplatform.catalog.dto.PostRequest;
import org.example.eventplatform.catalog.dto.PostResponse;
import org.example.eventplatform.catalog.dto.PostSummary;
import org.example.eventplatform.catalog.dto.SitemapItem;
import org.example.eventplatform.catalog.entity.Post;
import org.example.eventplatform.catalog.entity.PostStatus;
import org.example.eventplatform.catalog.repository.PostRepository;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Bài viết công khai. Đăng bài miễn phí cho mọi đơn vị. Quyền sở hữu do người gọi truyền vào:
 * {@code tenantScope} khác null là đơn vị chỉ được đụng bài của mình, null là Super Admin đụng được mọi bài.
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private static final Pattern SLUG_FORMAT = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final IdentityClient identityClient;

    // ===== Công khai =====

    @Transactional(readOnly = true)
    public PageResult<PostSummary> listPublished(int page, int size, Long tenantId) {
        PageRequest pageable = pageable(page, size, "publishedAt");
        var result = tenantId == null
                ? postRepository.findByStatus(PostStatus.PUBLISHED, pageable)
                : postRepository.findByStatusAndTenantId(PostStatus.PUBLISHED, tenantId, pageable);
        return PageResult.of(result, this::toSummary);
    }

    @Transactional(readOnly = true)
    public PostResponse getPublished(String slug) {
        Post post = postRepository.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bài viết"));
        List<PostSummary> related = postRepository
                .findTop3ByStatusAndIdNotOrderByPublishedAtDesc(PostStatus.PUBLISHED, post.getId())
                .stream().map(this::toSummary).toList();
        return toResponse(post, related);
    }

    @Transactional(readOnly = true)
    public List<SitemapItem> sitemap() {
        return postRepository.findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED).stream()
                .map(p -> new SitemapItem(p.getSlug(), p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getPublishedAt()))
                .toList();
    }

    // ===== Quản trị (đơn vị hoặc Super Admin) =====

    @Transactional(readOnly = true)
    public PageResult<PostSummary> listForTenant(Long tenantId, int page, int size) {
        return PageResult.of(postRepository.findByTenantId(tenantId, pageable(page, size, "createdAt")), this::toSummary);
    }

    @Transactional(readOnly = true)
    public PageResult<PostSummary> search(PostStatus status, Long tenantId, String q, int page, int size) {
        String keyword = q == null || q.isBlank() ? null : q.trim();
        return PageResult.of(postRepository.search(status, tenantId, keyword, pageable(page, size, "createdAt")), this::toSummary);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long id, Long tenantScope) {
        return toResponse(load(id, tenantScope), null);
    }

    /** {@code tenantId} là đơn vị đăng (null = bài của sàn). */
    @Transactional
    public PostResponse create(PostRequest request, Long tenantId, Long authorUserId) {
        Post post = new Post();
        post.setTenantId(tenantId);
        post.setAuthorUserId(authorUserId);
        fillAuthor(post);
        apply(post, request);
        post.setSlug(uniqueSlug(blankToNull(request.getSlug()) != null ? request.getSlug() : request.getTitle(), null));
        return toResponse(postRepository.save(post), null);
    }

    @Transactional
    public PostResponse update(Long id, PostRequest request, Long tenantScope) {
        Post post = load(id, tenantScope);
        apply(post, request);
        String wanted = blankToNull(request.getSlug());
        if (wanted != null && !wanted.equals(post.getSlug())) {
            post.setSlug(uniqueSlug(wanted, post.getId()));
        }
        return toResponse(postRepository.save(post), null);
    }

    @Transactional
    public PostResponse setStatus(Long id, PostStatus status, Long tenantScope) {
        Post post = load(id, tenantScope);
        changeStatus(post, status);
        return toResponse(postRepository.save(post), null);
    }

    @Transactional
    public void delete(Long id, Long tenantScope) {
        postRepository.delete(load(id, tenantScope));
    }

    // ===== Nội bộ =====

    private Post load(Long id, Long tenantScope) {
        return (tenantScope == null ? postRepository.findById(id) : postRepository.findByIdAndTenantId(id, tenantScope))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bài viết"));
    }

    private void apply(Post post, PostRequest request) {
        post.setTitle(request.getTitle().trim());
        post.setContent(sanitize(request.getContent()));
        post.setCoverImage(blankToNull(request.getCoverImage()));
        post.setSeoTitle(blankToNull(request.getSeoTitle()));
        post.setSeoDescription(blankToNull(request.getSeoDescription()));
        String excerpt = blankToNull(request.getExcerpt());
        post.setExcerpt(excerpt != null ? excerpt : autoExcerpt(post.getContent()));
        if (request.getStatus() != null) {
            changeStatus(post, request.getStatus());
        }
    }

    private void changeStatus(Post post, PostStatus status) {
        post.setStatus(status);
        if (status == PostStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
    }

    private void fillAuthor(Post post) {
        IdentityClient.TenantSummary tenant = identityClient.findTenant(post.getTenantId());
        if (tenant != null) {
            post.setAuthorName(tenant.name());
            post.setAuthorDomain(tenant.domain());
        } else if (post.getTenantId() == null) {
            post.setAuthorName("Stagio");
        }
    }

    /** Giữ thẻ định dạng thông dụng (tiêu đề, danh sách, link, ảnh, bảng), bỏ script/style/sự kiện để chống XSS. */
    private static String sanitize(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, Safelist.relaxed().addAttributes("a", "target").addEnforcedAttribute("a", "rel", "noopener"));
    }

    private static String autoExcerpt(String html) {
        if (html == null) {
            return null;
        }
        String text = Jsoup.parse(html).text().trim();
        if (text.isEmpty()) {
            return null;
        }
        return text.length() <= 160 ? text : text.substring(0, 157).trim() + "...";
    }

    private String uniqueSlug(String source, Long selfId) {
        String base = slugify(source);
        if (base.isEmpty()) {
            base = "bai-viet";
        }
        String slug = base;
        int n = 2;
        while (selfId == null ? postRepository.existsBySlug(slug) : postRepository.existsBySlugAndIdNot(slug, selfId)) {
            slug = base + "-" + n++;
        }
        if (!SLUG_FORMAT.matcher(slug).matches()) {
            throw new IllegalArgumentException("Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang");
        }
        return slug;
    }

    /** "Múa lân đón Tết" -> "mua-lan-don-tet": bỏ dấu tiếng Việt, đ -> d, ký tự lạ thành dấu gạch. */
    static String slugify(String input) {
        if (input == null) {
            return "";
        }
        String s = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return s.length() > 100 ? s.substring(0, 100).replaceAll("-+$", "") : s;
    }

    private static PageRequest pageable(int page, int size, String sortProp) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, sortProp));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PostSummary toSummary(Post p) {
        return PostSummary.builder()
                .id(p.getId()).tenantId(p.getTenantId()).authorName(p.getAuthorName()).authorDomain(p.getAuthorDomain())
                .title(p.getTitle()).slug(p.getSlug()).excerpt(p.getExcerpt()).coverImage(p.getCoverImage())
                .status(p.getStatus()).publishedAt(p.getPublishedAt())
                .build();
    }

    private PostResponse toResponse(Post p, List<PostSummary> related) {
        return PostResponse.builder()
                .id(p.getId()).tenantId(p.getTenantId()).authorName(p.getAuthorName()).authorDomain(p.getAuthorDomain())
                .title(p.getTitle()).slug(p.getSlug()).excerpt(p.getExcerpt()).content(p.getContent())
                .coverImage(p.getCoverImage()).seoTitle(p.getSeoTitle()).seoDescription(p.getSeoDescription())
                .status(p.getStatus()).publishedAt(p.getPublishedAt()).updatedAt(p.getUpdatedAt())
                .related(related)
                .build();
    }
}
