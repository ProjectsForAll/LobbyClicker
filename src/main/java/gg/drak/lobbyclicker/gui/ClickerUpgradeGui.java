package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.achievements.AchievementManager;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.upgrades.ClickerUpgrade;
import gg.drak.lobbyclicker.upgrades.ClickerUpgradeCatalog;
import gg.drak.lobbyclicker.upgrades.ClickerUpgradeEffect;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClickerUpgradeGui extends PaginationMonitor {
    private final PlayerData viewerData;
    private final PlayerData ownerData;
    private static final ConcurrentHashMap<UUID, ClickerUpgradeGui> OPEN_GUIS = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<UUID, ClickerUpgradeGui> getOpenGuis() { return OPEN_GUIS; }
    public static void registerGui(UUID uuid, ClickerUpgradeGui gui) { OPEN_GUIS.put(uuid, gui); }
    public static void unregisterGui(UUID uuid) { OPEN_GUIS.remove(uuid); }

    public ClickerUpgradeGui(Player player, PlayerData viewerData, PlayerData ownerData) {
        super(player, "clicker-upgrades-store", MonitorStyle.title("aqua", "Upgrades"), 0);
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

    private void buildDisplay() {
        setPlayerContext(viewerData, ownerData);
        fillMonitorBorder();
        buildStandardActionBar(p -> {
            unregisterGui(p.getUniqueId());
            new ShopGui(p, viewerData, ownerData).open();
        });

        int purchased = ownerData.getPurchasedUpgrades().size();
        int total = ClickerUpgradeCatalog.all().size();
        addItem(4, ClickerGuiHelper.createIcon(Material.COOKIE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Upgrades",
                "",
                ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCookies()),
                ChatColor.GRAY + "Purchased: " + ChatColor.WHITE + purchased + "/" + total,
                "",
                ChatColor.GRAY + "CPC Bonus: " + ChatColor.WHITE + "×" + FormatUtils.format(ownerData.getEffectMultiplier(ClickerUpgradeEffect.CPC_MULTIPLIER)),
                ChatColor.GRAY + "CPS Bonus: " + ChatColor.WHITE + "×" + FormatUtils.format(ownerData.getEffectMultiplier(ClickerUpgradeEffect.CPS_MULTIPLIER))));

        List<ClickerUpgrade> sorted = new ArrayList<>(ClickerUpgradeCatalog.all());
        Set<ClickerUpgrade> owned = ownerData.getPurchasedUpgrades();
        sorted.removeIf(u -> !owned.contains(u) && u.isHidden(ownerData.getActiveProfile()));
        sorted.sort((a, b) -> {
            int g = Integer.compare(getDisplayGroup(a, owned), getDisplayGroup(b, owned));
            return g != 0 ? g : a.getCost().compareTo(b.getCost());
        });

        populatePagedContent(sorted, (upgrade, slot) -> addItem(slot, createUpgradeIcon(upgrade)));
        addPaginationArrows(sorted, newPage -> {
            this.page = newPage;
            buildDisplay();
        });
    }

    private int getDisplayGroup(ClickerUpgrade u, Set<ClickerUpgrade> owned) {
        if (owned.contains(u)) return 3;
        if (!u.isUnlocked(ownerData.getActiveProfile())) return 2;
        if (ownerData.canAfford(u.getCost())) return 0;
        return 1;
    }

    private Icon createUpgradeIcon(ClickerUpgrade upgrade) {
        boolean purchased = ownerData.hasPurchasedUpgrade(upgrade);
        boolean unlocked = upgrade.isUnlocked(ownerData.getActiveProfile());
        boolean canAfford = !purchased && unlocked && ownerData.canAfford(upgrade.getCost());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + upgrade.getDescription());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Effect: " + ChatColor.WHITE + describeEffect(upgrade));
        if (!unlocked && !purchased) {
            lore.add("");
            lore.add(ChatColor.RED + "Requires: " + ChatColor.WHITE + describeRequirement(upgrade));
        }
        lore.add("");
        if (purchased) {
            lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "PURCHASED");
        } else {
            lore.add(ChatColor.GRAY + "Cost: " + (canAfford ? ChatColor.GREEN : ChatColor.RED)
                    + FormatUtils.format(upgrade.getCost()) + " cookies");
        }

        String nameColor = purchased ? ChatColor.GOLD.toString()
                : canAfford ? ChatColor.GREEN.toString()
                : unlocked ? ChatColor.RED.toString() : ChatColor.DARK_GRAY.toString();
        Material mat = unlocked || purchased ? upgrade.getMaterial() : Material.GRAY_DYE;
        Icon icon = ClickerGuiHelper.createIcon(mat, nameColor + ChatColor.BOLD + upgrade.getDisplayName(),
                lore.toArray(new String[0]));

        if (!purchased && unlocked) {
            icon.onClick(e -> {
                if (!e.isLeftClick()) return;
                if (ownerData.buyClickerUpgrade(upgrade)) {
                    AchievementManager.check(ownerData);
                    if (viewerData.getSettings().isSoundEnabled(gg.drak.lobbyclicker.settings.SettingType.SOUND_BUY)) {
                        float vol = viewerData.getSettings().getVolume(gg.drak.lobbyclicker.settings.SettingType.VOLUME_BUY);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, vol, 1.5f);
                    }
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                }
                buildDisplay();
            });
        }
        return icon;
    }

    private String describeEffect(ClickerUpgrade upgrade) {
        return switch (upgrade.getEffect()) {
            case CPC_MULTIPLIER -> "×" + upgrade.getEffectValue() + " cookies per click";
            case CPS_MULTIPLIER -> "×" + upgrade.getEffectValue() + " cookies per second";
            case BUILDING_MULTIPLIER -> (upgrade.getTargetBuilding() != null
                    ? upgrade.getTargetBuilding().getDisplayName() : "?") + " output ×" + upgrade.getEffectValue();
            case GOLDEN_FREQ_MULTIPLIER -> "Golden cookies appear ×" + upgrade.getEffectValue() + " as often";
            case GOLDEN_REWARD_MULTIPLIER -> "Golden cookie rewards ×" + upgrade.getEffectValue();
            case GOLDEN_DURATION_MULTIPLIER -> "Golden cookies last ×" + upgrade.getEffectValue() + " as long";
            case GOLDEN_AUTO_COLLECT -> "Cookie clicks auto-collect golden cookies";
            case FINGER_ADDITIVE -> "+" + upgrade.getEffectValue() + " per non-autoclicker building";
            case FINGER_MULTIPLIER -> "Thousand Fingers gain ×" + upgrade.getEffectValue();
            case SYNERGY -> "+" + upgrade.getEffectValue().multiply(java.math.BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString()
                    + "% per " + (upgrade.getSynergyPartner() != null ? upgrade.getSynergyPartner().getDisplayName() : "?");
        };
    }

    private String describeRequirement(ClickerUpgrade upgrade) {
        if (upgrade.getTargetBuilding() != null) {
            return "Own " + upgrade.getRequiredCount() + " " + upgrade.getTargetBuilding().getDisplayName();
        }
        if (upgrade.getRequiredCount() > 0) {
            return upgrade.getRequiredCount() + " cookies from clicking";
        }
        return "None";
    }
}
