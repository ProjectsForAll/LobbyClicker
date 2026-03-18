package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UpgradeGui extends PaginationMonitor {
    private final PlayerData viewerData;
    private final PlayerData ownerData;

    private static final ConcurrentHashMap<UUID, UpgradeGui> OPEN_GUIS = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<UUID, UpgradeGui> getOpenGuis() { return OPEN_GUIS; }
    public static void registerGui(UUID uuid, UpgradeGui gui) { OPEN_GUIS.put(uuid, gui); }
    public static void unregisterGui(UUID uuid) { OPEN_GUIS.remove(uuid); }

    public UpgradeGui(Player player, PlayerData data) {
        this(player, data, data);
    }

    public UpgradeGui(Player player, PlayerData viewerData, PlayerData ownerData) {
        super(player, "clicker-upgrades", MonitorStyle.title(ChatColor.GREEN, "Cookie Helpers"), 0);
        this.viewerData = viewerData;
        this.ownerData = ownerData;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        buildDisplay();
        registerGui(player.getUniqueId(), this);
    }

    public void refreshDisplay() {
        if (player == null || !player.isOnline()) return;
        buildDisplay();
    }

    private boolean isOwnerRemote() {
        return !ownerData.getIdentifier().equals(viewerData.getIdentifier())
                && !ownerData.isOnline()
                && LobbyClicker.getRedisManager() != null
                && LobbyClicker.getRedisManager().isPlayerOnlineRemotely(ownerData.getIdentifier());
    }

    private void buildDisplay() {
        setPlayerContext(viewerData, ownerData);
        fillMonitorBorder();
        buildStandardActionBar(p -> {
            unregisterGui(p.getUniqueId());
            new ShopGui(p, viewerData, ownerData).open();
        });

        // Cookie info at top
        addItem(4, GuiHelper.createIcon(Material.COOKIE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Realm Cookies",
                "",
                ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCookies()),
                ChatColor.GRAY + "CPS: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCps()),
                ChatColor.GRAY + "CPC: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCpc())));

        // Layout: blank, blank, Click Power, blank, blank, then Cursor through Prism in order
        List<UpgradeType> layout = new ArrayList<>();
        layout.add(null); // blank
        layout.add(null); // blank
        layout.add(UpgradeType.CLICK_POWER);
        layout.add(null); // blank
        layout.add(null); // blank
        layout.add(UpgradeType.CURSOR);
        layout.add(UpgradeType.GRANDMA);
        layout.add(UpgradeType.FARM);
        layout.add(UpgradeType.MINE);
        layout.add(UpgradeType.FACTORY);
        layout.add(UpgradeType.BANK);
        layout.add(UpgradeType.TEMPLE);
        layout.add(UpgradeType.WIZARD_TOWER);
        layout.add(UpgradeType.SHIPMENT);
        layout.add(UpgradeType.ALCHEMY_LAB);
        layout.add(UpgradeType.PORTAL);
        layout.add(UpgradeType.TIME_MACHINE);
        layout.add(UpgradeType.ANTIMATTER);
        layout.add(UpgradeType.PRISM);

        populatePagedContent(layout, (type, slot) -> {
            if (type != null) {
                addItem(slot, createUpgradeIcon(type));
            }
            // null entries stay as the monitor border/background
        });

        addPaginationArrows(layout, newPage -> {});
    }

    private Icon createUpgradeIcon(UpgradeType type) {
        int owned = ownerData.getUpgradeCount(type);
        BigDecimal cost = type.getCost(owned);
        boolean canAfford = ownerData.canAfford(cost);

        boolean revealed = !type.isHidden() || owned > 0 || canAfford
                || ownerData.getCookies().compareTo(type.getBaseCost()) >= 0
                || ownerData.getTotalCookiesEarned().compareTo(type.getBaseCost()) >= 0;

        if (!revealed) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.MAGIC + "??????????????????");
            lore.add("");
            lore.add(ChatColor.GRAY + "Owned: " + ChatColor.WHITE + owned);
            lore.add(ChatColor.GRAY + "CPS each: " + ChatColor.WHITE + ChatColor.MAGIC + "??????");
            lore.add("");
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.RED + ChatColor.MAGIC + "??????" + ChatColor.RESET + ChatColor.RED + " cookies");
            lore.add("");
            lore.add(ChatColor.RED + "Earn more cookies to reveal!");

            Icon icon = GuiHelper.createIcon(type.getMaterial(),
                    ChatColor.RED + "" + ChatColor.MAGIC + "??????");
            icon.onClick(e -> {
                if (!e.isLeftClick()) return;
                player.sendMessage(ChatColor.RED + "You haven't unlocked this upgrade yet!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            });
            return icon;
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + type.getDescription());
        lore.add("");
        lore.add(ChatColor.GRAY + "Owned: " + ChatColor.WHITE + owned);

        if (type.getCpsPerLevel().signum() > 0) {
            lore.add(ChatColor.GRAY + "CPS each: " + ChatColor.WHITE + "+" + FormatUtils.format(type.getCpsPerLevel()));
            lore.add(ChatColor.GRAY + "Total CPS: " + ChatColor.WHITE + "+" + FormatUtils.format(type.getCpsPerLevel().multiply(BigDecimal.valueOf(owned))));
        }
        if (type.getCpcPerLevel().signum() > 0) {
            lore.add(ChatColor.GRAY + "CPC each: " + ChatColor.WHITE + "+" + FormatUtils.format(type.getCpcPerLevel()));
            lore.add(ChatColor.GRAY + "Total CPC: " + ChatColor.WHITE + "+" + FormatUtils.format(type.getCpcPerLevel().multiply(BigDecimal.valueOf(owned))));
        }

        lore.add("");
        lore.add(ChatColor.GRAY + "Cost: " + (canAfford ? ChatColor.GREEN : ChatColor.RED) + FormatUtils.format(cost) + " cookies");
        lore.add("");
        lore.add(canAfford ? ChatColor.YELLOW + "Click to buy!" : ChatColor.RED + "Not enough cookies!");

        String color = canAfford ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
        Icon icon = GuiHelper.createIcon(type.getMaterial(), color + ChatColor.BOLD + type.getDisplayName(),
                lore.toArray(new String[0]));

        icon.onClick(e -> {
            if (!e.isLeftClick()) return;
            if (isOwnerRemote()) {
                RedisSyncHandler.publishBuyUpgrade(ownerData.getIdentifier(), viewerData.getIdentifier(), type.name());
                if (viewerData.getSettings().isSoundEnabled(gg.drak.lobbyclicker.settings.SettingType.SOUND_BUY)) {
                    float vol = viewerData.getSettings().getVolume(gg.drak.lobbyclicker.settings.SettingType.VOLUME_BUY);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, vol, 1.5f);
                }
            } else {
                if (ownerData.buyUpgrade(type)) {
                    if (viewerData.getSettings().isSoundEnabled(gg.drak.lobbyclicker.settings.SettingType.SOUND_BUY)) {
                        float vol = viewerData.getSettings().getVolume(gg.drak.lobbyclicker.settings.SettingType.VOLUME_BUY);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, vol, 1.5f);
                    }
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                }
                refreshDisplay();
            }
        });

        return icon;
    }
}
