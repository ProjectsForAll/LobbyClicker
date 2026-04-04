package gg.drak.lobbyclicker.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * MiniMessage parsing, Unicode small caps on all visible text, and legacy serialization for
 * inventory titles and Spigot ItemMeta (§ strings).
 */
public final class MenuText {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private static final String[] SMALL_CAPS = {
            "\u1D00", "\u0299", "\u1D04", "\u1D05", "\u1D07", "\uA730", "\u0262", "\u029C", "\u026A", "\u1D0A",
            "\u1D0B", "\u029F", "\u1D0D", "\u0274", "\u1D0F", "\u1D18", "\u01EB", "\u0280", "\uA731", "\u1D1B",
            "\u1D1C", "\u1D20", "\u1D21", "x", "\u028F", "\u1D22"
    };

    private MenuText() {}

    public static Component mm(String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) {
            return Component.empty();
        }
        return MM.deserialize(miniMessage);
    }

    /**
     * Applies Unicode small caps to every {@link TextComponent} content segment (style preserved).
     */
    public static Component withSmallCaps(Component component) {
        List<Component> newChildren = new ArrayList<>();
        for (Component child : component.children()) {
            newChildren.add(withSmallCaps(child));
        }
        if (component instanceof TextComponent tc) {
            Component base = Component.text(smallCaps(tc.content()), tc.style());
            if (newChildren.isEmpty()) {
                return base;
            }
            return base.children(newChildren);
        }
        if (newChildren.isEmpty()) {
            return component;
        }
        return component.children(newChildren);
    }

    /**
     * Inventory title / any string that must become legacy § for BukkitOfUtils.
     * MiniMessage 4.25+ rejects § in {@link MiniMessage#deserialize}; legacy inputs are parsed with
     * {@link LegacyComponentSerializer} instead.
     */
    public static String legacySection(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        // Fast path: ChatColor-built lines skip Adventure parse/serialize (hot in upgrade GUIs).
        if (text.indexOf('\u00A7') >= 0) {
            return smallCaps(text);
        }
        return LEGACY_SECTION.serialize(withSmallCaps(mm(text)));
    }

    /**
     * One line for item name/lore: MiniMessage → small caps → legacy §, or legacy § → small caps (fast scan).
     */
    public static String itemLine(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        if (text.indexOf('\u00A7') >= 0) {
            return smallCaps(text);
        }
        return legacySection(text);
    }

    public static String esc(String plain) {
        return MM.escapeTags(plain == null ? "" : plain);
    }

    public static String sc(String plain) {
        return esc(smallCaps(plain == null ? "" : plain));
    }

    public static String smallCaps(String input) {
        if (input == null || input.isEmpty()) {
            return input == null ? "" : input;
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); ) {
            char c = input.charAt(i);
            if (c == '\u00A7' && i + 1 < input.length()) {
                out.append(c).append(input.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c >= 'A' && c <= 'Z') {
                out.append(SMALL_CAPS[c - 'A']);
            } else if (c >= 'a' && c <= 'z') {
                out.append(SMALL_CAPS[c - 'a']);
            } else {
                out.append(c);
            }
            i++;
        }
        return out.toString();
    }

    public static String title(String plain) {
        return "<gold><bold>" + esc(plain) + "</bold></gold>";
    }

    public static String title(String namedColor, String plain) {
        return "<" + namedColor + "><bold>" + esc(plain) + "</bold></" + namedColor + ">";
    }

    /** Lore segment: gray label + white value (small caps applied when rendered via {@link #itemLine} / {@link #legacySection}). */
    public static String grayWhite(String grayLabel, String whiteValue) {
        return "<gray>" + esc(grayLabel) + "</gray><white>" + esc(whiteValue == null ? "" : whiteValue) + "</white>";
    }

    public static void hideVanillaTooltips(ItemMeta meta) {
        meta.addItemFlags(ItemFlag.values());
    }
}
