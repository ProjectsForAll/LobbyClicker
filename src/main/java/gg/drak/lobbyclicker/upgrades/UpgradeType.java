package gg.drak.lobbyclicker.upgrades;

import gg.drak.lobbyclicker.math.CookieMath;
import lombok.Getter;
import org.bukkit.Material;

import java.math.BigDecimal;

@Getter
public enum UpgradeType {
    // Row 1 (slots 19-22 -> indexes 18-21, plus click power at index 13)
    CURSOR("Cursor", Material.ARROW, "15", "1.15", "0.1", "0", 1, false, "An autoclicker that clicks once every 10 seconds.", 0),
    GRANDMA("Grandma", Material.BREAD, "100", "1.15", "1", "0", 5, false, "A nice grandma to bake more cookies.", 0),
    FARM("Farm", Material.WHEAT, "1100", "1.15", "8", "0", 20, false, "Grows cookie plants from cookie seeds.", 0),
    MINE("Mine", Material.IRON_PICKAXE, "12000", "1.15", "47", "0", 50, false, "Mines out cookie dough and chocolate chips.", 0),

    // Row 2 (slots 24-26 -> indexes 23-25)
    FACTORY("Factory", Material.FURNACE, "130000", "1.15", "260", "0", 100, false, "Produces large quantities of cookies.", 0),
    BANK("Bank", Material.GOLD_INGOT, "1400000", "1.15", "1400", "0", 250, false, "Generates cookies from interest.", 0),
    TEMPLE("Temple", Material.ENCHANTING_TABLE, "20000000", "1.15", "7800", "0", 500, false, "Full of ancient cookie-worship.", 0),

    // Click Power (slot 14 -> index 13)
    CLICK_POWER("Click Power", Material.GOLDEN_SWORD, "50", "1.3", "0", "1", 3, false, "Increases cookies earned per click.", 0),

    // Row 3 - Hidden upgrades (slots 29-35 -> indexes 28-34), revealed when affordable
    WIZARD_TOWER("Wizard Tower", Material.BLAZE_ROD, "330000000", "1.15", "44000", "0", 750, true, "Conjures cookies with arcane magic.", 0),
    SHIPMENT("Shipment", Material.CHEST_MINECART, "5100000000", "1.15", "260000", "0", 1000, true, "Imports cookies from the cookieverse.", 0),
    ALCHEMY_LAB("Alchemy Lab", Material.BREWING_STAND, "75000000000", "1.15", "1600000", "0", 1500, true, "Transmutes gold into cookies.", 0),
    PORTAL("Portal", Material.END_PORTAL_FRAME, "1000000000000", "1.15", "10000000", "0", 2000, true, "Opens a portal to the cookiedimension.", 0),
    TIME_MACHINE("Time Machine", Material.CLOCK, "14000000000000", "1.15", "65000000", "0", 3000, true, "Brings cookies from the past.", 0),
    ANTIMATTER("Antimatter", Material.DRAGON_EGG, "170000000000000", "1.15", "430000000", "0", 5000, true, "Condenses antimatter into cookies.", 0),
    PRISM("Prism", Material.PRISMARINE_SHARD, "2100000000000000", "1.15", "2900000000", "0", 8000, true, "Converts light into cookies.", 0),

    // Late game — require prestige to purchase (still follow the CPS chain after Prism)
    STARFORGE("Starforge", Material.END_CRYSTAL, "26000000000000000", "1.15", "20000000000", "0", 12000, true, "Condenses starlight into industrial-grade cookies.", 5),
    VOID_VAULT("Void Vault", Material.CRYING_OBSIDIAN, "320000000000000000", "1.15", "150000000000", "0", 18000, true, "Stashes cookies in sealed pocket dimensions.", 10),
    SINGULARITY("Singularity Oven", Material.NETHER_STAR, "4000000000000000000", "1.15", "1200000000000", "0", 26000, true, "Infinite density kitchen physics.", 18),
    OMNIBAKERY("Omnibakery", Material.LODESTONE, "50000000000000000000", "1.15", "9000000000000", "0", 35000, true, "Every timeline chips in a fresh batch.", 25),
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
    /** Minimum prestige level on this realm to purchase (0 = no requirement). */
    private final int requiredPrestigeLevel;

    UpgradeType(String displayName, Material material, String baseCost, String costMultiplier,
                String cpsPerLevel, String cpcPerLevel, int entropyWeight, boolean hidden, String description,
                int requiredPrestigeLevel) {
        this.displayName = displayName;
        this.material = material;
        this.baseCost = new BigDecimal(baseCost);
        this.costMultiplier = new BigDecimal(costMultiplier);
        this.cpsPerLevel = new BigDecimal(cpsPerLevel);
        this.cpcPerLevel = new BigDecimal(cpcPerLevel);
        this.entropyWeight = entropyWeight;
        this.hidden = hidden;
        this.description = description;
        this.requiredPrestigeLevel = requiredPrestigeLevel;
    }

    public BigDecimal getCost(int owned) {
        return CookieMath.floor(baseCost.multiply(CookieMath.pow(costMultiplier, owned)));
    }

    /** CPS building progression chain (excludes CLICK_POWER which is standalone). */
    private static final UpgradeType[] CPS_CHAIN = {
            CURSOR, GRANDMA, FARM, MINE, FACTORY, BANK, TEMPLE,
            WIZARD_TOWER, SHIPMENT, ALCHEMY_LAB, PORTAL, TIME_MACHINE, ANTIMATTER, PRISM,
            STARFORGE, VOID_VAULT, SINGULARITY, OMNIBAKERY
    };

    /**
     * Get the previous building in the CPS progression chain.
     * Returns null if this is the first building or CLICK_POWER.
     */
    public UpgradeType getPreviousInChain() {
        for (int i = 1; i < CPS_CHAIN.length; i++) {
            if (CPS_CHAIN[i] == this) return CPS_CHAIN[i - 1];
        }
        return null;
    }
}
