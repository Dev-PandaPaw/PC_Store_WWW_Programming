package iuh.fit.nhom16.pcstore.ai;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/** Shared by search and chat, counts actual provider calls rather than keystrokes/cache hits. */
@Component
public class AiLimiter {
    private final AiSettings settings;
    private final Clock clock;
    private final Map<String, Bucket> buckets = new HashMap<>();
    private long globalDay = -1, blockedUntil;
    private int globalCount, active;
    private static class Bucket { long minute, day; int minuteCount, dayCount; }

    @Autowired
    public AiLimiter(AiSettings settings) { this(settings, Clock.systemUTC()); }
    AiLimiter(AiSettings settings, Clock clock) { this.settings = settings; this.clock = clock; }

    public synchronized AutoCloseable acquire(String client) {
        long now = clock.millis(), minute = now / 60000, day = now / 86400000;
        if (now < blockedUntil) throw limited("AI_QUOTA_EXCEEDED", (int) Math.ceil((blockedUntil-now)/1000.0));
        if (globalDay != day) { globalDay = day; globalCount = 0; }
        buckets.entrySet().removeIf(entry -> entry.getValue().day != day);
        if (!buckets.containsKey(client) && buckets.size() >= 10000) throw limited("AI_BUSY", 60);
        Bucket bucket = buckets.computeIfAbsent(client, key -> new Bucket());
        if (bucket.day != day) { bucket.day = day; bucket.dayCount = 0; }
        if (bucket.minute != minute) { bucket.minute = minute; bucket.minuteCount = 0; }
        boolean daily = bucket.dayCount >= settings.perDay() || globalCount >= settings.globalPerDay();
        if (daily || bucket.minuteCount >= settings.perMinute())
            throw limited("AI_RATE_LIMITED", (int) Math.ceil(((daily ? (day+1)*86400000 : (minute+1)*60000)-now)/1000.0));
        if (active >= 2) throw limited("AI_BUSY", 10);
        bucket.minuteCount++; bucket.dayCount++; globalCount++; active++;
        return this::release;
    }
    private synchronized void release() { active--; }
    public synchronized void cooldown(int seconds) { blockedUntil = Math.max(blockedUntil, clock.millis()+seconds*1000L); }
    private AiFailure limited(String code, int delay) {
        return new AiFailure(429, code, "AI đang đạt giới hạn lượt sử dụng. Bạn vui lòng chờ rồi thử lại.", Math.max(1, delay));
    }
}
