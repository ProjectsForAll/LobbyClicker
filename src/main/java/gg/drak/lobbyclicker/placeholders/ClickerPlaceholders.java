package gg.drak.lobbyclicker.placeholders;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.utils.FormatUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClickerPlaceholders extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "lobbyclicker";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Drak";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        PlayerData data = PlayerManager.getPlayer(player.getUniqueId().toString()).orElse(null);
        if (data == null || !data.isFullyLoaded()) return "...";

        switch (params.toLowerCase()) {
            case "cookies":
                return FormatUtils.format(data.getCookies());
            case "cookies_raw":
                return String.valueOf((long) data.getCookies());
            case "total_cookies":
                return FormatUtils.format(data.getTotalCookiesEarned());
            case "total_cookies_raw":
                return String.valueOf((long) data.getTotalCookiesEarned());
            case "cps":
                return FormatUtils.format(data.getCps());
            case "cpc":
                return FormatUtils.format(data.getCpc());
            case "entropy":
                return FormatUtils.format(data.getClickerEntropy());
            case "entropy_raw":
                return String.valueOf(data.getClickerEntropy());
            case "clicks":
                return FormatUtils.format(data.getTimesClicked());
            case "clicks_raw":
                return String.valueOf(data.getTimesClicked());
            default:
                return null;
        }
    }
}
