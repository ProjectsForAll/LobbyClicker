package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.upgrades.ClickerUpgrade;
import gg.drak.lobbyclicker.upgrades.ClickerUpgradeEffect;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;
import java.util.*;

public class ClickerUpgradeGui extends PaginationMonitor {
    private final PlayerData viewerData;
    private final PlayerData ownerData;

    public ClickerUpgradeGui(Player player, PlayerData viewerData, PlayerData ownerData) {
        super(player, "clicker-upgrades-store", MonitorStyle.title("aqua", "Upgrades"), 0);
        this.viewerData = viewerData;
        this.ownerData = ownerData;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        buildDisplay();
    }

    private void buildDisplay() {
        setPlayerContext(viewerData, ownerData);
        fillMonitorBorder();
        buildStandardActionBar(p -> new ShopGui(p, viewerData, ownerData).open());

        // Cookie info at top
        int purchased = ownerData.getPurchasedUpgrades().size();
        int total = ClickerUpgrade.values().length;
        addItem(4, GuiHelper.createIcon(Material.COOKIE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Upgrades",
                "",
                ChatColor.GRAY + "Cookies: " + ChatColor.WHITE + FormatUtils.format(ownerData.getCookies()),
                ChatColor.GRAY + "Purchased: " + ChatColor.WHITE + purchased + "/" + total,
                "",
                ChatColor.GRAY + "CPC Bonus: " + ChatColor.WHITE + "×" + FormatUtils.format(ownerData.getEffectMultiplier(ClickerUpgradeEffect.CPC_MULTIPLIER)),
                ChatColor.GRAY + "CPS Bonus: " + ChatColor.WHITE + "×" + FormatUtils.format(ownerData.getEffectMultiplier(ClickerUpgradeEffect.CPS_MULTIPLIER))));

        // Sort: available first (by cost), then locked, then purchased
        List<ClickerUpgrade> sorted = new ArrayList<>(Arrays.asList(ClickerUpgrade.values()));
        Set<ClickerUpgrade> owned = ownerData.getPurchasedUpgrades();
        sorted.sort((a, b) -> {
            int aGroup = getDisplayGroup(a, owned);
            int bGroup = getDisplayGroup(b, owned);
            if (aGroup != bGroup) return Integer.compare(aGroup, bGroup);
            return a.getCost().compareTo(b.getCost());
        });

        // Filter out hidden upgrades
        sorted.removeIf(u -> !owned.contains(u) && u.isHidden(ownerData.getActiveProfile()));

        populatePagedContent(sorted, (upgrade, slot) -> {
            if (upgrade != null) {
                addItem(slot, createUpgradeIcon(upgrade));
            }
        });
        addPaginationArrows(sorted, newPage -> {});
    }

    private int getDisplayGroup(ClickerUpgrade u, Set<ClickerUpgrade> owned) {
        if (owned.contains(u)) return 3; // purchased last
        if (!u.isUnlocked(ownerData.getActiveProfile())) return 2; // locked
        if (ownerData.canAfford(u.getCost())) return 0; // affordable
        return 1; // unlocked but can't afford
    }

    private Icon createUpgradeIcon(ClickerUpgrade upgrade) {
        boolean purchased = ownerData.hasPurchasedUpgrade(upgrade);
        boolean unlocked = upgrade.isUnlocked(ownerData.getActiveProfile());
        boolean canAfford = !purchased && unlocked && ownerData.canAfford(upgrade.getCost());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + upgrade.getDescription());
        lore.add("");

        // Effect description
        lore.add(ChatColor.YELLOW + "Effect: " + ChatColor.WHITE + describeEffect(upgrade));

        // Unlock requirement
        if (!unlocked && !purchased) {
            lore.add("");
            lore.add(ChatColor.RED + "Requires: " + ChatColor.WHITE + describeRequirement(upgrade));
        }

