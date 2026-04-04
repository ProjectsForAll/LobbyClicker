package gg.drak.lobbyclicker.upgrades;

import gg.drak.lobbyclicker.realm.RealmProfile;
import lombok.Getter;
import org.bukkit.Material;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One-time purchasable upgrades (distinct from repeatable Cookie Helpers).
 * Each upgrade can only be bought once per realm profile and provides a permanent effect.
 */
@Getter
public enum ClickerUpgrade {

    // === Click Power Upgrades (multiply total CPC) ===
    // Unlock requirement: total realm clicks
    PLASTIC_MOUSE("Plastic Mouse", "Your clicking finger grows stronger.",
            Material.STONE_BUTTON, "100", ClickerUpgradeEffect.CPC_MULTIPLIER, "2", null, 10),
    IRON_MOUSE("Iron Mouse", "Reinforced clicking technique.",
            Material.IRON_NUGGET, "500", ClickerUpgradeEffect.CPC_MULTIPLIER, "2", null, 25),
    TITANIUM_MOUSE("Titanium Mouse", "Industrial-grade clicking power.",
            Material.IRON_INGOT, "10000", ClickerUpgradeEffect.CPC_MULTIPLIER, "2", null, 100),
    DIAMOND_MOUSE("Diamond Mouse", "Click with the force of diamonds.",
            Material.DIAMOND, "100000", ClickerUpgradeEffect.CPC_MULTIPLIER, "2", null, 500),
    NETHERITE_MOUSE("Netherite Mouse", "The ultimate clicking tool.",
            Material.NETHERITE_INGOT, "10000000", ClickerUpgradeEffect.CPC_MULTIPLIER, "2", null, 2500),

    // === Cursor Upgrades ===
    FASTER_CURSORS("Faster Cursors", "Cursors click twice as fast.",
            Material.ARROW, "100", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.CURSOR, 1),
    AMBIDEXTROUS("Ambidextrous", "Cursors use both hands.",
            Material.SPECTRAL_ARROW, "500", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.CURSOR, 10),

