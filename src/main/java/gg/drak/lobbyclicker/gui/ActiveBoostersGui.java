package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.boosters.ActiveBooster;
import gg.drak.lobbyclicker.boosters.BoosterManager;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.List;

public class ActiveBoostersGui extends PaginationMonitor {
    private final PlayerData data;

    public ActiveBoostersGui(Player player, PlayerData data, int page) {
        super(player, "active-boosters", MonitorStyle.title(ChatColor.AQUA, "Active Boosters"), page);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new BoostersMenuGui(p, data).open());

        List<ActiveBooster> boosters = BoosterManager.getActiveBoosters(data.getIdentifier());

        if (boosters.isEmpty()) {
            Icon empty = GuiHelper.createIcon(Material.BARRIER,
                    ChatColor.GRAY + "" + ChatColor.BOLD + "No Active Boosters",
                    "", ChatColor.GRAY + "Buy boosters from the shop!");
            addItem(22, empty);
        } else {
            populatePage(boosters, booster -> {
                String statusText;
                Material displayMat;
                if (booster.isPaused()) {
                    statusText = ChatColor.YELLOW + "PAUSED";
                    displayMat = Material.ORANGE_DYE;
                } else {
                    statusText = ChatColor.GREEN + "ACTIVE";
                    displayMat = booster.getType().getMaterial();
                }

                long remainingMs = booster.getRemainingMs();

                Icon icon = GuiHelper.createIcon(displayMat,
                        ChatColor.GOLD + "" + ChatColor.BOLD + booster.getType().getDisplayName(),
                        "",
                        ChatColor.GRAY + "Status: " + statusText,
                        ChatColor.GRAY + "Time remaining: " + ChatColor.WHITE + formatTime(remainingMs),
                        ChatColor.GRAY + "Effect: " + ChatColor.WHITE + booster.getType().getEffectValue() + "x "
                                + booster.getType().getEffect().name().toLowerCase().replace("_", " "),
                        "",
                        booster.isPaused()
                                ? ChatColor.GREEN + "Click to unpause"
                                : ChatColor.YELLOW + "Click to pause");

                icon.onClick(e -> {
                    if (booster.isPaused()) {
                        booster.unpause();
                    } else {
                        booster.pause();
                    }
                    // Refresh
                    new ActiveBoostersGui(player, data, page).open();
                });

                return icon;
            });
        }

        addPaginationArrows(boosters, newPage -> new ActiveBoostersGui(player, data, newPage).open());
    }

    private static String formatTime(long ms) {
        long totalSeconds = Math.max(0, ms / 1000);
        if (totalSeconds < 60) return totalSeconds + "s";
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (minutes < 60) return minutes + "m " + seconds + "s";
        long hours = minutes / 60;
        minutes = minutes % 60;
        return hours + "h " + minutes + "m";
    }
}
