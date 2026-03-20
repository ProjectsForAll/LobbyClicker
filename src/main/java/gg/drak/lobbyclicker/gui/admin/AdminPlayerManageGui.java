package gg.drak.lobbyclicker.gui.admin;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.ClickerGui;
import gg.drak.lobbyclicker.gui.GuiHelper;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.SimpleGuiMonitor;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import gg.drak.lobbyclicker.utils.ChatInput;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Admin GUI for managing a specific player. All admin actions available via clickable icons.
 */
public class AdminPlayerManageGui extends SimpleGuiMonitor {
    private final String targetUuid;
    private final String targetName;

    public AdminPlayerManageGui(Player player, String targetUuid, String targetName) {
        super(player, "admin-player-manage",
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "Manage: " + targetName,
                MonitorStyle.ROWS_FULL);
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        Player player = (Player) event.getPlayer();
        fillMonitorBorder();

        int b = (getSize() / 9 - 1) * 9;
        Icon back = GuiHelper.createIcon(Material.DARK_OAK_DOOR,
                ChatColor.RED + "" + ChatColor.BOLD + "Back", "", ChatColor.GRAY + "Back to player list");
        back.onClick(e -> new AdminPlayerListGui(player).open());
        addItem(b + 7, back);
        Icon close = GuiHelper.createIcon(Material.BARRIER, ChatColor.RED + "Close");
        close.onClick(e -> player.closeInventory());
        addItem(b + 8, close);

        // Load player data
        PlayerData data = getTargetData();
        if (data == null) {
            setContent(10, GuiHelper.createIcon(Material.BARRIER,
                    ChatColor.RED + "Could not load player data"));
            return;
        }

        // Title with info
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "UUID: " + ChatColor.DARK_GRAY + targetUuid);
        infoLore.add(ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(data.getCookies()));
        infoLore.add(ChatColor.GRAY + "Total Earned: " + ChatColor.WHITE + FormatUtils.format(data.getLifetimeCookiesEarned()));
        infoLore.add(ChatColor.GRAY + "CPS: " + ChatColor.WHITE + FormatUtils.format(data.getCps()));
        infoLore.add(ChatColor.GRAY + "CPC: " + ChatColor.WHITE + FormatUtils.format(data.getCpc()));
        infoLore.add(ChatColor.GRAY + "Prestige: " + ChatColor.WHITE + data.getPrestigeLevel());
        infoLore.add(ChatColor.GRAY + "Aura: " + ChatColor.WHITE + FormatUtils.format(data.getAura()));
        infoLore.add(ChatColor.GRAY + "Entropy: " + ChatColor.WHITE + FormatUtils.format(data.getClickerEntropy()));
        for (UpgradeType type : UpgradeType.values()) {
            int count = data.getUpgradeCount(type);
            if (count > 0) {
                infoLore.add(ChatColor.GRAY + "  " + type.getDisplayName() + ": " + ChatColor.WHITE + count);
            }
        }
        addItem(4, GuiHelper.createIcon(Material.NETHER_STAR,
                ChatColor.GOLD + "" + ChatColor.BOLD + targetName,
                infoLore.toArray(new String[0])));

        // Row 1: Cookie management
        // Set Cookies
        Icon setCookies = GuiHelper.createIcon(Material.GOLD_INGOT,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Set Cookies",
                "", ChatColor.GRAY + "Current: " + ChatColor.WHITE + FormatUtils.format(data.getCookies()),
                "", ChatColor.YELLOW + "Click to set amount");
        setCookies.onClick(e -> promptAmount(player, "Set cookies to:", amount -> {
            PlayerData d = getTargetData();
            if (d == null) return;
            BigDecimal diff = amount.subtract(d.getCookies());
            d.setCookies(amount);
            if (diff.signum() > 0) d.setTotalCookiesEarned(d.getTotalCookiesEarned().add(diff));
            d.save(true);
            player.sendMessage(ChatColor.GREEN + "Set " + targetName + "'s cookies to " + FormatUtils.format(amount));
            new AdminPlayerManageGui(player, targetUuid, targetName).open();
        }));
        setContent(0, setCookies);

        // Add Cookies
        Icon addCookies = GuiHelper.createIcon(Material.GOLD_NUGGET,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Add Cookies",
                "", ChatColor.YELLOW + "Click to add amount");
        addCookies.onClick(e -> promptAmount(player, "Add how many cookies?", amount -> {
            PlayerData d = getTargetData();
            if (d == null) return;
            d.addCookies(amount);
            d.save(true);
            player.sendMessage(ChatColor.GREEN + "Added " + FormatUtils.format(amount) + " cookies to " + targetName);
            new AdminPlayerManageGui(player, targetUuid, targetName).open();
        }));
        setContent(1, addCookies);

        // Set Prestige
        Icon prestige = GuiHelper.createIcon(Material.BEACON,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Set Prestige",
                "", ChatColor.GRAY + "Current: " + ChatColor.WHITE + data.getPrestigeLevel(),
                "", ChatColor.YELLOW + "Click to set level");
        prestige.onClick(e -> promptInt(player, "Set prestige level to:", level -> {
            PlayerData d = getTargetData();
            if (d == null) return;
            d.setPrestigeLevel(Math.max(0, level));
            d.save(true);
            player.sendMessage(ChatColor.GREEN + "Set " + targetName + "'s prestige to " + level);
            new AdminPlayerManageGui(player, targetUuid, targetName).open();
        }));
        setContent(2, prestige);

