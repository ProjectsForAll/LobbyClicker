package gg.drak.lobbyclicker.upgrades;

import org.bukkit.Material;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cookie Clicker upgrade tree (Minecraft names). Mirrors CC unlock counts, prices, and effects.
 */
public final class ClickerUpgradeCatalog {
    private static final int[] TIERS = {1, 5, 25, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600};
    private static final String[] TIER_MULTS = {
            "10", "50", "500", "50000", "5000000", "500000000", "50000000000",
            "5000000000000", "500000000000000", "50000000000000000", "5000000000000000000",
            "500000000000000000000", "50000000000000000000000", "5000000000000000000000000",
            "500000000000000000000000000"
    };
    private static final String[] TIER_PREFIX = {
            "Reinforced", "Caramelized", "Steel-Plated", "Enchanted", "Gilded",
            "Netherite", "Chorus", "Sculk", "Prismarine", "Ancient",
            "Beacon", "Dragon", "Wither", "Warden", "Star"
    };
    private static final Material[] TIER_MAT = {
            Material.STONE, Material.HONEY_BOTTLE, Material.IRON_INGOT, Material.ENCHANTED_BOOK, Material.GOLD_INGOT,
            Material.NETHERITE_INGOT, Material.CHORUS_FRUIT, Material.SCULK, Material.PRISMARINE_CRYSTALS, Material.ANCIENT_DEBRIS,
            Material.BEACON, Material.DRAGON_BREATH, Material.WITHER_SKELETON_SKULL, Material.ECHO_SHARD, Material.NETHER_STAR
    };

    private static final Map<String, ClickerUpgrade> BY_ID = new LinkedHashMap<>();
    private static final List<ClickerUpgrade> ALL = new ArrayList<>();

    static {
        build();
    }

    private ClickerUpgradeCatalog() {}

    public static List<ClickerUpgrade> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static ClickerUpgrade byId(String id) {
        if (id == null) return null;
        return BY_ID.get(id);
    }

    private static void add(ClickerUpgrade upgrade) {
        BY_ID.put(upgrade.getId(), upgrade);
        ALL.add(upgrade);
    }

    private static void build() {
        addMouseAndFingers();
        addBuildingTiers();
        addGolden();
        addGlobal();
        addSynergies();
    }

