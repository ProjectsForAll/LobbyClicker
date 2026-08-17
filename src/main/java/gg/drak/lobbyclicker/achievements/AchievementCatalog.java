package gg.drak.lobbyclicker.achievements;

import gg.drak.lobbyclicker.upgrades.UpgradeType;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every Cookie Clicker achievement that can exist in Minecraft (no garden, stocks, wrinklers, etc.).
 */
public final class AchievementCatalog {
    private static final Map<String, Achievement> BY_ID = new LinkedHashMap<>();
    private static final List<Achievement> ALL = new ArrayList<>();

    static {
        build();
    }

    private AchievementCatalog() {}

    public static List<Achievement> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static Achievement byId(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    public static Achievement fromLegacyQuest(String questName) {
        return switch (questName) {
            case "FIRST_CLICK", "HUNDRED_CLICKS" -> byId("CLICKTASTIC");
            case "COOKIE_BEGINNER" -> byId("WAKE_AND_BAKE");
            case "COOKIE_MILLIONAIRE" -> byId("FLEDGLING_BAKERY");
            case "COOKIE_BILLIONAIRE" -> byId("WORLD_FAMOUS_BAKERY");
            case "FIRST_HELPER" -> byId("CLICK");
            case "FIRST_PRESTIGE" -> byId("PRESTIGE_1");
            case "LUCKY_FIND" -> byId("GOLDEN_COOKIE");
            case "PASSIVE_INCOME" -> byId("HARDCORE_BAKING");
            default -> null;
        };
    }

    private static void add(Achievement a) {
        BY_ID.put(a.getId(), a);
        ALL.add(a);
    }

    private static Achievement n(String id, String name, String desc, Material mat,
                                 AchievementType type, String req, UpgradeType building, int extra) {
        Achievement a = new Achievement(id, name, desc, mat, type, AchievementTier.NORMAL, req, building, extra);
        add(a);
        return a;
    }

    private static void shadow(String id, String name, String desc, Material mat,
                               AchievementType type, String req) {
        add(new Achievement(id, name, desc, mat, type, AchievementTier.SHADOW, req, null, 0));
    }

    private static void build() {
        baked();
        cps();
        clicking();
        buildings();
        totals();
        golden();
        prestige();
        combos();
        shadows();
        n("HERE_YOU_GO", "Here you go", "Click this achievement slot.", Material.MAP,
                AchievementType.HERE_YOU_GO, "1", null, 0);
    }

    private static void baked() {
        String[][] rows = {
                {"WAKE_AND_BAKE", "Wake and bake", "1", "Bake 1 cookie in one ascension."},
                {"MAKING_SOME_DOUGH", "Making some dough", "1000", "Bake 1,000 cookies in one ascension."},
                {"SO_BAKED", "So baked right now", "100000", "Bake 100,000 cookies in one ascension."},
                {"FLEDGLING_BAKERY", "Fledgling bakery", "1000000", "Bake 1 million cookies in one ascension."},
                {"AFFLUENT_BAKERY", "Affluent bakery", "100000000", "Bake 100 million cookies in one ascension."},
                {"WORLD_FAMOUS_BAKERY", "World-famous bakery", "1000000000", "Bake 1 billion cookies in one ascension."},
                {"COSMIC_BAKERY", "Cosmic bakery", "100000000000", "Bake 100 billion cookies in one ascension."},
                {"GALACTIC_BAKERY", "Galactic bakery", "1000000000000", "Bake 1 trillion cookies in one ascension."},
                {"UNIVERSAL_BAKERY", "Universal bakery", "100000000000000", "Bake 100 trillion cookies in one ascension."},
                {"TIMELESS_BAKERY", "Timeless bakery", "1000000000000000", "Bake 1 quadrillion cookies in one ascension."},
                {"INFINITE_BAKERY", "Infinite bakery", "100000000000000000", "Bake 100 quadrillion cookies in one ascension."},
                {"IMMORTAL_BAKERY", "Immortal bakery", "1000000000000000000", "Bake 1 quintillion cookies in one ascension."},
                {"DONT_STOP", "Don't stop me now", "100000000000000000000", "Bake 100 quintillion cookies in one ascension."},
                {"YOU_CAN_STOP", "You can stop now", "1000000000000000000000", "Bake 1 sextillion cookies in one ascension."},
                {"ALL_THE_WAY", "Cookies all the way down", "100000000000000000000000", "Bake 100 sextillion cookies in one ascension."},
                {"OVERDOSE", "Overdose", "1000000000000000000000000", "Bake 1 septillion cookies in one ascension."},
                {"HOW", "How?", "100000000000000000000000000", "Bake 100 septillion cookies in one ascension."},
                {"MILK_LAND", "The land of milk and cookies", "1000000000000000000000000000", "Bake 1 octillion cookies in one ascension."},
                {"CONTROLS_UNIVERSE", "He who controls the cookies", "100000000000000000000000000000", "Bake 100 octillion cookies in one ascension."},
                {"HOARDERS", "Tonight on Hoarders", "1000000000000000000000000000000", "Bake 1 nonillion cookies in one ascension."},
                {"EAT_ALL_THAT", "Are you gonna eat all that?", "100000000000000000000000000000000", "Bake 100 nonillion cookies in one ascension."},
                {"BIGGER_BAKERY", "We're gonna need a bigger bakery", "1000000000000000000000000000000000", "Bake 1 decillion cookies in one ascension."},
                {"MOUTH_OF_MADNESS", "In the mouth of madness", "100000000000000000000000000000000000", "Bake 100 decillion cookies in one ascension."},
                {"LETTER_C", "Brought to you by the letter", "1000000000000000000000000000000000000", "Bake 1 undecillion cookies in one ascension."},
                {"DREAMS_BAKING", "The dreams in which I'm baking", "100000000000000000000000000000000000000", "Bake 100 undecillion cookies in one ascension."},
                {"SET_FOR_LIFE", "Set for life", "1000000000000000000000000000000000000000", "Bake 1 duodecillion cookies in one ascension."},
        };
        for (String[] r : rows) {
            n(r[0], r[1], r[3], Material.COOKIE, AchievementType.BAKED_THIS_ASCENSION, r[2], null, 0);
        }
    }

    private static void cps() {
        String[][] rows = {
                {"CASUAL_BAKING", "Casual baking", "1"},
                {"HARDCORE_BAKING", "Hardcore baking", "10"},
                {"STEADY_STREAM", "Steady tasty stream", "100"},
                {"COOKIE_MONSTER", "Cookie monster", "1000"},
                {"MASS_PRODUCER", "Mass producer", "10000"},
                {"COOKIE_VORTEX", "Cookie vortex", "1000000"},
                {"COOKIE_PULSAR", "Cookie pulsar", "10000000"},
                {"COOKIE_QUASAR", "Cookie quasar", "100000000"},
                {"OH_HEY", "Oh hey, you're still here", "1000000000"},
                {"NEVER_BAKE_AGAIN", "Let's never bake again", "10000000000"},
                {"WORLD_FILLED", "A world filled with cookies", "1000000000000"},
                {"THIRTY_SIX_Q", "When this baby hits 36 quadrillion", "10000000000000"},
                {"FAST_DELICIOUS", "Fast and delicious", "100000000000000"},
                {"COOKIEHERTZ", "Cookiehertz", "1000000000000000"},
                {"WORLD_HUNGER", "Woops, you solved world hunger", "10000000000000000"},
                {"TURBOPUNS", "Turbopuns", "1000000000000000000"},
                {"FASTER_MENNER", "Faster menner", "10000000000000000000"},
                {"STILL_HUNGRY", "And yet you're still hungry", "100000000000000000000"},
                {"ABAKENING", "The Abakening", "1000000000000000000000"},
                {"FAST", "Fast", "1000000000000000000000000"},
                {"KNEAD_FOR_SPEED", "Knead for speed", "10000000000000000000000000"},
        };
        for (String[] r : rows) {
            n(r[0], r[1], "Bake " + r[2] + " cookies per second.", Material.HOPPER,
                    AchievementType.RAW_CPS, r[2], null, 0);
        }
    }

    private static void clicking() {
        String[][] rows = {
                {"CLICKTASTIC", "Clicktastic", "1000"},
                {"CLICKATHLON", "Clickathlon", "100000"},
                {"CLICKOLYMPICS", "Clickolympics", "10000000"},
                {"CLICKORAMA", "Clickorama", "1000000000"},
                {"CLICKASMIC", "Clickasmic", "100000000000"},
                {"CLICKAGEDDON", "Clickageddon", "10000000000000"},
                {"CLICKNAROK", "Clicknarok", "1000000000000000"},
                {"CLICKASTROPHE", "Clickastrophe", "100000000000000000"},
                {"CLICKATACLYSM", "Clickataclysm", "10000000000000000000"},
                {"ULTIMATE_CLICKDOWN", "The ultimate clickdown", "1000000000000000000000"},
        };
        for (String[] r : rows) {
            n(r[0], r[1], "Make " + r[2] + " cookies from clicking.", Material.WOODEN_SWORD,
                    AchievementType.COOKIES_FROM_CLICKS, r[2], null, 0);
        }
    }

    private static void buildings() {
        int[] counts = {1, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700};
        n("CLICK", "Click", "Have 1 autoclicker.", Material.ARROW, AchievementType.BUILDING_COUNT, "1", UpgradeType.CURSOR, 0);
        n("DOUBLE_CLICK", "Double-click", "Have 2 autoclickers.", Material.ARROW, AchievementType.BUILDING_COUNT, "2", UpgradeType.CURSOR, 0);
        for (int c : counts) {
            if (c == 1) continue;
            n("CURSOR_" + c, cursorName(c), "Have " + c + " autoclickers.", Material.ARROW,
                    AchievementType.BUILDING_COUNT, String.valueOf(c), UpgradeType.CURSOR, 0);
        }
        n("CURSOR_800", "The devil's workshop", "Have 800 autoclickers.", Material.ARROW,
                AchievementType.BUILDING_COUNT, "800", UpgradeType.CURSOR, 0);
        n("CURSOR_900", "All on deck", "Have 900 autoclickers.", Material.ARROW,
                AchievementType.BUILDING_COUNT, "900", UpgradeType.CURSOR, 0);
        n("CURSOR_1000", "A round of applause", "Have 1,000 autoclickers.", Material.ARROW,
                AchievementType.BUILDING_COUNT, "1000", UpgradeType.CURSOR, 0);

        addBuildingLine(UpgradeType.GRANDMA, Material.BREAD, new String[]{
                "Grandma's cookies", "Sloppy kisses", "Retirement home", "Friend of the ancients",
                "Ruler of the ancients", "The old never bothered me anyway", "The agemaster", "To oldly go",
                "Aged well", "101st birthday", "But wait 'til you get older", "Defense of the ancients",
                "Okay boomer", "They moistly come at night", "And now you're even older"
        });
        addBuildingLine(UpgradeType.FARM, Material.WHEAT, new String[]{
                "Bought the farm", "Reap what you sow", "Farm ill", "Perfected agriculture", "Homegrown",
                "Gardener extraordinaire", "Seedy business", "You and the beanstalk", "Harvest moon",
                "Make like a tree", "Sharpest tool in the shed", "Overripe", "In the green",
                "It's grown on you", "Au naturel"
        });
        addBuildingLine(UpgradeType.MINE, Material.IRON_PICKAXE, new String[]{
                "You know the drill", "Excavation site", "Hollow the planet", "Can you dig it",
                "Center of the Earth", "Tectonic ambassador", "Freak fracking", "Romancing the stone",
                "Mine?", "Cave story", "Hey now, you're a rock", "Rock on",
                "Mountain out of a molehill", "Don't let the walls cave in", "Dirt-rich"
        });
        addBuildingLine(UpgradeType.FACTORY, Material.FURNACE, new String[]{
                "Production chain", "Industrial revolution", "Global warming", "Ultimate automation",
                "Technocracy", "Rise of the machines", "Modern times", "Ex machina", "In full gear",
                "In-cog-neato", "Break the mold", "Self-manmade man", "The wheels of progress",
                "Replaced by robots", "Bots build bots"
        });
        addGenericBuilding(UpgradeType.BANK, Material.GOLD_INGOT);
        addGenericBuilding(UpgradeType.TEMPLE, Material.ENCHANTING_TABLE);
        addGenericBuilding(UpgradeType.WIZARD_TOWER, Material.BLAZE_ROD);
        addGenericBuilding(UpgradeType.SHIPMENT, Material.CHEST_MINECART);
        addGenericBuilding(UpgradeType.ALCHEMY_LAB, Material.BREWING_STAND);
        addGenericBuilding(UpgradeType.PORTAL, Material.END_PORTAL_FRAME);
        addGenericBuilding(UpgradeType.TIME_MACHINE, Material.CLOCK);
        addGenericBuilding(UpgradeType.ANTIMATTER, Material.DRAGON_EGG);
        addGenericBuilding(UpgradeType.PRISM, Material.PRISMARINE_SHARD);
        addGenericBuilding(UpgradeType.CHANCEMAKER, Material.GOLDEN_APPLE);
        addGenericBuilding(UpgradeType.FRACTAL_ENGINE, Material.REPEATER);
        addGenericBuilding(UpgradeType.JAVASCRIPT_CONSOLE, Material.COMMAND_BLOCK);
        addGenericBuilding(UpgradeType.IDLEVERSE, Material.ENDER_PEARL);
        addGenericBuilding(UpgradeType.CORTEX_BAKER, Material.SCULK_CATALYST);
        addGenericBuilding(UpgradeType.YOU, Material.IRON_BLOCK);

        n("CLICK_DELEGATOR", "Click delegator", "Make 10 quintillion cookies just from autoclickers.",
                Material.ARROW, AchievementType.COOKIES_FROM_BUILDING, "10000000000000000000", UpgradeType.CURSOR, 0);
        n("GUSHING_GRANNIES", "Gushing grannies", "Make 10 quintillion cookies just from village bakers.",
                Material.BREAD, AchievementType.COOKIES_FROM_BUILDING, "10000000000000000000", UpgradeType.GRANDMA, 0);
        n("I_HATE_MANURE", "I hate manure", "Make 100 trillion cookies just from wheat farms.",
                Material.WHEAT, AchievementType.COOKIES_FROM_BUILDING, "100000000000000", UpgradeType.FARM, 0);
        n("NEVER_DIG_DOWN", "Never dig down", "Make 1 quadrillion cookies just from cookie mines.",
                Material.IRON_PICKAXE, AchievementType.COOKIES_FROM_BUILDING, "1000000000000000", UpgradeType.MINE, 0);
        n("INCREDIBLE_MACHINE", "The incredible machine", "Make 10 quadrillion cookies just from furnace factories.",
                Material.FURNACE, AchievementType.COOKIES_FROM_BUILDING, "10000000000000000", UpgradeType.FACTORY, 0);
    }

    private static String cursorName(int c) {
        return switch (c) {
            case 50 -> "Mouse wheel";
            case 100 -> "Of Mice and Men";
            case 200 -> "The Digital";
            case 300 -> "Extreme polydactyly";
            case 400 -> "Dr. T";
            case 500 -> "Thumbs, phalanges, metacarpals";
            case 550 -> "With her finger and her thumb";
            case 600 -> "Gotta hand it to you";
            case 650 -> "The devil's workshop";
            case 700 -> "All on deck";
            default -> c + " Autoclickers";
        };
    }

    private static void addBuildingLine(UpgradeType type, Material mat, String[] names) {
        int[] counts = {1, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700};
        for (int i = 0; i < counts.length && i < names.length; i++) {
            n(type.name() + "_" + counts[i], names[i], "Have " + counts[i] + " " + type.getDisplayName() + "(s).",
                    mat, AchievementType.BUILDING_COUNT, String.valueOf(counts[i]), type, 0);
        }
    }

    private static void addGenericBuilding(UpgradeType type, Material mat) {
        int[] counts = {1, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700};
        for (int c : counts) {
            n(type.name() + "_" + c, type.getDisplayName() + " " + c,
                    "Have " + c + " " + type.getDisplayName() + "(s).",
                    mat, AchievementType.BUILDING_COUNT, String.valueOf(c), type, 0);
        }
    }

    private static void totals() {
        n("BUILDER", "Builder", "Own 100 buildings.", Material.BRICKS, AchievementType.TOTAL_BUILDINGS, "100", null, 0);
        n("ARCHITECT", "Architect", "Own 500 buildings.", Material.BRICKS, AchievementType.TOTAL_BUILDINGS, "500", null, 0);
        n("ENGINEER", "Engineer", "Own 1,000 buildings.", Material.BRICKS, AchievementType.TOTAL_BUILDINGS, "1000", null, 0);
        n("LORD_CONSTRUCTS", "Lord of Constructs", "Own 2,500 buildings.", Material.BRICKS, AchievementType.TOTAL_BUILDINGS, "2500", null, 0);
        n("GRAND_DESIGN", "Grand design", "Own 5,000 buildings.", Material.BRICKS, AchievementType.TOTAL_BUILDINGS, "5000", null, 0);
        n("ECUMENOPOLIS", "Ecumenopolis", "Own 7,500 buildings.", Material.BRICKS, AchievementType.TOTAL_BUILDINGS, "7500", null, 0);
        n("MYRIAD", "Myriad", "Own 10,000 buildings.", Material.BRICKS, AchievementType.TOTAL_BUILDINGS, "10000", null, 0);

        n("ENHANCER", "Enhancer", "Purchase 20 upgrades.", Material.DIAMOND, AchievementType.UPGRADES_BOUGHT, "20", null, 0);
        n("AUGMENTER", "Augmenter", "Purchase 50 upgrades.", Material.DIAMOND, AchievementType.UPGRADES_BOUGHT, "50", null, 0);
        n("UPGRADER", "Upgrader", "Purchase 100 upgrades.", Material.DIAMOND, AchievementType.UPGRADES_BOUGHT, "100", null, 0);
        n("LORD_PROGRESS", "Lord of Progress", "Purchase 200 upgrades.", Material.DIAMOND, AchievementType.UPGRADES_BOUGHT, "200", null, 0);
        n("FULL_PICTURE", "The full picture", "Purchase 300 upgrades.", Material.DIAMOND, AchievementType.UPGRADES_BOUGHT, "300", null, 0);
    }

    private static void golden() {
        n("GOLDEN_COOKIE", "Golden cookie", "Click a golden cookie.", Material.GOLD_NUGGET, AchievementType.GOLDEN_CLICKED, "1", null, 0);
        n("LUCKY_COOKIE", "Lucky cookie", "Click 7 golden cookies.", Material.GOLD_NUGGET, AchievementType.GOLDEN_CLICKED, "7", null, 0);
        n("STROKE_OF_LUCK", "A stroke of luck", "Click 27 golden cookies.", Material.GOLD_INGOT, AchievementType.GOLDEN_CLICKED, "27", null, 0);
        n("FORTUNE", "Fortune", "Click 77 golden cookies.", Material.GOLD_INGOT, AchievementType.GOLDEN_CLICKED, "77", null, 0);
        n("LEPRECHAUN", "Leprechaun", "Click 777 golden cookies.", Material.GOLD_BLOCK, AchievementType.GOLDEN_CLICKED, "777", null, 0);
        n("BLACK_CAT", "Black cat's paw", "Click 7,777 golden cookies.", Material.GOLD_BLOCK, AchievementType.GOLDEN_CLICKED, "7777", null, 0);
        n("EARLY_BIRD", "Early bird", "Click a golden cookie less than 1 second after it spawns.",
                Material.CLOCK, AchievementType.GOLDEN_EARLY, "1", null, 0);
        n("FADING_LUCK", "Fading luck", "Click a golden cookie less than 1 second before it dies.",
                Material.CLOCK, AchievementType.GOLDEN_LATE, "1", null, 0);
    }

    private static void prestige() {
        n("PRESTIGE_1", "Ascension", "Prestige for the first time.", Material.BEACON, AchievementType.PRESTIGE, "1", null, 0);
        n("PRESTIGE_10", "Rebirth", "Reach prestige level 10.", Material.BEACON, AchievementType.PRESTIGE, "10", null, 0);
        n("PRESTIGE_100", "Reincarnation", "Reach prestige level 100.", Material.BEACON, AchievementType.PRESTIGE, "100", null, 0);
        n("PRESTIGE_1000", "Endless cycle", "Ascend 1,000 times.", Material.BEACON, AchievementType.PRESTIGE, "1000", null, 0);
    }

    private static void combos() {
        n("ONE_WITH_EVERYTHING", "One with everything", "Have at least 1 of every building.",
                Material.NETHER_STAR, AchievementType.ONE_OF_EVERYTHING, "1", null, 0);
        n("MATHEMATICIAN", "Mathematician", "Have at least 1 of the most expensive building, 2 of the next, 4 of the next...",
                Material.BOOK, AchievementType.MATHEMATICIAN, "1", null, 0);
        n("BASE_10", "Base 10", "Have at least 10 of the most expensive building, 20 of the next, 30 of the next...",
                Material.BOOK, AchievementType.BASE_10, "1", null, 0);
        int[] cents = {100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700};
        String[] centNames = {
                "Centennial", "Centennial and a half", "Bicentennial", "Bicentennial and a half",
                "Tricentennial", "Tricentennial and a half", "Quadricentennial", "Quadricentennial and a half",
                "Quincentennial", "Quincentennial and a half", "Sexcentennial", "Sexcentennial and a half",
                "Septcentennial"
        };
        for (int i = 0; i < cents.length; i++) {
            n("CENT_" + cents[i], centNames[i], "Have at least " + cents[i] + " of everything.",
                    Material.CAKE, AchievementType.CENTENNIAL, String.valueOf(cents[i]), null, cents[i]);
        }
    }

    private static void shadows() {
        shadow("HARDCORE", "Hardcore", "Get to 1 billion cookies baked with no upgrades purchased.",
                Material.BARRIER, AchievementType.HARDCORE, "1000000000");
        shadow("NEVERCLICK", "Neverclick", "Make 1 million cookies by only having clicked 15 times.",
                Material.BARRIER, AchievementType.NEVERCLICK, "1000000");
        shadow("TRUE_NEVERCLICK", "True Neverclick", "Make 1 million cookies with no cookie clicks.",
                Material.BARRIER, AchievementType.TRUE_NEVERCLICK, "1000000");
        shadow("SPEED_BAKING_I", "Speed baking I", "Get to 1 million cookies baked in 35 minutes.",
                Material.CLOCK, AchievementType.SPEED_BAKING, "2100");
        shadow("SPEED_BAKING_II", "Speed baking II", "Get to 1 million cookies baked in 25 minutes.",
                Material.CLOCK, AchievementType.SPEED_BAKING, "1500");
        shadow("SPEED_BAKING_III", "Speed baking III", "Get to 1 million cookies baked in 15 minutes.",
                Material.CLOCK, AchievementType.SPEED_BAKING, "900");
    }
}
