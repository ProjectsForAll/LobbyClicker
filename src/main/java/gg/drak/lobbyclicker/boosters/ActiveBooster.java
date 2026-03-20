package gg.drak.lobbyclicker.boosters;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActiveBooster {
    private final BoosterType type;
    private final long startTime;
    private boolean paused;
    private long pausedAt;
    private long totalPausedMs;

    public ActiveBooster(BoosterType type) {
        this.type = type;
        this.startTime = System.currentTimeMillis();
        this.paused = false;
        this.pausedAt = 0;
        this.totalPausedMs = 0;
    }

    public long getRemainingMs() {
        long effectiveElapsed;
        if (paused) {
            effectiveElapsed = pausedAt - startTime - totalPausedMs;
        } else {
            effectiveElapsed = System.currentTimeMillis() - startTime - totalPausedMs;
        }
        return (type.getDurationSeconds() * 1000L) - effectiveElapsed;
    }

    public boolean isExpired() {
        return getRemainingMs() <= 0;
    }

    public boolean isActive() {
        return !paused && !isExpired();
    }

    public void pause() {
        if (!paused) {
            paused = true;
            pausedAt = System.currentTimeMillis();
        }
    }

    public void unpause() {
        if (paused) {
            totalPausedMs += System.currentTimeMillis() - pausedAt;
            paused = false;
        }
    }
}
