package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Banner display utility. Creates red banners with white characters for
 * use as digit/letter displays in GUIs.
 *
 * All digit patterns are exact replicas of tested in-game banners.
 * Base: RED_BANNER. Characters: WHITE dye. Border cleanup: RED dye.
 */
public class BannerUtil {

    private static final DyeColor W = DyeColor.WHITE; // character color
    private static final DyeColor R = DyeColor.RED;   // base / erase color

    /**
     * Creates a red banner displaying a single character in white.
     * Supports digits 0-9, letters K M B T Q S E, and symbols + .
     * Unknown or blank (" ") returns a plain red banner.
     */
    public static ItemStack createCharBanner(String ch) {
        ItemStack banner = new ItemStack(Material.RED_BANNER);
        if (ch == null || ch.isEmpty() || ch.equals(" ")) return banner;

        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        if (meta == null) return banner;

        switch (ch) {
            case "0":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_BOTTOM));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_LEFT));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_TOP));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_RIGHT));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_DOWNLEFT));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            case "1":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_CENTER));
                meta.addPattern(new Pattern(W, PatternType.SQUARE_TOP_LEFT));
                meta.addPattern(new Pattern(R, PatternType.CURLY_BORDER));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_BOTTOM));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            case "2":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_TOP));
                meta.addPattern(new Pattern(R, PatternType.RHOMBUS));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_BOTTOM));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_DOWNLEFT));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            case "3":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_BOTTOM));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_MIDDLE));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_TOP));
                meta.addPattern(new Pattern(R, PatternType.CURLY_BORDER));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_RIGHT));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            case "4":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_LEFT));
                meta.addPattern(new Pattern(R, PatternType.HALF_HORIZONTAL_BOTTOM));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_RIGHT));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_MIDDLE));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            case "5":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_BOTTOM));
                meta.addPattern(new Pattern(R, PatternType.RHOMBUS));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_TOP));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_DOWNRIGHT));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            case "6":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_BOTTOM));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_RIGHT));
                meta.addPattern(new Pattern(R, PatternType.HALF_HORIZONTAL));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_MIDDLE));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_TOP));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_LEFT));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            case "7":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_DOWNLEFT));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_TOP));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            case "8":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_TOP));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_LEFT));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_MIDDLE));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_BOTTOM));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_RIGHT));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            case "9":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_LEFT));
                meta.addPattern(new Pattern(R, PatternType.HALF_HORIZONTAL_BOTTOM));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_TOP));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_RIGHT));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_MIDDLE));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;

            // === LETTERS (from gamergeeks.net/apps/minecraft/banners/letters) ===
            // B: red base -> white rs, bs, ts, red cbo, white ls, ms, red bo
            case "B":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_RIGHT));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_BOTTOM));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_TOP));
                meta.addPattern(new Pattern(R, PatternType.CURLY_BORDER));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_LEFT));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_MIDDLE));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            // E: red base -> white ls, ts, ms, bs, red bo
            case "e":
            case "E":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_LEFT));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_TOP));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_MIDDLE));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_BOTTOM));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            // K: red base -> white drs, red hh, white dls, ls, red bo
            case "K":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_DOWNRIGHT));
                meta.addPattern(new Pattern(R, PatternType.HALF_HORIZONTAL));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_DOWNLEFT));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_LEFT));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            // M: red base -> white tt, red tts, white ls, rs, red bo
            case "M":
                meta.addPattern(new Pattern(W, PatternType.TRIANGLE_TOP));
                meta.addPattern(new Pattern(R, PatternType.TRIANGLES_TOP));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_LEFT));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_RIGHT));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            // T: red base -> white ts, cs, red bo
            case "T":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_TOP));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_CENTER));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            // Q: WHITE base -> red mr, white rs, ls, br, red bo
            case "Q": {
                // Q uses a white base, not red
                banner = new ItemStack(Material.WHITE_BANNER);
                meta = (BannerMeta) banner.getItemMeta();
                if (meta == null) return banner;
                meta.addPattern(new Pattern(R, PatternType.RHOMBUS));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_RIGHT));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_LEFT));
                meta.addPattern(new Pattern(W, PatternType.SQUARE_BOTTOM_RIGHT));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            }
            // S: WHITE base -> red mr, red ms, white drs, red bo
            case "S": {
                // S uses a white base, not red
                banner = new ItemStack(Material.WHITE_BANNER);
                meta = (BannerMeta) banner.getItemMeta();
                if (meta == null) return banner;
                meta.addPattern(new Pattern(R, PatternType.RHOMBUS));
                meta.addPattern(new Pattern(R, PatternType.STRIPE_MIDDLE));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_DOWNRIGHT));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            }

            // === SYMBOLS ===
            case ".":
                meta.addPattern(new Pattern(W, PatternType.SQUARE_BOTTOM_LEFT));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;
            case "+":
                meta.addPattern(new Pattern(W, PatternType.STRIPE_CENTER));
                meta.addPattern(new Pattern(W, PatternType.STRIPE_MIDDLE));
                meta.addPattern(new Pattern(R, PatternType.BORDER));
                break;

            default:
                // Unknown - plain red banner
                break;
        }

        banner.setItemMeta(meta);
        return banner;
    }

    /**
     * Creates a banner Icon for a character with a display name and lore line.
     */
    public static Icon charBannerIcon(String ch, String loreLine) {
        ItemStack banner = createCharBanner(ch);
        ItemMeta meta = banner.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + (ch != null ? ch.trim() : ""));
            if (loreLine != null) meta.setLore(Arrays.asList(loreLine));
            banner.setItemMeta(meta);
        }
        return new Icon(banner);
    }

    /**
     * Parses a formatted value string into 4 display characters for banners.
     * Returns String[4], blank slots are " ".
     *
     * Examples:
     *   "15"    -> [" ", " ", "1", "5"]
     *   "203"   -> [" ", "2", "0", "3"]
     *   "2.03K" -> ["2", ".", "0", "K"]
     *   "25.0M" -> ["2", "5", ".", "M"]
     */
    public static String[] parseBannerDisplay(BigDecimal value) {
        return parseBannerDisplay(FormatUtils.format(value));
    }

    public static String[] parseBannerDisplay(String formatted) {
        int suffixStart = -1;
        for (int i = 0; i < formatted.length(); i++) {
            char c = formatted.charAt(i);
            if (c != '.' && (c < '0' || c > '9')) {
                suffixStart = i;
                break;
            }
        }

        String numPart;
        String suffixPart;
        if (suffixStart >= 0) {
            numPart = formatted.substring(0, suffixStart);
            suffixPart = formatted.substring(suffixStart);
        } else {
            numPart = formatted;
            suffixPart = "";
        }

        String[] display = new String[4];

        if (suffixPart.isEmpty()) {
            while (numPart.length() < 4) numPart = " " + numPart;
            String shown = numPart.length() > 4 ? numPart.substring(0, 4) : numPart;
            for (int i = 0; i < 4; i++) {
                display[i] = String.valueOf(shown.charAt(i));
            }
        } else {
            if (numPart.length() > 3) numPart = numPart.substring(0, 3);
            while (numPart.length() < 3) numPart = " " + numPart;
            for (int i = 0; i < 3; i++) {
                display[i] = String.valueOf(numPart.charAt(i));
            }
            display[3] = suffixPart.length() > 0 ? suffixPart.substring(0, 1) : " ";
        }

        return display;
    }
}
