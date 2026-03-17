package gg.drak.lobbyclicker.prestige;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.math.CookieMath;
import gg.drak.lobbyclicker.upgrades.UpgradeType;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PrestigeManager {
    private static final BigDecimal BASE_COST = new BigDecimal("100000000"); // 100M
    private static final BigDecimal COST_MULTIPLIER = new BigDecimal("1.5");
    private static final BigDecimal AURA_DIVISOR = new BigDecimal("1000000"); // 1M

    // Prestige multipliers
    private static final BigDecimal UPGRADE_MULT_PER_LEVEL = new BigDecimal("0.05"); // 5% per prestige
    private static final BigDecimal CLICK_MULT_PER_LEVEL = new BigDecimal("0.1");    // 10% per prestige
    private static final BigDecimal CLICK_MULT_PER_AURA = new BigDecimal("0.01");    // 1% per aura

    /**
     * Cost to prestige at the given level.
     * Formula: 100,000,000 * 1.5^level (slow exponential)
     */
    public static BigDecimal getPrestigeCost(int currentLevel) {
        return BASE_COST.multiply(CookieMath.pow(COST_MULTIPLIER, currentLevel));
    }

    /**
     * Whether the player can prestige (has enough cookies).
     */
    public static boolean canPrestige(PlayerData data) {
        return data.getCookies().compareTo(getPrestigeCost(data.getPrestigeLevel())) >= 0;
    }

    /**
     * Calculate aura gained from excess cookies over prestige cost.
     * Formula: (cookies - cost) / 1,000,000
     */
    public static BigDecimal calculateAuraGain(PlayerData data) {
        BigDecimal cost = getPrestigeCost(data.getPrestigeLevel());
        BigDecimal excess = data.getCookies().subtract(cost);
        if (excess.signum() <= 0) return BigDecimal.ZERO;
        return excess.divide(AURA_DIVISOR, 2, RoundingMode.FLOOR);
    }

    /**
     * Multiplier applied to upgrade effectiveness (CPS).
     * Formula: 1 + 0.05 * prestigeLevel (linear, 5% per level)
     */
    public static BigDecimal getUpgradeMultiplier(int prestigeLevel) {
        return BigDecimal.ONE.add(UPGRADE_MULT_PER_LEVEL.multiply(BigDecimal.valueOf(prestigeLevel)));
    }

    /**
     * Multiplier applied to click value (CPC).
     * Formula: 1 + 0.1 * prestigeLevel + 0.01 * aura
     */
    public static BigDecimal getClickMultiplier(int prestigeLevel, BigDecimal aura) {
        return BigDecimal.ONE
                .add(CLICK_MULT_PER_LEVEL.multiply(BigDecimal.valueOf(prestigeLevel)))
                .add(CLICK_MULT_PER_AURA.multiply(aura));
    }

    /**
     * Base additive component for click value.
     * Formula: 1 cookie per click per prestige level
     */
    public static BigDecimal getBaseClickAdditive(int prestigeLevel) {
        return BigDecimal.valueOf(prestigeLevel);
    }

    /**
     * Perform prestige: calculate aura gain, increment level, reset realm data.
     * Preserves: settings, friends, blocks, friend requests, prestige level, aura.
     * Resets: cookies, totalCookiesEarned, timesClicked, all upgrades.
     */
    public static void performPrestige(PlayerData data) {
        BigDecimal auraGain = calculateAuraGain(data);

        // Add aura
        data.setAura(data.getAura().add(auraGain));

        // Increment prestige
        data.setPrestigeLevel(data.getPrestigeLevel() + 1);

        // Reset realm data
        data.setCookies(BigDecimal.ZERO);
        data.setTotalCookiesEarned(BigDecimal.ZERO);
        data.setTimesClicked(0);
        for (UpgradeType type : UpgradeType.values()) {
            data.setUpgradeCount(type, 0);
        }

        // Reset milestone tracking
        data.setLastCurrentDigitCount(0);
        data.setLastTotalDigitCount(0);
        data.setLastEntropyDigitCount(0);
        data.setLastEntropyLeadDigit(0);
    }
}
