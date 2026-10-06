package org.example.eventplatform.shared.cache;

import java.util.function.Supplier;

/**
 * Bộ nhớ đệm một giá trị có hạn dùng, không cần thư viện ngoài. Nhiều luồng cùng hết hạn thì chỉ một luồng đi tải
 * (các luồng còn lại chờ rồi dùng kết quả mới), tránh cảnh vài trăm request cùng dội xuống service/database.
 * Tải lỗi mà còn giá trị cũ thì trả giá trị cũ thay vì làm hỏng request; chưa có giá trị cũ thì ném lỗi ra,
 * nên lỗi tạm thời không bao giờ bị cache lại.
 */
public final class TtlCache<T> {

    private final long ttlNanos;
    private volatile Entry<T> entry;

    public TtlCache(java.time.Duration ttl) {
        this.ttlNanos = ttl.toNanos();
    }

    public T get(Supplier<T> loader) {
        Entry<T> current = entry;
        if (current != null && fresh(current)) {
            return current.value;
        }
        synchronized (this) {
            current = entry;
            if (current != null && fresh(current)) {
                return current.value;
            }
            try {
                T value = loader.get();
                entry = new Entry<>(value, System.nanoTime());
                return value;
            } catch (RuntimeException ex) {
                if (current != null) {
                    return current.value;
                }
                throw ex;
            }
        }
    }

    /** Xoá cache, lần gọi sau tải lại ngay (dùng khi dữ liệu vừa đổi và cần thấy kết quả tức thì). */
    public void invalidate() {
        entry = null;
    }

    private boolean fresh(Entry<T> e) {
        return System.nanoTime() - e.loadedAt < ttlNanos;
    }

    private record Entry<T>(T value, long loadedAt) {
    }
}
