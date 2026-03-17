package gg.drak.lobbyclicker.upgrades;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum UpgradeType {
    CURSOR("Cursor", Material.ARROW, 15, 1.15, 0.1, 0, 1, "An autoclicker that clicks once every 10 seconds."),
    GRANDMA("Grandma", Material.BREAD, 100, 1.15, 1, 0, 5, "A nice grandma to bake more cookies."),
    FARM("Farm", Material.WHEAT, 1100, 1.15, 8, 0, 20, "Grows cookie plants from cookie seeds."),
    MINE("Mine", Material.IRON_PICKAXE, 12000, 1.15, 47, 0, 50, "Mines out cookie dough and chocolate chips."),
    FACTORY("Factory", Material.FURNACE, 130000, 1.15, 260, 0, 100, "Produces large quantities of cookies."),
    BANK("Bank", Material.GOLD_INGOT, 1400000, 1.15, 1400, 0, 250, "Generates cookies from interest."),
    TEMPLE("Temple", Material.ENCHANTING_TABLE, 20000000, 1.15, 7800, 0, 500, "Full of ancient cookie-worship."),
    CLICK_POWER("Click Power", Material.GOLDEN_SWORD, 50, 1.3, 0, 1, 3, "Increases cookies earned per click."),
    ;

    private final String displayName;
    private final Material material;
    private final double baseCost;
    private final double costMultiplier;
    private final double cpsPerLevel;
    private final double cpcPerLevel;
    private final int entropyWeight;
    private final String description;

    UpgradeType(String displayName, Material material, double baseCost, double costMultiplier,
                double cpsPerLevel, double cpcPerLevel, int entropyWeight, String description) {
        this.displayName = displayName;
        this.material = material;
        this.baseCost = baseCost;
        this.costMultiplier = costMultiplier;
        this.cpsPerLevel = cpsPerLevel;
        this.cpcPerLevel = cpcPerLevel;
        this.entropyWeight = entropyWeight;
        this.description = description;
    }

    public double getCost(int owned) {
        return Math.floor(baseCost * Math.pow(costMultiplier, owned));
    }
}
