package gg.drak.lobbyclicker.gui;

import lombok.Getter;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Creates banner items displaying a single character with configurable colors.
 *
 * Usage:
 *   BannerChar.of("K", BannerColor.RED, BannerColor.WHITE).toStack()
 *   BannerChar.of("0", BannerColor.BLACK, BannerColor.WHITE).toStack()
 *
 * Patterns sourced from gamergeeks.net banner generators.
 * "A" = text color (the character drawn ON the banner)
 * "B" = base color (the banner background, also used for erase/mask patterns)
 */
public class BannerChar {

    private final String character;
    private final BannerColor baseColor;
    private final BannerColor textColor;

    private BannerChar(String character, BannerColor baseColor, BannerColor textColor) {
        this.character = character;
        this.baseColor = baseColor;
        this.textColor = textColor;
    }

    /**
     * Create a BannerChar for the given character with specified colors.
     * Returns a blank banner (base color only) for unsupported characters.
     */
    public static BannerChar of(String ch, BannerColor baseColor, BannerColor textColor) {
        return new BannerChar(ch, baseColor, textColor);
    }

    public ItemStack toStack() {
        Material bannerMat = baseColor.getBannerMaterial();
        ItemStack banner = new ItemStack(bannerMat);

        if (character == null || character.isEmpty() || character.equals(" ")) return banner;

        // Special chars that need a different base
        CharDef def = getCharDef(character.toUpperCase());
        if (def == null) return banner; // unsupported char -> blank banner

        // If the char def specifies "inverted base" (base=text, patterns use base to erase)
        if (def.invertedBase) {
            banner = new ItemStack(textColor.getBannerMaterial());
        }

        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        if (meta == null) return banner;

        DyeColor text = textColor.getDyeColor();
        DyeColor base = baseColor.getDyeColor();

        for (PatternStep step : def.steps) {
            // 'A' = text color, 'B' = base color
            DyeColor color = step.colorKey == 'A' ? text : base;
            if (def.invertedBase) {
                // When inverted, A = base (erase), B = text... actually
                // In the gamergeeks data: base-0 means WHITE base, then patterns use 14 (red) and 0 (white)
                // For inverted chars (Q, S, H), the base is the TEXT color and patterns erase with BASE
                color = step.colorKey == 'A' ? base : text;
            }
            meta.addPattern(new Pattern(color, step.type));
        }

        banner.setItemMeta(meta);
        return banner;
    }

