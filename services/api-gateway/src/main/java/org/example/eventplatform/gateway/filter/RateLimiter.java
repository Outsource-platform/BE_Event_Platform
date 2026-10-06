package org.example.eventplatform.gateway.filter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Giới hạn tốc độ theo cửa sổ cố định, lưu trong bộ nhớ của gateway (đủ khi chỉ chạy một bản gateway).
 * Mục đích: chặn dò mật khẩu và spam đăng ký ở các đường công khai, đồng thời không để BCrypt ngốn hết CPU.
 */
final class RateLimiter {

    private static final int SWEEP_THRESHOLD = 20_000;

    private record Window(long startedAt, int count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /** true nếu còn trong hạn mức; false là đã vượt, nên từ chối request này. */
    boolean allow(String key, int max, long windowMillis) {
        long now = System.currentTimeMillis();
        if (windows.size() > SWEEP_THRESHOLD) {
            windows.entrySet().removeIf(e -> now - e.getValue().startedAt() > windowMillis);
        }
        Window updated = windows.merge(key, new Window(now, 1), (old, fresh) ->
                now - old.startedAt() >= windowMillis ? fresh : new Window(old.startedAt(), old.count() + 1));
        return updated.count() <= max;
    }
}