    private static void addMouseAndFingers() {
        add(new ClickerUpgrade("PLASTIC_MOUSE", "Wooden Mouse", "Your clicking finger grows stronger.",
                Material.OAK_BUTTON, "50000", ClickerUpgradeEffect.CPC_MULTIPLIER, "2", null, 1000));
        add(new ClickerUpgrade("IRON_MOUSE", "Stone Mouse", "Reinforced clicking technique.",
                Material.STONE_BUTTON, "5000000", ClickerUpgradeEffect.CPC_MULTIPLIER, "2", null, 100000));
        add(new ClickerUpgrade("TITANIUM_MOUSE", "Iron Mouse", "Industrial-grade clicking power.",
                Material.IRON_NUGGET, "500000000", ClickerUpgradeEffect.CPC_MULTIPLIER, "2", null, 10000000));
        add(new ClickerUpgrade("DIAMOND_MOUSE", "Diamond Mouse", "Click with the force of diamonds.",
                Material.DIAMOND, "50000000000", ClickerUpgradeEffect.CPC_MULTIPLIER, "2", null, 1000000000));
        add(new ClickerUpgrade("NETHERITE_MOUSE", "Netherite Mouse", "The ultimate clicking tool.",
                Material.NETHERITE_INGOT, "5000000000000", ClickerUpgradeEffect.CPC_MULTIPLIER, "2", null, 0));
        add(new ClickerUpgrade("CELESTIAL_MOUSE", "Beacon Mouse", "Clicks blessed by beacon light.",
                Material.END_ROD, "500000000000000", ClickerUpgradeEffect.CPC_MULTIPLIER, "2", null, 0));

        add(new ClickerUpgrade("REINFORCED_FINGER", "Reinforced Redstone Finger", "The mouse and autoclickers are twice as efficient.",
                Material.ARROW, "100", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.CURSOR, 1));
        add(new ClickerUpgrade("CARPAL_WRAP", "Carpal Tunnel Wrap", "The mouse and autoclickers are twice as efficient.",
                Material.LEATHER, "500", ClickerUpgradeEffect.CPC_MULTIPLIER, "2", UpgradeType.CURSOR, 1));
        add(new ClickerUpgrade("AMBIDEXTROUS", "Ambidextrous Pistons", "The mouse and autoclickers are twice as efficient.",
                Material.PISTON, "10000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.CURSOR, 10));

        add(new ClickerUpgrade("THOUSAND_FINGERS", "Thousand Fingers", "Mouse and autoclickers gain +0.1 cookies for each non-autoclicker building.",
                Material.SPECTRAL_ARROW, "100000", ClickerUpgradeEffect.FINGER_ADDITIVE, "0.1", UpgradeType.CURSOR, 25));
        String[] fingerNames = {
                "Million Fingers", "Billion Fingers", "Trillion Fingers", "Quadrillion Fingers",
                "Quintillion Fingers", "Sextillion Fingers", "Septillion Fingers", "Octillion Fingers",
                "Nonillion Fingers", "Decillion Fingers", "Undecillion Fingers"
        };
        int[] fingerReq = {50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550};
        String[] fingerCost = {
                "10000000", "100000000", "1000000000", "10000000000", "10000000000000",
                "10000000000000000", "10000000000000000000", "10000000000000000000000",
                "10000000000000000000000000", "10000000000000000000000000000", "10000000000000000000000000000000"
        };
        String[] fingerMult = {"5", "10", "20", "20", "20", "20", "20", "20", "20", "20", "20"};
        for (int i = 0; i < fingerNames.length; i++) {
            add(new ClickerUpgrade("FINGERS_" + i, fingerNames[i],
                    "Multiplies the gain from Thousand Fingers by " + fingerMult[i] + ".",
                    Material.TIPPED_ARROW, fingerCost[i], ClickerUpgradeEffect.FINGER_MULTIPLIER, fingerMult[i],
                    UpgradeType.CURSOR, fingerReq[i]));
        }
    }

    private static void addBuildingTiers() {
        for (UpgradeType building : UpgradeType.values()) {
            if (building == UpgradeType.CURSOR) continue;
            for (int i = 0; i < TIERS.length; i++) {
                BigDecimal cost = building.getBaseCost().multiply(new BigDecimal(TIER_MULTS[i]));
                String id = building.name() + "_T" + TIERS[i];
                String name = TIER_PREFIX[i] + " " + building.getDisplayName();
                add(new ClickerUpgrade(id, name,
                        building.getDisplayName() + "s are twice as efficient.",
                        TIER_MAT[i], cost.toPlainString(),
                        ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", building, TIERS[i]));
            }
        }
        // Cursor also has later ×2 tiers after the finger chain starts (CC: first 3 double, rest are fingers).
        // Extra cursor doubles at 1 already added; skip extra building-style cursor tiers.
    }

    private static void addGolden() {
        add(new ClickerUpgrade("LUCKY_DAY", "Lucky Day", "Golden cookies appear twice as often.",
                Material.GOLD_NUGGET, "777777777", ClickerUpgradeEffect.GOLDEN_FREQ_MULTIPLIER, "2", null, 0));
        add(new ClickerUpgrade("SERENDIPITY", "Serendipity", "Golden cookies appear twice as often.",
                Material.GOLDEN_APPLE, "77777777777", ClickerUpgradeEffect.GOLDEN_FREQ_MULTIPLIER, "2", null, 0));
        add(new ClickerUpgrade("GET_LUCKY", "Get Lucky", "Golden cookie effects last twice as long.",
                Material.GOLDEN_CARROT, "77777777777777", ClickerUpgradeEffect.GOLDEN_DURATION_MULTIPLIER, "2", null, 0));
        add(new ClickerUpgrade("GOLDEN_JACKPOT", "Jackpot Apple", "Golden cookies are worth twice as much.",
                Material.GOLDEN_APPLE, "7777777777", ClickerUpgradeEffect.GOLDEN_REWARD_MULTIPLIER, "2", null, 0));
        add(new ClickerUpgrade("GOLDEN_RUSH", "Gilded Fortune", "Golden cookies are worth twice as much.",
                Material.GLISTERING_MELON_SLICE, "777777777777", ClickerUpgradeEffect.GOLDEN_REWARD_MULTIPLIER, "2", null, 0));
    }

    private static void addGlobal() {
        add(new ClickerUpgrade("COOKIE_ECONOMICS", "Cookie Economics", "Cookie production +1%.",
                Material.EMERALD, "999999", ClickerUpgradeEffect.CPS_MULTIPLIER, "1.01", null, 0));
        add(new ClickerUpgrade("OATMEAL_RAISIN", "Oatmeal Raisin", "Cookie production +1%.",
                Material.COOKIE, "9999999", ClickerUpgradeEffect.CPS_MULTIPLIER, "1.01", null, 0));
        add(new ClickerUpgrade("PEANUT_BUTTER", "Peanut Butter Cookie", "Cookie production +1%.",
                Material.COOKIE, "99999999", ClickerUpgradeEffect.CPS_MULTIPLIER, "1.01", null, 0));
        add(new ClickerUpgrade("PLAIN_COOKIE", "Sugar Cookie", "Cookie production +1%.",
                Material.SUGAR, "999999999", ClickerUpgradeEffect.CPS_MULTIPLIER, "1.01", null, 0));
        add(new ClickerUpgrade("BAKING_MASTERY", "Baking Mastery", "Cookie production +2%.",
                Material.CAKE, "99999999999", ClickerUpgradeEffect.CPS_MULTIPLIER, "1.02", null, 0));
        add(new ClickerUpgrade("COOKIE_FORTUNE", "Fortune Cookie", "Cookie production +2%.",
                Material.PAPER, "9999999999999", ClickerUpgradeEffect.CPS_MULTIPLIER, "1.02", null, 0));
        add(new ClickerUpgrade("ASTRAL_SUPPLY", "Astral Supply Chain", "Cookie production +2%.",
                Material.END_STONE, "999999999999999", ClickerUpgradeEffect.CPS_MULTIPLIER, "1.02", null, 0));
    }

    private static void addSynergies() {
        addSynergy("FARM_MINE", UpgradeType.FARM, UpgradeType.MINE, "11000000");
        addSynergy("MINE_FACTORY", UpgradeType.MINE, UpgradeType.FACTORY, "130000000");
        addSynergy("FACTORY_BANK", UpgradeType.FACTORY, UpgradeType.BANK, "1400000000");
        addSynergy("BANK_TEMPLE", UpgradeType.BANK, UpgradeType.TEMPLE, "20000000000");
        addSynergy("TEMPLE_WIZARD", UpgradeType.TEMPLE, UpgradeType.WIZARD_TOWER, "330000000000");
        addSynergy("WIZARD_SHIP", UpgradeType.WIZARD_TOWER, UpgradeType.SHIPMENT, "5100000000000");
        addSynergy("SHIP_ALCHEMY", UpgradeType.SHIPMENT, UpgradeType.ALCHEMY_LAB, "75000000000000");
        addSynergy("ALCHEMY_PORTAL", UpgradeType.ALCHEMY_LAB, UpgradeType.PORTAL, "1000000000000000");
        addSynergy("PORTAL_TIME", UpgradeType.PORTAL, UpgradeType.TIME_MACHINE, "14000000000000000");
        addSynergy("TIME_ANTI", UpgradeType.TIME_MACHINE, UpgradeType.ANTIMATTER, "170000000000000000");
        addSynergy("ANTI_PRISM", UpgradeType.ANTIMATTER, UpgradeType.PRISM, "2100000000000000000");
        addSynergy("PRISM_LOOT", UpgradeType.PRISM, UpgradeType.CHANCEMAKER, "26000000000000000000");
        addSynergy("LOOT_FRACTAL", UpgradeType.CHANCEMAKER, UpgradeType.FRACTAL_ENGINE, "310000000000000000000");
        addSynergy("FRACTAL_CONSOLE", UpgradeType.FRACTAL_ENGINE, UpgradeType.JAVASCRIPT_CONSOLE, "71000000000000000000000");
        addSynergy("CONSOLE_HUB", UpgradeType.JAVASCRIPT_CONSOLE, UpgradeType.IDLEVERSE, "12000000000000000000000000");
        addSynergy("HUB_CORTEX", UpgradeType.IDLEVERSE, UpgradeType.CORTEX_BAKER, "1900000000000000000000000000");
        addSynergy("CORTEX_YOU", UpgradeType.CORTEX_BAKER, UpgradeType.YOU, "540000000000000000000000000000");
    }

    private static void addSynergy(String id, UpgradeType a, UpgradeType b, String cost) {
        add(new ClickerUpgrade(id + "_A", a.getDisplayName() + " Synergy",
                a.getDisplayName() + "s gain +5% CpS per " + b.getDisplayName() + ".",
                a.getMaterial(), cost, ClickerUpgradeEffect.SYNERGY, "0.05", a, b, 15));
        add(new ClickerUpgrade(id + "_B", b.getDisplayName() + " Synergy",
                b.getDisplayName() + "s gain +5% CpS per " + a.getDisplayName() + ".",
                b.getMaterial(), cost, ClickerUpgradeEffect.SYNERGY, "0.05", b, a, 15));
    }
}
