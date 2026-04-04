package gg.drak.lobbyclicker.golden;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum CookieType {
    NULL(null, ""),
    SMALL(Material.GOLD_NUGGET, "Lucky Cookie"),
    MEDIUM(Material.GOLD_INGOT, "Golden Cookie"),
    GRAND(Material.GOLD_BLOCK, "Jackpot"),
    LEGENDARY(Material.ENCHANTED_GOLDEN_APPLE, "Legendary Cookie");

    private final Material material;
    private final String displayName;

    CookieType(Material material, String displayName) {
        this.material = material;
        this.displayName = displayName;
    }
}
