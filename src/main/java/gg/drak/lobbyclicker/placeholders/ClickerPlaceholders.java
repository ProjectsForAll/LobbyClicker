package gg.drak.lobbyclicker.placeholders;

import gg.drak.lobbyclicker.LobbyClicker;
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
        // Server-level placeholders (no player needed)
        switch (params.toLowerCase()) {
            case "server_id":
                return LobbyClicker.getMainConfig().getServerId();
            case "server_prettyname":
                return LobbyClicker.getMainConfig().getServerPrettyName();
        }

        if (player == null) return "";

        PlayerData data = PlayerManager.getPlayer(player.getUniqueId().toString()).orElse(null);
        if (data == null || !data.isFullyLoaded()) return "...";

        switch (params.toLowerCase()) {
            case "cookies":
                return FormatUtils.format(data.getCookies());
            case "cookies_raw":
                return data.getCookies().toPlainString();
            case "total_cookies":
                return FormatUtils.format(data.getTotalCookiesEarned());
            case "total_cookies_raw":
                return data.getTotalCookiesEarned().toPlainString();
            case "cps":
                return FormatUtils.format(data.getCps());
            case "cpc":
                return FormatUtils.format(data.getCpc());
            case "entropy":
                return FormatUtils.format(data.getClickerEntropy());
            case "entropy_raw":
                return data.getClickerEntropy().toPlainString();
            case "clicks":
            case "realm_clicks":
                return FormatUtils.format(data.getTimesClicked());
            case "clicks_raw":
            case "realm_clicks_raw":
                return String.valueOf(data.getTimesClicked());
            case "realm_owner_clicks":
                return FormatUtils.format(data.getOwnerClicks());
            case "realm_owner_clicks_raw":
                return String.valueOf(data.getOwnerClicks());
            case "realm_other_clicks":
                return FormatUtils.format(data.getOtherClicks());
            case "realm_other_clicks_raw":
                return String.valueOf(data.getOtherClicks());
            case "global_clicks":
                return FormatUtils.format(data.getGlobalClicks());
            case "global_clicks_raw":
                return String.valueOf(data.getGlobalClicks());
            case "friends":
                return String.valueOf(data.getFriends().size());
            case "realm_public":
                return data.isRealmPublic() ? "Public" : "Private";
            case "prestige":
                return String.valueOf(data.getPrestigeLevel());
            case "aura":
                return FormatUtils.format(data.getAura());
            case "aura_raw":
                return data.getAura().toPlainString();
            default:
                return null;
        }
    }
}
