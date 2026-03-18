package gg.drak.lobbyclicker.gui.admin;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.config.MainConfig;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.GuiHelper;
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
 * Admin control panel GUI. Shows admin actions as a bordered monitor.
 * Red + black themed for admin distinction.
 */
public class AdminMainGui extends SimpleGuiMonitor {

    public AdminMainGui(Player player) {
        super(player, "admin-main", ChatColor.DARK_RED + "" + ChatColor.BOLD + "Admin Panel", MonitorStyle.ROWS_FULL);
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        fillMonitorBorder();

        // Title item at top center
        addItem(4, GuiHelper.createIcon(Material.COMMAND_BLOCK,
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "LobbyClicker Admin",
                "",
                ChatColor.GRAY + "Manage the clicker plugin",
                ChatColor.GRAY + "Loaded players: " + ChatColor.WHITE + PlayerManager.getLoadedPlayers().size()));

        // Row 1: Player management
        setContent(0, makeButton(Material.PLAYER_HEAD, ChatColor.GOLD, "Manage Player",
                "Set cookies, upgrades, etc.", "Use: /clickeradmin <action> <player>"));

        setContent(1, makeButton(Material.BOOK, ChatColor.YELLOW, "Manage Profiles",
                "View & delete player profiles", "Click, then select a player"));

        setContent(2, makeButton(Material.DIAMOND, ChatColor.AQUA, "Manage Upgrades",
                "Add/remove/set upgrades", "Use: /clickeradmin upgrade ..."));

        // Row 2: Server actions
        setContent(7, makeAction(Material.CHEST, ChatColor.GREEN, "Force Save All",
                "Save all loaded player data", p -> {
                    int count = 0;
                    for (PlayerData data : PlayerManager.getLoadedPlayers()) {
                        if (data.isFullyLoaded()) { data.save(true); count++; }
                    }
                    p.sendMessage(ChatColor.GREEN + "Force-saved " + count + " player(s).");
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    new AdminMainGui(p).open();
                }));

        setContent(8, makeAction(Material.COMPARATOR, ChatColor.YELLOW, "Reload Config",
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

        setContent(9, makeAction(Material.OAK_SIGN, ChatColor.AQUA, "Online Players",
                PlayerManager.getLoadedPlayers().size() + " loaded", p -> {
                    new AdminPlayerListGui(p).open();
                }));

        // Row 3: Info
        setContent(14, GuiHelper.createIcon(Material.PAPER,
                ChatColor.WHITE + "" + ChatColor.BOLD + "Quick Help",
                "",
                ChatColor.YELLOW + "/clickeradmin set <player> <cookies>",
                ChatColor.YELLOW + "/clickeradmin add <player> <cookies>",
                ChatColor.YELLOW + "/clickeradmin reset <player>",
                ChatColor.YELLOW + "/clickeradmin info <player>",
                ChatColor.YELLOW + "/clickeradmin upgrade <player> ...",
                ChatColor.YELLOW + "/clickeradmin profiles <player>",
                ChatColor.YELLOW + "/clickeradmin openfor <player>"));

        // Close button bottom-right
        int b = (getSize() / 9 - 1) * 9;
        Icon close = GuiHelper.createIcon(Material.BARRIER, ChatColor.RED + "Close");
        close.onClick(e -> player.closeInventory());
        addItem(b + 8, close);
    }

    private Icon makeButton(Material mat, ChatColor color, String label, String... desc) {
        String[] lore = new String[desc.length + 2];
        lore[0] = "";
        for (int i = 0; i < desc.length; i++) lore[i + 1] = ChatColor.GRAY + desc[i];
        lore[lore.length - 1] = ChatColor.YELLOW + "Use command for this action";
        return GuiHelper.createIcon(mat, color + "" + ChatColor.BOLD + label, lore);
    }

    private Icon makeAction(Material mat, ChatColor color, String label, String desc, java.util.function.Consumer<Player> onClick) {
        Icon icon = GuiHelper.createIcon(mat, color + "" + ChatColor.BOLD + label,
                "", ChatColor.GRAY + desc, "", ChatColor.YELLOW + "Click to execute");
        icon.onClick(e -> onClick.accept(player));
        return icon;
    }
}