        lore.add("");
        if (purchased) {
            lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "PURCHASED");
        } else {
            lore.add(ChatColor.GRAY + "Cost: " + (canAfford ? ChatColor.GREEN : ChatColor.RED) + FormatUtils.format(upgrade.getCost()) + " cookies");
            lore.add("");
            if (canAfford) {
                lore.add(ChatColor.YELLOW + "Click to buy!");
            } else if (!unlocked) {
                lore.add(ChatColor.RED + "Requirements not met!");
            } else {
                lore.add(ChatColor.RED + "Not enough cookies!");
            }
        }

        // Choose display color
        String nameColor;
        Material mat;
        if (purchased) {
            nameColor = ChatColor.GOLD.toString();
            mat = upgrade.getMaterial();
        } else if (canAfford) {
            nameColor = ChatColor.GREEN.toString();
            mat = upgrade.getMaterial();
        } else if (unlocked) {
            nameColor = ChatColor.RED.toString();
            mat = upgrade.getMaterial();
        } else {
            nameColor = ChatColor.DARK_GRAY.toString();
            mat = Material.GRAY_DYE;
        }

        Icon icon = GuiHelper.createIcon(mat,
                nameColor + ChatColor.BOLD + upgrade.getDisplayName(),
                lore.toArray(new String[0]));

        if (!purchased && unlocked) {
            icon.onClick(e -> {
                if (!e.isLeftClick()) return;
                if (ownerData.buyClickerUpgrade(upgrade)) {
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
        String mult = formatMultiplier(upgrade.getEffectValue());
        switch (upgrade.getEffect()) {
            case CPC_MULTIPLIER:
                return mult + " cookies per click";
            case CPS_MULTIPLIER:
                return mult + " cookies per second";
            case BUILDING_MULTIPLIER:
                String buildingName = upgrade.getTargetBuilding() != null ? upgrade.getTargetBuilding().getDisplayName() : "?";
                return buildingName + " output " + mult;
            case GOLDEN_FREQ_MULTIPLIER:
                return "Golden cookies appear " + mult + " as often";
            case GOLDEN_REWARD_MULTIPLIER:
                return "Golden cookie rewards " + mult;
            case GOLDEN_DURATION_MULTIPLIER:
                return "Golden cookies last " + mult + " as long";
            default:
                return "Unknown";
        }
    }

    /** Format a multiplier like 2 → "Double", 1.5 → "×1.5", 1.1 → "+10%", 1.25 → "+25%" */
    private static String formatMultiplier(java.math.BigDecimal value) {
        if (value.compareTo(java.math.BigDecimal.valueOf(2)) == 0) return "Double";
        if (value.compareTo(java.math.BigDecimal.valueOf(3)) == 0) return "Triple";
        // For values like 1.1, 1.25, 1.5 — show as percentage bonus
        if (value.compareTo(java.math.BigDecimal.ONE) > 0 && value.compareTo(java.math.BigDecimal.valueOf(2)) < 0) {
            java.math.BigDecimal pct = value.subtract(java.math.BigDecimal.ONE).multiply(java.math.BigDecimal.valueOf(100));
            String pctStr = pct.stripTrailingZeros().toPlainString();
            return "+" + pctStr + "%";
        }
        return "×" + value.stripTrailingZeros().toPlainString();
    }

    private String describeRequirement(ClickerUpgrade upgrade) {
        List<String> parts = new ArrayList<>();
        if (upgrade.getRequiredPrestigeLevel() > 0) {
            parts.add("Prestige " + upgrade.getRequiredPrestigeLevel());
        }
        if (upgrade.getEffect() == ClickerUpgradeEffect.CPC_MULTIPLIER && upgrade.getRequiredCount() > 0) {
            parts.add(upgrade.getRequiredCount() + " total realm clicks");
        } else if (upgrade.getTargetBuilding() != null) {
            parts.add("Own " + upgrade.getRequiredCount() + " " + upgrade.getTargetBuilding().getDisplayName());
        }
        if (parts.isEmpty()) return "Unknown";
        return String.join(" and ", parts);
    }
}