    public Icon toIcon(String loreLine) {
        ItemStack stack = toStack();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + (character != null ? character.trim() : ""));
            if (loreLine != null) meta.setLore(Collections.singletonList(loreLine));
            stack.setItemMeta(meta);
        }
        return new Icon(stack);
    }

    // === Color enum ===

    @Getter
    public enum BannerColor {
        WHITE(DyeColor.WHITE, Material.WHITE_BANNER),
        BLACK(DyeColor.BLACK, Material.BLACK_BANNER),
        RED(DyeColor.RED, Material.RED_BANNER),
        BLUE(DyeColor.BLUE, Material.BLUE_BANNER),
        GREEN(DyeColor.GREEN, Material.GREEN_BANNER),
        YELLOW(DyeColor.YELLOW, Material.YELLOW_BANNER),
        ORANGE(DyeColor.ORANGE, Material.ORANGE_BANNER),
        PURPLE(DyeColor.PURPLE, Material.PURPLE_BANNER),
        PINK(DyeColor.PINK, Material.PINK_BANNER),
        LIME(DyeColor.LIME, Material.LIME_BANNER),
        CYAN(DyeColor.CYAN, Material.CYAN_BANNER),
        LIGHT_BLUE(DyeColor.LIGHT_BLUE, Material.LIGHT_BLUE_BANNER),
        LIGHT_GRAY(DyeColor.LIGHT_GRAY, Material.LIGHT_GRAY_BANNER),
        GRAY(DyeColor.GRAY, Material.GRAY_BANNER),
        MAGENTA(DyeColor.MAGENTA, Material.MAGENTA_BANNER),
        BROWN(DyeColor.BROWN, Material.BROWN_BANNER);

        private final DyeColor dyeColor;
        private final Material bannerMaterial;

        BannerColor(DyeColor dyeColor, Material bannerMaterial) {
            this.dyeColor = dyeColor;
            this.bannerMaterial = bannerMaterial;
        }
    }

    // === Pattern definitions ===

    private static class PatternStep {
        final char colorKey; // 'A' = text, 'B' = base
        final PatternType type;
        PatternStep(char colorKey, PatternType type) {
            this.colorKey = colorKey;
            this.type = type;
        }
    }

    private static class CharDef {
        final PatternStep[] steps;
        final boolean invertedBase; // true if base should be text color (for Q, S, H)
        CharDef(boolean invertedBase, PatternStep... steps) {
            this.invertedBase = invertedBase;
            this.steps = steps;
        }
    }

    private static PatternStep a(PatternType t) { return new PatternStep('A', t); }
    private static PatternStep b(PatternType t) { return new PatternStep('B', t); }

    /**
     * Get the pattern definition for a character.
     * Patterns from gamergeeks.net, where A=text color, B=base color.
     */
    private static CharDef getCharDef(String ch) {
        switch (ch) {
            // === NUMBERS (from gamergeeks numbers page) ===
            case "0": return new CharDef(false, a(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_LEFT), a(PatternType.STRIPE_TOP), a(PatternType.STRIPE_RIGHT), a(PatternType.STRIPE_DOWNLEFT), b(PatternType.BORDER));
            case "1": return new CharDef(false, a(PatternType.STRIPE_CENTER), a(PatternType.SQUARE_TOP_LEFT), b(PatternType.CURLY_BORDER), a(PatternType.STRIPE_BOTTOM), b(PatternType.BORDER));
            case "2": return new CharDef(false, a(PatternType.STRIPE_TOP), b(PatternType.RHOMBUS), a(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_DOWNLEFT), b(PatternType.BORDER));
            case "3": return new CharDef(false, a(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_MIDDLE), a(PatternType.STRIPE_TOP), b(PatternType.CURLY_BORDER), a(PatternType.STRIPE_RIGHT), b(PatternType.BORDER));
            case "4": return new CharDef(false, a(PatternType.STRIPE_LEFT), b(PatternType.HALF_HORIZONTAL_BOTTOM), a(PatternType.STRIPE_RIGHT), a(PatternType.STRIPE_MIDDLE), b(PatternType.BORDER));
            case "5": return new CharDef(false, a(PatternType.STRIPE_BOTTOM), b(PatternType.RHOMBUS), a(PatternType.STRIPE_TOP), a(PatternType.STRIPE_DOWNRIGHT), b(PatternType.BORDER));
            case "6": return new CharDef(false, a(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_RIGHT), b(PatternType.HALF_HORIZONTAL), a(PatternType.STRIPE_MIDDLE), a(PatternType.STRIPE_TOP), a(PatternType.STRIPE_LEFT), b(PatternType.BORDER));
            case "7": return new CharDef(false, a(PatternType.STRIPE_DOWNLEFT), a(PatternType.STRIPE_TOP), b(PatternType.BORDER));
            case "8": return new CharDef(false, a(PatternType.STRIPE_TOP), a(PatternType.STRIPE_LEFT), a(PatternType.STRIPE_MIDDLE), a(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_RIGHT), b(PatternType.BORDER));
            case "9": return new CharDef(false, a(PatternType.STRIPE_LEFT), b(PatternType.HALF_HORIZONTAL_BOTTOM), a(PatternType.STRIPE_MIDDLE), a(PatternType.STRIPE_TOP), a(PatternType.STRIPE_RIGHT), a(PatternType.STRIPE_BOTTOM), b(PatternType.BORDER));

            // === LETTERS (from gamergeeks letters page) ===
            case "A": return new CharDef(false, a(PatternType.STRIPE_RIGHT), a(PatternType.STRIPE_LEFT), a(PatternType.STRIPE_MIDDLE), a(PatternType.STRIPE_TOP), b(PatternType.BORDER));
            case "B": return new CharDef(false, a(PatternType.STRIPE_RIGHT), a(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_TOP), b(PatternType.CURLY_BORDER), a(PatternType.STRIPE_LEFT), a(PatternType.STRIPE_MIDDLE), b(PatternType.BORDER));
            case "C": return new CharDef(false, a(PatternType.STRIPE_TOP), a(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_RIGHT), b(PatternType.STRIPE_MIDDLE), a(PatternType.STRIPE_LEFT), b(PatternType.BORDER));
            case "D": return new CharDef(false, a(PatternType.STRIPE_RIGHT), a(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_TOP), b(PatternType.CURLY_BORDER), a(PatternType.STRIPE_LEFT), b(PatternType.BORDER));
            case "E": return new CharDef(false, a(PatternType.STRIPE_LEFT), a(PatternType.STRIPE_TOP), a(PatternType.STRIPE_MIDDLE), a(PatternType.STRIPE_BOTTOM), b(PatternType.BORDER));
            case "F": return new CharDef(false, a(PatternType.STRIPE_MIDDLE), b(PatternType.STRIPE_RIGHT), a(PatternType.STRIPE_TOP), a(PatternType.STRIPE_LEFT), b(PatternType.BORDER));
            case "G": return new CharDef(false, a(PatternType.STRIPE_RIGHT), b(PatternType.HALF_HORIZONTAL), a(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_LEFT), a(PatternType.STRIPE_TOP), b(PatternType.BORDER));
            case "H": return new CharDef(true, b(PatternType.STRIPE_TOP), b(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_LEFT), a(PatternType.STRIPE_RIGHT), b(PatternType.BORDER));
            case "I": return new CharDef(false, a(PatternType.STRIPE_CENTER), a(PatternType.STRIPE_TOP), a(PatternType.STRIPE_BOTTOM), b(PatternType.BORDER));
            case "J": return new CharDef(false, a(PatternType.STRIPE_LEFT), b(PatternType.HALF_HORIZONTAL), a(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_RIGHT), b(PatternType.BORDER));
            case "K": return new CharDef(false, a(PatternType.STRIPE_DOWNRIGHT), b(PatternType.HALF_HORIZONTAL), a(PatternType.STRIPE_DOWNLEFT), a(PatternType.STRIPE_LEFT), b(PatternType.BORDER));
            case "L": return new CharDef(false, a(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_LEFT), b(PatternType.BORDER));
            case "M": return new CharDef(false, a(PatternType.TRIANGLE_TOP), b(PatternType.TRIANGLES_TOP), a(PatternType.STRIPE_LEFT), a(PatternType.STRIPE_RIGHT), b(PatternType.BORDER));
            case "N": return new CharDef(false, a(PatternType.STRIPE_LEFT), b(PatternType.TRIANGLE_TOP), a(PatternType.STRIPE_DOWNRIGHT), a(PatternType.STRIPE_RIGHT), b(PatternType.BORDER));
            case "O": return new CharDef(false, a(PatternType.STRIPE_LEFT), a(PatternType.STRIPE_RIGHT), a(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_TOP), b(PatternType.BORDER));
            case "P": return new CharDef(false, a(PatternType.STRIPE_RIGHT), b(PatternType.HALF_HORIZONTAL_BOTTOM), a(PatternType.STRIPE_MIDDLE), a(PatternType.STRIPE_TOP), a(PatternType.STRIPE_LEFT), b(PatternType.BORDER));
            case "Q": return new CharDef(true, b(PatternType.RHOMBUS), a(PatternType.STRIPE_RIGHT), a(PatternType.STRIPE_LEFT), a(PatternType.SQUARE_BOTTOM_RIGHT), b(PatternType.BORDER));
            case "R": return new CharDef(false, a(PatternType.HALF_HORIZONTAL), b(PatternType.STRIPE_CENTER), a(PatternType.STRIPE_TOP), a(PatternType.STRIPE_LEFT), a(PatternType.STRIPE_DOWNRIGHT), b(PatternType.BORDER));
            case "S": return new CharDef(true, b(PatternType.RHOMBUS), b(PatternType.STRIPE_MIDDLE), a(PatternType.STRIPE_DOWNRIGHT), b(PatternType.BORDER));
            case "T": return new CharDef(false, a(PatternType.STRIPE_TOP), a(PatternType.STRIPE_CENTER), b(PatternType.BORDER));
            case "U": return new CharDef(false, a(PatternType.STRIPE_BOTTOM), a(PatternType.STRIPE_LEFT), a(PatternType.STRIPE_RIGHT), b(PatternType.BORDER));
            case "V": return new CharDef(false, a(PatternType.STRIPE_DOWNLEFT), a(PatternType.STRIPE_LEFT), b(PatternType.TRIANGLE_BOTTOM), a(PatternType.STRIPE_DOWNLEFT), b(PatternType.BORDER));
            case "W": return new CharDef(false, a(PatternType.TRIANGLE_BOTTOM), b(PatternType.TRIANGLES_BOTTOM), a(PatternType.STRIPE_LEFT), a(PatternType.STRIPE_RIGHT), b(PatternType.BORDER));
            case "X": return new CharDef(false, a(PatternType.CROSS), b(PatternType.BORDER));
            case "Y": return new CharDef(false, a(PatternType.STRIPE_DOWNRIGHT), b(PatternType.HALF_HORIZONTAL_BOTTOM), a(PatternType.STRIPE_DOWNLEFT), b(PatternType.BORDER));
            case "Z": return new CharDef(false, a(PatternType.STRIPE_TOP), a(PatternType.STRIPE_DOWNLEFT), a(PatternType.STRIPE_BOTTOM), b(PatternType.BORDER));

            // === SYMBOLS ===
            case ".": return new CharDef(false, a(PatternType.SQUARE_BOTTOM_LEFT), b(PatternType.BORDER));
            case "+": return new CharDef(false, a(PatternType.STRIPE_CENTER), a(PatternType.STRIPE_MIDDLE), b(PatternType.BORDER));
            case "<": return new CharDef(false, a(PatternType.STRIPE_LEFT), a(PatternType.STRIPE_MIDDLE), b(PatternType.STRIPE_TOP), b(PatternType.STRIPE_BOTTOM), b(PatternType.CURLY_BORDER));
            case ">": return new CharDef(false, a(PatternType.STRIPE_RIGHT), a(PatternType.STRIPE_MIDDLE), b(PatternType.STRIPE_TOP), b(PatternType.STRIPE_BOTTOM), b(PatternType.CURLY_BORDER));

            default: return null;
        }
    }
}
