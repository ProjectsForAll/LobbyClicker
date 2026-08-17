package gg.drak.lobbyclicker.upgrades;

import gg.drak.lobbyclicker.math.CookieMath;
import lombok.Getter;
import org.bukkit.Material;

import java.math.BigDecimal;

/**
 * Cookie Clicker's 20 buildings, with Minecraft-flavored display names.
 */
@Getter
public enum UpgradeType {
    CURSOR("Autoclicker", Material.ARROW, "15", "0.1", 1, "A redstone piston that clicks once every 10 seconds."),
    GRANDMA("Village Baker", Material.BREAD, "100", "1", 5, "A nice villager grandma to bake more cookies."),
    FARM("Wheat Farm", Material.WHEAT, "1100", "8", 20, "Grows cookie plants from cookie seeds."),
    MINE("Cookie Mine", Material.IRON_PICKAXE, "12000", "47", 50, "Mines out cookie dough and chocolate chips."),
    FACTORY("Furnace Factory", Material.FURNACE, "130000", "260", 100, "Produces large quantities of cookies."),
    BANK("Gold Bank", Material.GOLD_INGOT, "1400000", "1400", 250, "Generates cookies from interest."),
    TEMPLE("Enchanting Temple", Material.ENCHANTING_TABLE, "20000000", "7800", 500, "Full of ancient cookie-worship."),
    WIZARD_TOWER("Blaze Tower", Material.BLAZE_ROD, "330000000", "44000", 750, "Conjures cookies with arcane blaze magic."),
    SHIPMENT("Minecart Shipment", Material.CHEST_MINECART, "5100000000", "260000", 1000, "Imports cookies from the cookieverse."),
    ALCHEMY_LAB("Brewing Lab", Material.BREWING_STAND, "75000000000", "1600000", 1500, "Transmutes gold into cookies."),
    PORTAL("End Portal", Material.END_PORTAL_FRAME, "1000000000000", "10000000", 2000, "Opens a portal to the cookiedimension."),
    TIME_MACHINE("Clockwork", Material.CLOCK, "14000000000000", "65000000", 3000, "Brings cookies from the past."),
    ANTIMATTER("Dragon Condenser", Material.DRAGON_EGG, "170000000000000", "430000000", 5000, "Condenses antimatter into cookies."),
    PRISM("Prismarine Lens", Material.PRISMARINE_SHARD, "2100000000000000", "2900000000", 8000, "Converts light into cookies."),
    CHANCEMAKER("Loot Shrine", Material.GOLDEN_APPLE, "26000000000000000", "21000000000", 12000, "Bends luck until cookies fall out of chests."),
    FRACTAL_ENGINE("Redstone Fractal", Material.REPEATER, "310000000000000000", "150000000000", 18000, "A self-repeating redstone engine of cookies."),
    JAVASCRIPT_CONSOLE("Command Console", Material.COMMAND_BLOCK, "71000000000000000000", "1100000000000", 26000, "Runs /give @a cookie at cosmic scale."),
    IDLEVERSE("End Gateway Hub", Material.ENDER_PEARL, "12000000000000000000000", "8300000000000", 35000, "Idle realms that bake while you explore."),
    CORTEX_BAKER("Sculk Cortex", Material.SCULK_CATALYST, "1900000000000000000000000", "64000000000000", 47000, "A thinking sculk mass that dreams of cookies."),
    YOU("Iron Clone Lab", Material.IRON_BLOCK, "540000000000000000000000000", "510000000000000", 59000, "A lab of iron golem clones that bake as you do.");

    private static final BigDecimal COST_MULT = new BigDecimal("1.15");

    private final String displayName;
    private final Material material;
    private final BigDecimal baseCost;
    private final BigDecimal cpsPerLevel;
    private final int entropyWeight;
    private final String description;

    UpgradeType(String displayName, Material material, String baseCost, String cpsPerLevel,
                int entropyWeight, String description) {
        this.displayName = displayName;
        this.material = material;
        this.baseCost = new BigDecimal(baseCost);
        this.cpsPerLevel = new BigDecimal(cpsPerLevel);
        this.entropyWeight = entropyWeight;
        this.description = description;
    }

    /** Cookie Clicker: ceil(baseCost * 1.15^owned). */
    public BigDecimal getCost(int owned) {
        return CookieMath.ceil(baseCost.multiply(CookieMath.pow(COST_MULT, owned)));
    }

    public BigDecimal getCpcPerLevel() {
        return BigDecimal.ZERO;
    }

    public boolean isHidden() {
        return false;
    }

    public int getRequiredPrestigeLevel() {
        return 0;
    }

    public UpgradeType getPreviousInChain() {
        UpgradeType[] all = values();
        for (int i = 1; i < all.length; i++) {
            if (all[i] == this) return all[i - 1];
        }
        return null;
    }
}
