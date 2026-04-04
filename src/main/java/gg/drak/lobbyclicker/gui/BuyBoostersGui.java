package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.boosters.BoosterManager;
import gg.drak.lobbyclicker.boosters.BoosterType;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class BuyBoostersGui extends PaginationMonitor {
    private final PlayerData data;

    public BuyBoostersGui(Player player, PlayerData data, int page) {
        super(player, "buy-boosters", MonitorStyle.title("green", "Buy Boosters"), page);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new BoostersMenuGui(p, data).open());

        List<BoosterType> boosters = Arrays.asList(BoosterType.values());

        populatePage(boosters, type -> {
            boolean onCooldown = BoosterManager.isOnCooldown(data.getIdentifier(), type);
            boolean canAfford = data.getCookies().compareTo(type.getCost(data.getPrestigeLevel())) >= 0;

            Material displayMat;
            if (onCooldown) {
                displayMat = Material.GRAY_DYE;
            } else if (!canAfford) {
                displayMat = Material.RED_DYE;
            } else {
                displayMat = type.getMaterial();
            }

            String statusLine;
            if (onCooldown) {
                long remaining = BoosterManager.getCooldownRemaining(data.getIdentifier(), type);
                statusLine = ChatColor.GRAY + "Cooldown: " + ChatColor.WHITE + formatTime(remaining);
            } else if (!canAfford) {
                statusLine = ChatColor.RED + "Can't afford!";
            } else {
                statusLine = ChatColor.GREEN + "Click to buy!";
            }

            Icon icon = GuiHelper.createIcon(displayMat,
                    ChatColor.GOLD + "" + ChatColor.BOLD + type.getDisplayName(),
                    "",
                    ChatColor.GRAY + type.getDescription(),
                    "",
                    ChatColor.GRAY + "Cost: " + ChatColor.WHITE + FormatUtils.format(type.getCost(data.getPrestigeLevel())),
                    ChatColor.GRAY + "Duration: " + ChatColor.WHITE + formatTime(type.getDurationSeconds() * 1000L),
                    ChatColor.GRAY + "Cooldown: " + ChatColor.WHITE + formatTime(type.getCooldownSeconds() * 1000L),
                    ChatColor.GRAY + "Effect: " + ChatColor.WHITE + type.getEffectValue() + "x " + type.getEffect().name().toLowerCase().replace("_", " "),
                    "",
                    statusLine);

            icon.onClick(e -> {
                if (BoosterManager.isOnCooldown(data.getIdentifier(), type)) {
                    player.sendMessage(ChatColor.RED + "That booster is on cooldown!");
                    return;
                }
                if (data.getCookies().compareTo(type.getCost(data.getPrestigeLevel())) < 0) {
                    player.sendMessage(ChatColor.RED + "You can't afford that booster!");
                    return;
                }
                data.removeCookies(type.getCost(data.getPrestigeLevel()));
                BoosterManager.addBooster(data.getIdentifier(), type);
                player.sendMessage(ChatColor.GREEN + "Purchased " + ChatColor.GOLD + type.getDisplayName()
                        + ChatColor.GREEN + " for " + ChatColor.WHITE + FormatUtils.format(type.getCost(data.getPrestigeLevel())) + ChatColor.GREEN + " cookies!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                // Refresh
                new BuyBoostersGui(player, data, page).open();
            });

            return icon;
        });

        addPaginationArrows(boosters, newPage -> new BuyBoostersGui(player, data, newPage).open());
    }

    private static String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        if (totalSeconds < 60) return totalSeconds + "s";
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (minutes < 60) return minutes + "m " + seconds + "s";
        long hours = minutes / 60;
        minutes = minutes % 60;
        return hours + "h " + minutes + "m";
    }
}
