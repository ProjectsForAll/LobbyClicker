package gg.drak.lobbyclicker.upgrades;

import gg.drak.lobbyclicker.math.CookieMath;
import lombok.Getter;
import org.bukkit.Material;

import java.math.BigDecimal;

@Getter
public enum UpgradeType {
    // Row 1 (slots 19-22 -> indexes 18-21, plus click power at index 13)
    CURSOR("Cursor", Material.ARROW, "15", "1.15", "0.1", "0", 1, false, "An autoclicker that clicks once every 10 seconds."),
    GRANDMA("Grandma", Material.BREAD, "100", "1.15", "1", "0", 5, false, "A nice grandma to bake more cookies."),
    FARM("Farm", Material.WHEAT, "1100", "1.15", "8", "0", 20, false, "Grows cookie plants from cookie seeds."),
    MINE("Mine", Material.IRON_PICKAXE, "12000", "1.15", "47", "0", 50, false, "Mines out cookie dough and chocolate chips."),

    // Row 2 (slots 24-26 -> indexes 23-25)
    FACTORY("Factory", Material.FURNACE, "130000", "1.15", "260", "0", 100, false, "Produces large quantities of cookies."),
    BANK("Bank", Material.GOLD_INGOT, "1400000", "1.15", "1400", "0", 250, false, "Generates cookies from interest."),
    TEMPLE("Temple", Material.ENCHANTING_TABLE, "20000000", "1.15", "7800", "0", 500, false, "Full of ancient cookie-worship."),

    // Click Power (slot 14 -> index 13)
    CLICK_POWER("Click Power", Material.GOLDEN_SWORD, "50", "1.3", "0", "1", 3, false, "Increases cookies earned per click."),

    // Row 3 - Hidden upgrades (slots 29-35 -> indexes 28-34), revealed when affordable
    WIZARD_TOWER("Wizard Tower", Material.BLAZE_ROD, "330000000", "1.15", "44000", "0", 750, true, "Conjures cookies with arcane magic."),
    SHIPMENT("Shipment", Material.CHEST_MINECART, "5100000000", "1.15", "260000", "0", 1000, true, "Imports cookies from the cookieverse."),
    ALCHEMY_LAB("Alchemy Lab", Material.BREWING_STAND, "75000000000", "1.15", "1600000", "0", 1500, true, "Transmutes gold into cookies."),
    PORTAL("Portal", Material.END_PORTAL_FRAME, "1000000000000", "1.15", "10000000", "0", 2000, true, "Opens a portal to the cookiedimension."),
    TIME_MACHINE("Time Machine", Material.CLOCK, "14000000000000", "1.15", "65000000", "0", 3000, true, "Brings cookies from the past."),
    ANTIMATTER("Antimatter", Material.DRAGON_EGG, "170000000000000", "1.15", "430000000", "0", 5000, true, "Condenses antimatter into cookies."),
    PRISM("Prism", Material.PRISMARINE_SHARD, "2100000000000000", "1.15", "2900000000", "0", 8000, true, "Converts light into cookies."),
    ;

    private final String displayName;
    private final Material material;
    private final BigDecimal baseCost;
    private final BigDecimal costMultiplier;
    private final BigDecimal cpsPerLevel;
    private final BigDecimal cpcPerLevel;
    private final int entropyWeight;
    private final boolean hidden;
    private final String description;

    UpgradeType(String displayName, Material material, String baseCost, String costMultiplier,
                String cpsPerLevel, String cpcPerLevel, int entropyWeight, boolean hidden, String description) {
        this.displayName = displayName;
        this.material = material;
        this.baseCost = new BigDecimal(baseCost);
        this.costMultiplier = new BigDecimal(costMultiplier);
        this.cpsPerLevel = new BigDecimal(cpsPerLevel);
        this.cpcPerLevel = new BigDecimal(cpcPerLevel);
        this.entropyWeight = entropyWeight;
        this.hidden = hidden;
        this.description = description;
    }

    public BigDecimal getCost(int owned) {
        return CookieMath.floor(baseCost.multiply(CookieMath.pow(costMultiplier, owned)));
    }
}
