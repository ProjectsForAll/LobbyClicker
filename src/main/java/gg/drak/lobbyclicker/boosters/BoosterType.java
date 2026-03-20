package gg.drak.lobbyclicker.boosters;

import lombok.Getter;
import org.bukkit.Material;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public enum BoosterType {
    CPS_BOOST_2X("CPS Boost x2", "Doubles cookies per second", Material.SUGAR, "500000", 300, 600, "2", BoosterEffect.CPS_MULTIPLIER),
    CPS_BOOST_5X("CPS Boost x5", "5x cookies per second", Material.SUGAR_CANE, "5000000", 300, 1200, "5", BoosterEffect.CPS_MULTIPLIER),
    CPC_BOOST_2X("CPC Boost x2", "Doubles cookies per click", Material.BLAZE_POWDER, "250000", 300, 600, "2", BoosterEffect.CPC_MULTIPLIER),
    CPC_BOOST_5X("CPC Boost x5", "5x cookies per click", Material.MAGMA_CREAM, "2500000", 300, 1200, "5", BoosterEffect.CPC_MULTIPLIER),
    GOLDEN_FREQ("Golden Rush", "Golden cookies 3x more often", Material.GOLD_NUGGET, "2000000", 300, 900, "3", BoosterEffect.GOLDEN_FREQ),
    GOLDEN_REWARD("Golden Jackpot", "Golden cookie rewards 3x", Material.GOLD_INGOT, "3000000", 300, 900, "3", BoosterEffect.GOLDEN_REWARD),
    MEGA_CPS("Mega CPS x10", "10x cookies per second", Material.GLOWSTONE_DUST, "50000000", 180, 3600, "10", BoosterEffect.CPS_MULTIPLIER),
    MEGA_CPC("Mega CPC x10", "10x cookies per click", Material.REDSTONE, "25000000", 180, 3600, "10", BoosterEffect.CPC_MULTIPLIER),
    ;

    private final String displayName;
    private final String description;
    private final Material material;
    private final BigDecimal cost;
    private final int durationSeconds;
    private final int cooldownSeconds;
    private final BigDecimal effectValue;
    private final BoosterEffect effect;

    BoosterType(String displayName, String description, Material material, String cost,
                int durationSeconds, int cooldownSeconds, String effectValue, BoosterEffect effect) {
        this.displayName = displayName;
        this.description = description;
        this.material = material;
        this.cost = new BigDecimal(cost);
        this.durationSeconds = durationSeconds;
        this.cooldownSeconds = cooldownSeconds;
        this.effectValue = new BigDecimal(effectValue);
        this.effect = effect;
    }

    /**
     * Get the cost scaled by prestige level.
     * Each prestige level multiplies the base cost by 1.5.
     * Prestige 0 = base cost, Prestige 1 = 1.5x, Prestige 2 = 2.25x, etc.
     */
    public BigDecimal getCost(int prestigeLevel) {
        if (prestigeLevel <= 0) return cost;
        BigDecimal multiplier = BigDecimal.valueOf(1.5).pow(prestigeLevel);
        return cost.multiply(multiplier).setScale(0, RoundingMode.FLOOR);
    }
}
