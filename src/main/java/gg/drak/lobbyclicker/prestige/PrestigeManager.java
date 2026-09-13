package gg.drak.lobbyclicker.prestige;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.math.CookieMath;
import gg.drak.lobbyclicker.upgrades.UpgradeType;

import java.math.BigDecimal;

/**
 * Hybrid Cookie Clicker prestige: heavenly-chip math, displayed as Aura.
 *
 * Aura is CUMULATIVE on all-time baked cookies, exactly like heavenly chips:
 *   totalAuraEarnable = floor(cbrt(lifetimeCookiesEarned / 1e12))
 *   auraGain          = totalAuraEarnable - auraAlreadyEarned
 *
 * This is what makes ascension cost something. Basing the gain on the
 * per-run counter instead would let a player bake 1T, ascend, bake 1T,
 * ascend... forever at a flat price - the Nth aura point must cost
 * (N^3 - (N-1)^3) * 1e12 all-time cookies, not a flat 1e12 each time.
 *
 * CPS * (1 + 0.01 * prestigeLevel) * (1 + 0.01 * aura)
 */
public class PrestigeManager {
    public static final BigDecimal TRILLION = new BigDecimal("1000000000000");
    private static final BigDecimal PERCENT = new BigDecimal("0.01");

    /**
     * Aura the player would gain by ascending right now: the total aura their
     * all-time baked cookies entitle them to, minus the aura they already hold.
     */
    public static BigDecimal calculateAuraGain(PlayerData data) {
        if (data == null) return BigDecimal.ZERO;
        BigDecimal earnable = totalAuraEarnable(data.getLifetimeCookiesEarned());
        BigDecimal gain = earnable.subtract(nonNull(data.getAura()));
        return gain.signum() > 0 ? gain : BigDecimal.ZERO;
    }

    /** Total aura an all-time bake count is worth: floor(cbrt(lifetime / 1e12)). */
    public static BigDecimal totalAuraEarnable(BigDecimal lifetimeCookiesEarned) {
        if (lifetimeCookiesEarned == null || lifetimeCookiesEarned.signum() <= 0) return BigDecimal.ZERO;
        return CookieMath.cbrtFloor(lifetimeCookiesEarned.divide(TRILLION, CookieMath.MC));
    }

    /** All-time cookies required to hold {@code aura} points - the inverse of the cbrt curve. */
    public static BigDecimal lifetimeCookiesForAura(BigDecimal aura) {
        if (aura == null || aura.signum() <= 0) return BigDecimal.ZERO;
        return aura.multiply(aura, CookieMath.MC).multiply(aura, CookieMath.MC).multiply(TRILLION);
    }

    /** All-time cookies still needed before the next aura point is reachable. */
    public static BigDecimal cookiesUntilNextAura(PlayerData data) {
        if (data == null) return TRILLION;
        BigDecimal lifetime = nonNull(data.getLifetimeCookiesEarned());
        BigDecimal next = totalAuraEarnable(lifetime).max(nonNull(data.getAura())).add(BigDecimal.ONE);
        BigDecimal needed = lifetimeCookiesForAura(next).subtract(lifetime);
        return needed.signum() > 0 ? needed : BigDecimal.ZERO;
    }

    public static boolean canPrestige(PlayerData data) {
        return calculateAuraGain(data).signum() > 0;
    }

    private static BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** +1% CPS per prestige level. */
    public static BigDecimal getUpgradeMultiplier(int prestigeLevel) {
        return BigDecimal.ONE.add(PERCENT.multiply(BigDecimal.valueOf(Math.max(0, prestigeLevel))));
    }

    /** +1% CPS per Aura point (heavenly chips). */
    public static BigDecimal getAuraCpsMultiplier(BigDecimal aura) {
        if (aura == null || aura.signum() <= 0) return BigDecimal.ONE;
        return BigDecimal.ONE.add(PERCENT.multiply(aura));
    }

    /** Prestige no longer adds a separate click multiplier; chips affect CpS. */
    public static BigDecimal getClickMultiplier(int prestigeLevel, BigDecimal aura) {
        return getUpgradeMultiplier(prestigeLevel);
    }

    public static BigDecimal getBaseClickAdditive(int prestigeLevel) {
        return BigDecimal.ZERO;
    }

    public static void performPrestige(PlayerData data) {
        performPrestige(data, calculateAuraGain(data));
    }

    /**
     * Ascend, granting exactly {@code auraGain}. Callers that showed the player a
     * figure pass it back in so the granted amount matches what was confirmed.
     * Note lifetimeCookiesEarned is deliberately NOT reset - it is the basis of
     * the cumulative aura curve, and zeroing it would make aura farmable again.
     */
    public static void performPrestige(PlayerData data, BigDecimal auraGain) {
        if (auraGain == null || auraGain.signum() < 0) auraGain = BigDecimal.ZERO;
        data.setAura(nonNull(data.getAura()).add(auraGain));
        data.setPrestigeLevel(data.getPrestigeLevel() + 1);

        data.setCookies(BigDecimal.ZERO);
        data.setTotalCookiesEarned(BigDecimal.ZERO);
        data.setTimesClicked(0);
        if (data.getActiveProfile() != null) {
            data.getActiveProfile().setOwnerClicks(0);
            data.getActiveProfile().setOtherClicks(0);
            data.getActiveProfile().resetBuildingCookies();
            data.getActiveProfile().setCookiesFromClicks(BigDecimal.ZERO);
            data.getActiveProfile().getPurchasedUpgrades().clear();
        }
        for (UpgradeType type : UpgradeType.values()) {
            data.setUpgradeCount(type, 0);
        }

        data.setLastCurrentDigitCount(0);
        data.setLastTotalDigitCount(0);
        data.setLastEntropyDigitCount(0);
        data.setLastEntropyLeadDigit(0);
    }
}