        // Set Aura
        Icon aura = GuiHelper.createIcon(Material.EXPERIENCE_BOTTLE,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Set Aura",
                "", ChatColor.GRAY + "Current: " + ChatColor.WHITE + FormatUtils.format(data.getAura()),
                "", ChatColor.YELLOW + "Click to set amount");
        aura.onClick(e -> promptAmount(player, "Set aura to:", amount -> {
            PlayerData d = getTargetData();
            if (d == null) return;
            d.setAura(amount);
            d.save(true);
            player.sendMessage(ChatColor.GREEN + "Set " + targetName + "'s aura to " + FormatUtils.format(amount));
            new AdminPlayerManageGui(player, targetUuid, targetName).open();
        }));
        setContent(3, aura);

        // Row 2: More actions
        // Reset
        Icon reset = GuiHelper.createIcon(Material.FLINT_AND_STEEL,
                ChatColor.RED + "" + ChatColor.BOLD + "Reset Player",
                "", ChatColor.GRAY + "Resets cookies, upgrades, prestige",
                "", ChatColor.RED + "Shift-click to confirm");
        reset.onClick(e -> {
            if (!e.isShiftClick()) {
                player.sendMessage(ChatColor.RED + "Shift-click to confirm reset!");
                return;
            }
            PlayerData d = getTargetData();
            if (d == null) return;
            d.setCookies(BigDecimal.ZERO);
            d.setTotalCookiesEarned(BigDecimal.ZERO);
            for (UpgradeType type : UpgradeType.values()) d.setUpgradeCount(type, 0);
            d.getPurchasedUpgrades().clear();
            d.save(true);
            player.sendMessage(ChatColor.GREEN + "Reset all data for " + targetName);
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1f);
            new AdminPlayerManageGui(player, targetUuid, targetName).open();
        });
        setContent(7, reset);

        // Open For
        boolean targetOnline = Bukkit.getPlayer(UUID.fromString(targetUuid)) != null;
        Icon openFor = GuiHelper.createIcon(
                targetOnline ? Material.COOKIE : Material.GRAY_DYE,
                (targetOnline ? ChatColor.GOLD : ChatColor.DARK_GRAY) + "" + ChatColor.BOLD + "Open Clicker For",
                "", targetOnline ? ChatColor.YELLOW + "Opens their clicker GUI" : ChatColor.RED + "Player is offline");
        if (targetOnline) {
            openFor.onClick(e -> {
                Player target = Bukkit.getPlayer(UUID.fromString(targetUuid));
                if (target != null) {
                    PlayerData d = getTargetData();
                    if (d != null) new ClickerGui(target, d).open();
                    player.sendMessage(ChatColor.GREEN + "Opened clicker GUI for " + targetName);
                }
            });
        }
        setContent(8, openFor);

        // Profiles
        Icon profiles = GuiHelper.createIcon(Material.BOOK,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Manage Profiles",
                "", ChatColor.GRAY + "View and manage profiles",
                "", ChatColor.YELLOW + "Click to open");
        profiles.onClick(e -> new AdminProfilesGui(player, targetUuid, targetName).open());
        setContent(9, profiles);

        // Manage Upgrades
        Icon upgrades = GuiHelper.createIcon(Material.DIAMOND,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Manage Upgrades",
                "", ChatColor.GRAY + "Click to open upgrade manager");
        upgrades.onClick(e -> new AdminUpgradeManageGui(player, targetUuid, targetName).open());
        setContent(10, upgrades);
    }

    private PlayerData getTargetData() {
        PlayerData data = PlayerManager.getPlayer(targetUuid).orElse(null);
        if (data == null || !data.isFullyLoaded()) {
            java.util.Optional<PlayerData> opt = PlayerManager.getOrGetPlayer(targetUuid);
            if (opt.isPresent()) data = opt.get().waitUntilFullyLoaded();
        }
        return data;
    }

    private void promptAmount(Player player, String prompt, java.util.function.Consumer<BigDecimal> callback) {
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + prompt + " (e.g. 2.5m, 1000, 5k). Type 'cancel' to cancel.");
        ChatInput.request(player, input -> {
            if (input == null || input.equalsIgnoreCase("cancel")) {
                new AdminPlayerManageGui(player, targetUuid, targetName).open();
                return;
            }
            BigDecimal parsed = FormatUtils.parseShorthand(input);
            if (parsed == null) {
                player.sendMessage(ChatColor.RED + "Invalid amount: " + input);
                new AdminPlayerManageGui(player, targetUuid, targetName).open();
                return;
            }
            callback.accept(parsed);
        });
    }

    private void promptInt(Player player, String prompt, java.util.function.Consumer<Integer> callback) {
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + prompt + " Type 'cancel' to cancel.");
        ChatInput.request(player, input -> {
            if (input == null || input.equalsIgnoreCase("cancel")) {
                new AdminPlayerManageGui(player, targetUuid, targetName).open();
                return;
            }
            try {
                callback.accept(Integer.parseInt(input.trim()));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid number: " + input);
                new AdminPlayerManageGui(player, targetUuid, targetName).open();
            }
        });
    }
}
