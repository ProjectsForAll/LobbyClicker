package gg.drak.lobbyclicker.boosters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BoosterManager {
    private static final ConcurrentHashMap<String, List<ActiveBooster>> ACTIVE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Map<BoosterType, Long>> COOLDOWNS = new ConcurrentHashMap<>();

    public static void addBooster(String uuid, BoosterType type) {
        ACTIVE.computeIfAbsent(uuid, k -> new ArrayList<>()).add(new ActiveBooster(type));
        COOLDOWNS.computeIfAbsent(uuid, k -> new EnumMap<>(BoosterType.class))
                .put(type, System.currentTimeMillis());
    }

    public static List<ActiveBooster> getActiveBoosters(String uuid) {
        List<ActiveBooster> boosters = ACTIVE.get(uuid);
        if (boosters == null) return new ArrayList<>();
        cleanExpired(uuid);
        List<ActiveBooster> result = ACTIVE.get(uuid);
        return result != null ? new ArrayList<>(result) : new ArrayList<>();
    }

    public static void cleanExpired(String uuid) {
        List<ActiveBooster> boosters = ACTIVE.get(uuid);
        if (boosters == null) return;
        boosters.removeIf(ActiveBooster::isExpired);
        if (boosters.isEmpty()) ACTIVE.remove(uuid);
    }

    public static boolean isOnCooldown(String uuid, BoosterType type) {
        return getCooldownRemaining(uuid, type) > 0;
    }

    public static long getCooldownRemaining(String uuid, BoosterType type) {
        Map<BoosterType, Long> cooldowns = COOLDOWNS.get(uuid);
        if (cooldowns == null) return 0;
        Long purchaseTime = cooldowns.get(type);
        if (purchaseTime == null) return 0;
        long elapsed = System.currentTimeMillis() - purchaseTime;
        long remaining = (type.getCooldownSeconds() * 1000L) - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Get combined multiplier for a specific effect.
     * Multiplies together all active (not paused, not expired) boosters of that effect type.
     */
    public static BigDecimal getMultiplier(String uuid, BoosterEffect effect) {
        BigDecimal mult = BigDecimal.ONE;
        for (ActiveBooster b : getActiveBoosters(uuid)) {
            if (b.isActive() && b.getType().getEffect() == effect) {
                mult = mult.multiply(b.getType().getEffectValue());
            }
        }
        return mult;
    }

    public static void clearAll(String uuid) {
        ACTIVE.remove(uuid);
        COOLDOWNS.remove(uuid);
    }
}
