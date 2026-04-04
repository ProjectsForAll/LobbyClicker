package gg.drak.lobbyclicker.gui.admin;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.config.MainConfig;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.GuiHelper;
import gg.drak.lobbyclicker.gui.MenuText;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.SimpleGuiMonitor;
import gg.drak.lobbyclicker.redis.RedisManager;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

/**
 * Admin control panel GUI.
 */
public class AdminMainGui extends SimpleGuiMonitor {

    public AdminMainGui(Player player) {
        super(player, "admin-main", MonitorStyle.title("dark_red", "Admin Panel"), MonitorStyle.ROWS_FULL);
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        fillMonitorBorder();

        addItem(4, GuiHelper.createIcon(Material.COMMAND_BLOCK,
                MonitorStyle.title("dark_red", "LobbyClicker Admin"),
                "",
                "<gray>" + MenuText.esc("Manage the clicker plugin") + "</gray>",
                MenuText.grayWhite("Loaded players: ", String.valueOf(PlayerManager.getLoadedPlayers().size()))));

        // Row 1: Player management
        Icon managePlayers = GuiHelper.createIcon(Material.PLAYER_HEAD,
                MenuText.title("gold", "Manage Players"),
                "", "<gray>" + MenuText.esc("Browse all players") + "</gray>",
                "", "<yellow>" + MenuText.esc("Click to open") + "</yellow>");
        managePlayers.onClick(e -> new AdminPlayerListGui(player).open());
        setContent(0, managePlayers);

        // Row 2: Server actions
        setContent(7, makeAction(Material.CHEST, "green", "Force Save All",
                "Save all loaded player data", p -> {
                    int count = 0;
                    for (PlayerData data : PlayerManager.getLoadedPlayers()) {
                        if (data.isFullyLoaded()) { data.save(true); count++; }
                    }
                    p.sendMessage(ChatColor.GREEN + "Force-saved " + count + " player(s).");
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    new AdminMainGui(p).open();
                }));

        setContent(8, makeAction(Material.COMPARATOR, "yellow", "Reload Config",
                "Reload config & reconnect Redis", p -> {
                    LobbyClicker.setMainConfig(new MainConfig());
                    if (LobbyClicker.getRedisManager() != null) {
                        LobbyClicker.getRedisManager().shutdown();
                        LobbyClicker.setRedisManager(null);
                    }
                    if (LobbyClicker.getMainConfig().isRedisEnabled()) {
                        RedisManager rm = new RedisManager();
                        rm.connect();
                        LobbyClicker.setRedisManager(rm);
                        p.sendMessage(ChatColor.GREEN + "Config reloaded. Redis reconnected.");
                    } else {
                        p.sendMessage(ChatColor.GREEN + "Config reloaded. " + ChatColor.YELLOW + "Redis disabled.");
                    }
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    new AdminMainGui(p).open();
                }));

        // Row 3: Info
        setContent(14, GuiHelper.createIcon(Material.PAPER,
                "<white><bold>" + MenuText.esc("Quick Help") + "</bold></white>",
                "",
                "<yellow>/clickeradmin</yellow><gray>" + MenuText.esc(" - Open this GUI") + "</gray>",
                "<yellow>/clickeradmin set <player> <cookies></yellow>",
                "<yellow>/clickeradmin add <player> <cookies></yellow>",
                "<yellow>/clickeradmin reset <player></yellow>",
                "<yellow>/clickeradmin info <player></yellow>",
                "<yellow>/clickeradmin prestige <player> <set|add|remove> <amount></yellow>",
                "<yellow>/clickeradmin upgrade <player> <add|remove|set> <upgrade> <amount></yellow>",
                "<yellow>/clickeradmin profiles <player></yellow>",
                "<yellow>/clickeradmin openfor <player></yellow>",
                "<yellow>/clickeradmin save</yellow>",
                "<yellow>/clickeradmin reload</yellow>"));

        // Close button
        int b = (getSize() / 9 - 1) * 9;
        Icon close = GuiHelper.createIcon(Material.BARRIER, "<red>" + MenuText.esc("Close") + "</red>");
        close.onClick(e -> player.closeInventory());
        addItem(b + 8, close);
    }

    private Icon makeAction(Material mat, String namedColor, String label, String desc, java.util.function.Consumer<Player> onClick) {
        Icon icon = GuiHelper.createIcon(mat,
                "<" + namedColor + "><bold>" + MenuText.esc(label) + "</bold></" + namedColor + ">",
                "",
                "<gray>" + MenuText.esc(desc) + "</gray>",
                "",
                "<yellow>" + MenuText.esc("Click to execute") + "</yellow>");
        icon.onClick(e -> onClick.accept(player));
        return icon;
    }
}
