package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.achievements.AchievementManager;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.realm.RealmProfile;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
        super(player, "clicker-upgrades", MonitorStyle.title("green", "Cookie Helpers"), 0);
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

        addItem(4, ClickerGuiHelper.createIcon(Material.COOKIE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Realm Cookies",
                "",
                ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCookies()),
                ChatColor.GRAY + "CPS: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCps()),
                ChatColor.GRAY + "CPC: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCpc())));

        List<UpgradeType> layout = Arrays.asList(UpgradeType.values());
        populatePagedContent(layout, (type, slot) -> addItem(slot, createUpgradeIcon(type)));
        addPaginationArrows(layout, newPage -> { this.page = newPage; refreshDisplay(); });
    }

    private Icon createUpgradeIcon(UpgradeType type) {
        int owned = ownerData.getUpgradeCount(type);
        BigDecimal cost = type.getCost(owned);
        boolean canAfford = ownerData.canAfford(cost);
        RealmProfile profile = ownerData.getActiveProfile();

        List<String> lore = new java.util.ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + type.getDescription());
        lore.add("");
        lore.add(ChatColor.GRAY + "owned: " + ChatColor.WHITE + owned);

        if (profile != null && type.getCpsPerLevel().signum() > 0) {
            // "base" is the catalog rate; "actual" is that rate after every multiplier the
            // profile carries — building and synergy upgrades, prestige, aura, milk — so the
            // two numbers show what the helper is worth before and after the realm's bonuses.
            BigDecimal global = profile.getGlobalCpsMultiplier();
            BigDecimal baseEach = type.getCpsPerLevel();
            BigDecimal actualEach = profile.getBuildingRawCpsEach(type).multiply(global);
            BigDecimal count = BigDecimal.valueOf(owned);
            BigDecimal baseTotal = baseEach.multiply(count);
            BigDecimal actualTotal = profile.getBuildingRawCps(type).multiply(global);

            lore.add(ChatColor.GRAY + "each: " + ChatColor.WHITE + "+" + FormatUtils.formatRate(baseEach)
                    + " " + ChatColor.AQUA + "(+" + FormatUtils.formatRate(actualEach) + ")"
                    + " " + ChatColor.GOLD + "cps");
            lore.add(ChatColor.GRAY + "total: " + ChatColor.WHITE + "+" + FormatUtils.formatRate(baseTotal)
                    + " " + ChatColor.AQUA + "(+" + FormatUtils.formatRate(actualTotal) + ")"
                    + " " + ChatColor.GOLD + "cps");
        }

        lore.add("");
        lore.add(ChatColor.GRAY + "cost: " + (canAfford ? ChatColor.GREEN : ChatColor.RED) + FormatUtils.format(cost) + " cookies");
        lore.add("");
        lore.add(canAfford ? ChatColor.YELLOW + "click to buy!" : ChatColor.RED + "not enough cookies!");

        String nameColor = canAfford ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
        Icon icon = ClickerGuiHelper.createIcon(type.getMaterial(), nameColor + ChatColor.BOLD + type.getDisplayName(),
                lore.toArray(new String[0]));

        icon.onClick(e -> {
            if (!e.isLeftClick()) return;
            if (isOwnerRemote()) {
                RedisSyncHandler.publishBuyUpgrade(ownerData.getIdentifier(), viewerData.getIdentifier(), type.name());
            } else if (ownerData.buyUpgrade(type)) {
                AchievementManager.check(ownerData);
                if (viewerData.getSettings().isSoundEnabled(gg.drak.lobbyclicker.settings.SettingType.SOUND_BUY)) {
                    float vol = viewerData.getSettings().getVolume(gg.drak.lobbyclicker.settings.SettingType.VOLUME_BUY);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, vol, 1.5f);
                }
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            }
            refreshDisplay();
        });
        return icon;
    }
}
