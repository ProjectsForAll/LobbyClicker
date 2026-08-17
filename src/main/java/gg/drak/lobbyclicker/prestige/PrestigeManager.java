package gg.drak.lobbyclicker.prestige;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.math.CookieMath;
import gg.drak.lobbyclicker.upgrades.UpgradeType;

import java.math.BigDecimal;

/**
 * Hybrid Cookie Clicker prestige: heavenly-chip math, displayed as Aura.
 * Aura gain = floor(cbrt(cookies baked this ascension / 1e12))
 * CPS * (1 + 0.01 * prestigeLevel) * (1 + 0.01 * aura)
 */
public class PrestigeManager {
    public static final BigDecimal TRILLION = new BigDecimal("1000000000000");
    private static final BigDecimal PERCENT = new BigDecimal("0.01");

    public static BigDecimal calculateAuraGain(PlayerData data) {
        if (data == null) return BigDecimal.ZERO;
        return calculateAuraGain(data.getTotalCookiesEarned());
    }

    public static BigDecimal calculateAuraGain(BigDecimal cookiesThisAscension) {
        if (cookiesThisAscension == null || cookiesThisAscension.signum() <= 0) return BigDecimal.ZERO;
        return CookieMath.cbrtFloor(cookiesThisAscension.divide(TRILLION, CookieMath.MC));
    }

    public static boolean canPrestige(PlayerData data) {
        return calculateAuraGain(data).signum() > 0;
    }

    @Deprecated
    public static BigDecimal getPrestigeCost(int currentLevel, BigDecimal aura) {
        return TRILLION;
    }

    public static BigDecimal getPrestigeCost(int currentLevel) {
        return TRILLION;
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
        BigDecimal auraGain = calculateAuraGain(data);
        data.setAura(data.getAura().add(auraGain));
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
