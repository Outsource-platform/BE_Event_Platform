package org.example.eventplatform.catalog.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Nén ảnh sang WebP và video sang H.264 MP4 bằng ffmpeg trong container. Không có ffmpeg (chạy local trên máy dev)
 * thì mọi hàm trả false và nơi gọi giữ tệp gốc, nên tải lên vẫn dùng được.
 */
@Component
@Slf4j
public class MediaTranscoder {

    private static final long IMAGE_TIMEOUT_SECONDS = 60;
    private static final long VIDEO_TIMEOUT_SECONDS = 15 * 60;

    private volatile Boolean available;

    public boolean isAvailable() {
        if (available == null) {
            available = run(List.of("ffmpeg", "-version"), 10);
            if (!available) {
                log.warn("Không có ffmpeg: ảnh và video được lưu nguyên bản, không nén");
            }
        }
        return available;
    }

    /** Ảnh -> WebP, cạnh dài tối đa 1600px, chất lượng 80: nhẹ hơn nhiều mà vẫn nét trên điện thoại. */
    public boolean toWebp(Path in, Path out) {
        return isAvailable() && run(List.of("ffmpeg", "-y", "-loglevel", "error", "-i", in.toString(),
                "-vf", "scale='if(gt(iw,ih),min(1600,iw),-2)':'if(gt(iw,ih),-2,min(1600,ih))'",
                "-c:v", "libwebp", "-quality", "80", "-frames:v", "1", out.toString()), IMAGE_TIMEOUT_SECONDS);
    }

    /**
     * Video -> MP4 H.264 + AAC, cạnh dài tối đa 1280px, CRF 25, faststart để phát ngay khi tải. Chạy một luồng, ưu tiên thấp,
     * để không chiếm hết CPU và bộ nhớ của server dùng chung.
     */
    public boolean toMp4(Path in, Path out) {
        return isAvailable() && run(List.of("nice", "-n", "10", "ffmpeg", "-y", "-loglevel", "error", "-i", in.toString(),
                "-vf", "scale='if(gt(iw,ih),min(1280,iw),-2)':'if(gt(iw,ih),-2,min(1280,ih))'",
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "25", "-pix_fmt", "yuv420p", "-threads", "1",
                "-c:a", "aac", "-b:a", "96k", "-movflags", "+faststart", out.toString()), VIDEO_TIMEOUT_SECONDS);
    }

    private boolean run(List<String> command, long timeoutSeconds) {
        try {
            Process process = new ProcessBuilder(new ArrayList<>(command)).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("ffmpeg quá thời gian {}s: {}", timeoutSeconds, command.get(command.size() - 1));
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