    // === Grandma Upgrades ===
    GRANDMA_SECRET("Grandma's Secret Recipe", "A secret ingredient makes grandma's cookies irresistible.",
            Material.COOKIE, "1000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.GRANDMA, 1),
    STEEL_ROLLING_PINS("Steel Rolling Pins", "Industrial-grade baking equipment.",
            Material.IRON_INGOT, "5000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.GRANDMA, 25),

    // === Farm Upgrades ===
    BETTER_IRRIGATION("Better Irrigation", "Improved watering systems for cookie crops.",
            Material.WATER_BUCKET, "11000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.FARM, 1),
    COOKIE_SEEDS("Cookie Seeds", "Genetically modified seeds that grow cookies directly.",
            Material.WHEAT_SEEDS, "55000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.FARM, 25),

    // === Mine Upgrades ===
    DEEPER_SHAFTS("Deeper Shafts", "Dig deeper for richer cookie ore veins.",
            Material.IRON_PICKAXE, "120000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.MINE, 1),
    DIAMOND_DRILLS("Diamond-Tipped Drills", "Diamond drills cut through anything.",
            Material.DIAMOND_PICKAXE, "600000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.MINE, 25),

    // === Factory Upgrades ===
    ASSEMBLY_LINES("Assembly Lines", "Streamlined production for maximum output.",
            Material.HOPPER, "1300000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.FACTORY, 1),
    ROBOT_WORKERS("Robot Workers", "Tireless machines that never need breaks.",
            Material.PISTON, "6500000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.FACTORY, 25),

    // === Bank Upgrades ===
    TALLER_VAULTS("Taller Vaults", "More vault space means more cookie interest.",
            Material.GOLD_INGOT, "14000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.BANK, 1),
    COMPOUND_INTEREST("Compound Interest", "Interest on your interest. Cookie finance at its finest.",
            Material.GOLD_BLOCK, "70000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.BANK, 25),

    // === Temple Upgrades ===
    ANCIENT_TEXTS("Ancient Texts", "Discover lost cookie scriptures.",
            Material.BOOK, "200000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.TEMPLE, 1),
    DIVINE_BAKING("Divine Baking", "Channel divine energy into cookie creation.",
            Material.ENCHANTED_BOOK, "1000000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.TEMPLE, 25),

    // === Click Power Building Upgrades ===
    SHARPER_SWORDS("Sharper Swords", "A keener edge for more effective clicking.",
            Material.GOLDEN_SWORD, "100", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.CLICK_POWER, 1),
    DIAMOND_EDGE("Diamond Edge", "Diamond-tipped swords for ultimate click power.",
            Material.DIAMOND_SWORD, "1000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.CLICK_POWER, 10),

    // === Hidden Building Upgrades (1 tier each) ===
    ARCANE_RECIPES("Arcane Recipes", "Ancient magical cookie formulas.",
            Material.BLAZE_ROD, "3300000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.WIZARD_TOWER, 1),
    BIGGER_CONTAINERS("Bigger Containers", "Larger ships carry more cookies.",
            Material.CHEST_MINECART, "51000000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.SHIPMENT, 1),
    PHILOSOPHERS_SCONE("Philosopher's Scone", "Turn lead into cookies with alchemy.",
            Material.BREWING_STAND, "750000000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.ALCHEMY_LAB, 1),
    COOKIE_DIMENSION("Cookie Dimension", "A realm made entirely of cookies.",
            Material.END_PORTAL_FRAME, "10000000000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.PORTAL, 1),
    PARADOX_RESOLVER("Paradox Resolver", "Fix time paradoxes to bring more cookies.",
            Material.CLOCK, "140000000000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.TIME_MACHINE, 1),
    COOKIE_CONDENSATION("Cookie Condensation", "Compress antimatter into dense cookie matter.",
            Material.DRAGON_EGG, "1700000000000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.ANTIMATTER, 1),
    BRIGHTER_BEAMS("Brighter Beams", "Concentrate more light into cookie energy.",
            Material.PRISMARINE_SHARD, "21000000000000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.PRISM, 1),

    // === Global CPS Upgrades ===
    COOKIE_ECONOMICS("Cookie Economics", "Optimize your cookie supply chain.",
            Material.EMERALD, "1000000", ClickerUpgradeEffect.CPS_MULTIPLIER, "1.1", null, 0),
    COOKIE_FORTUNE("Cookie Fortune", "The stars align for cookie production.",
            Material.EMERALD_BLOCK, "100000000", ClickerUpgradeEffect.CPS_MULTIPLIER, "1.25", null, 0),
    BAKING_MASTERY("Baking Mastery", "True mastery of the cookie arts.",
            Material.NETHER_STAR, "10000000000", ClickerUpgradeEffect.CPS_MULTIPLIER, "1.5", null, 0),

    // === Golden Cookie Upgrades ===
    LUCKY_DAY("Lucky Day", "Golden cookies appear twice as often.",
            Material.GOLD_NUGGET, "777777", ClickerUpgradeEffect.GOLDEN_FREQ_MULTIPLIER, "2", null, 0),
    SERENDIPITY("Serendipity", "Golden cookies are worth twice as much.",
            Material.GOLDEN_APPLE, "7777777", ClickerUpgradeEffect.GOLDEN_REWARD_MULTIPLIER, "2", null, 0),
    GET_LUCKY("Get Lucky", "Golden cookies stick around twice as long.",
            Material.GOLDEN_CARROT, "77777777", ClickerUpgradeEffect.GOLDEN_DURATION_MULTIPLIER, "2", null, 0),

    // === Prestige-gated late game (helpers + global production) ===
    STELLAR_BLUEPRINTS("Stellar Blueprints", "Engineer cookie forges that run on captured starlight.",
            Material.END_CRYSTAL, "260000000000000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.STARFORGE, 1, 5),
    VOID_SEAL("Void Seals", "Lock unstable void energy into stable cookie storage.",
            Material.CRYING_OBSIDIAN, "3200000000000000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.VOID_VAULT, 1, 10),
    GRAVITY_WHISK("Gravity Whisk", "Stir singularities until they cough up crumbs.",
            Material.NETHER_STAR, "40000000000000000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.SINGULARITY, 1, 18),
    INFINITE_BATCH("Infinite Batch License", "Legal in most timelines for bulk omniversal baking.",
            Material.LODESTONE, "500000000000000000000", ClickerUpgradeEffect.BUILDING_MULTIPLIER, "2", UpgradeType.OMNIBAKERY, 1, 25),

    ASTRAL_SUPPLY_CHAIN("Astral Supply Chain", "Route ingredients through low-orbit logistics.",
            Material.END_STONE, "5000000000000", ClickerUpgradeEffect.CPS_MULTIPLIER, "1.75", null, 0, 5),
    CHRONOS_SYNERGY("Chronos Synergy", "When every second counts, steal a few from yesterday.",
            Material.PURPUR_BLOCK, "80000000000000", ClickerUpgradeEffect.CPS_MULTIPLIER, "2", null, 0, 12),
    REALITY_GLAZE("Reality Glaze", "A finish so shiny it bends production upward.",
            Material.SCULK_SHRIEKER, "5000000000000000", ClickerUpgradeEffect.CPS_MULTIPLIER, "2.25", null, 0, 22),

    COSMIC_FORTUNE("Cosmic Fortune", "Golden cookies sense your ascended bakery.",
            Material.SUNFLOWER, "888888888", ClickerUpgradeEffect.GOLDEN_FREQ_MULTIPLIER, "2", null, 0, 7),
    JACKPOT_INFINITY("Jackpot Infinity", "Golden payouts scale with your ambition.",
            Material.GLISTERING_MELON_SLICE, "8888888888", ClickerUpgradeEffect.GOLDEN_REWARD_MULTIPLIER, "2", null, 0, 14),
    TIMELESS_SNACK("Timeless Snack", "Golden cookies linger like legends.",
            Material.HONEY_BOTTLE, "88888888888", ClickerUpgradeEffect.GOLDEN_DURATION_MULTIPLIER, "2", null, 0, 18),

    CELESTIAL_MOUSE("Celestial Mouse", "Clicks blessed by the outer realms.",
            Material.END_ROD, "500000000000", ClickerUpgradeEffect.CPC_MULTIPLIER, "2", null, 75000, 8),
    ;

    private final String displayName;
    private final String description;
    private final Material material;
    private final BigDecimal cost;
    private final ClickerUpgradeEffect effect;
    private final BigDecimal effectValue;
    private final UpgradeType targetBuilding;
    private final int requiredCount;
    private final int requiredPrestigeLevel;

    ClickerUpgrade(String displayName, String description, Material material, String cost,
                   ClickerUpgradeEffect effect, String effectValue, UpgradeType targetBuilding, int requiredCount) {
        this(displayName, description, material, cost, effect, effectValue, targetBuilding, requiredCount, 0);
    }

    ClickerUpgrade(String displayName, String description, Material material, String cost,
                   ClickerUpgradeEffect effect, String effectValue, UpgradeType targetBuilding, int requiredCount,
                   int requiredPrestigeLevel) {
        this.displayName = displayName;
        this.description = description;
        this.material = material;
        this.cost = new BigDecimal(cost);
        this.effect = effect;
        this.effectValue = new BigDecimal(effectValue);
        this.targetBuilding = targetBuilding;
        this.requiredCount = requiredCount;
        this.requiredPrestigeLevel = requiredPrestigeLevel;
    }

    /**
     * Whether this upgrade's requirements are met (player can see/buy it).
     * CPC_MULTIPLIER upgrades require total realm clicks.
     * BUILDING_MULTIPLIER upgrades require owning N of the target building.
     * Other upgrades are always unlocked.
     */
    public boolean isUnlocked(RealmProfile profile) {
        if (profile == null) return false;
        if (requiredPrestigeLevel > 0 && profile.getPrestigeLevel() < requiredPrestigeLevel) {
            return false;
        }
        if (requiredCount <= 0) return true;
        if (effect == ClickerUpgradeEffect.CPC_MULTIPLIER) {
            return profile.getTimesClicked() >= requiredCount;
        }
        if (targetBuilding != null) {
            return profile.getUpgradeCount(targetBuilding) >= requiredCount;
        }
        return true;
    }

    /**
     * Whether this upgrade should be hidden until requirements are closer to being met.
     * Hidden building upgrades are hidden until the target building itself is revealed.
     */
    public boolean isHidden(RealmProfile profile) {
        if (targetBuilding == null) return false;
        if (!targetBuilding.isHidden()) return false;
        if (profile == null) return true;
        // Hidden building upgrade: only show if the building itself is owned or revealed
        return profile.getUpgradeCount(targetBuilding) == 0
                && profile.getTotalCookiesEarned().compareTo(targetBuilding.getBaseCost()) < 0;
    }

    public static Set<ClickerUpgrade> deserialize(String data) {
        Set<ClickerUpgrade> set = EnumSet.noneOf(ClickerUpgrade.class);
        if (data != null && !data.isEmpty()) {
            for (String name : data.split(",")) {
                try {
                    set.add(ClickerUpgrade.valueOf(name.trim()));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return set;
    }

    public static String serialize(Set<ClickerUpgrade> upgrades) {
        if (upgrades.isEmpty()) return "";
        return upgrades.stream().map(Enum::name).collect(Collectors.joining(","));
    }
}
