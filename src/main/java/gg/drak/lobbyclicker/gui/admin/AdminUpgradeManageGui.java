package gg.drak.lobbyclicker.gui.admin;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.gui.GuiHelper;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.upgrades.UpgradeType;
import gg.drak.lobbyclicker.utils.ChatInput;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Admin GUI for managing a player's Cookie Helper upgrade counts.
 * Left-click: set count via chat input. Right-click: +1. Shift-right: -1.
 */
public class AdminUpgradeManageGui extends PaginationMonitor {
    private final String targetUuid;
    private final String targetName;

    public AdminUpgradeManageGui(Player player, String targetUuid, String targetName) {
        this(player, targetUuid, targetName, 0);
    }

    public AdminUpgradeManageGui(Player player, String targetUuid, String targetName, int page) {
        super(player, "admin-upgrades",
                ChatColor.DARK_RED + "" + ChatColor.BOLD + targetName + "'s Upgrades", page);
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        fillMonitorBorder();

        int b = (getSize() / 9 - 1) * 9;
        Icon back = GuiHelper.createIcon(Material.DARK_OAK_DOOR,
                ChatColor.RED + "" + ChatColor.BOLD + "Back", "", ChatColor.GRAY + "Back to player");
        back.onClick(e -> new AdminPlayerManageGui(player, targetUuid, targetName).open());
        addItem(b + 7, back);

        PlayerData data = PlayerManager.getPlayer(targetUuid).orElse(null);
        if (data == null || !data.isFullyLoaded()) {
            java.util.Optional<PlayerData> opt = PlayerManager.getOrGetPlayer(targetUuid);
            if (opt.isPresent()) data = opt.get().waitUntilFullyLoaded();
        }
        if (data == null) {
            addItem(22, GuiHelper.createIcon(Material.BARRIER, ChatColor.RED + "Could not load data"));
            return;
        }

        addItem(4, GuiHelper.createIcon(Material.DIAMOND,
                ChatColor.AQUA + "" + ChatColor.BOLD + targetName + "'s Upgrades",
                "", ChatColor.GRAY + "Left-click: set count",
                ChatColor.GRAY + "Right-click: +1",
                ChatColor.GRAY + "Shift+right-click: -1"));

        List<UpgradeType> types = Arrays.asList(UpgradeType.values());
        PlayerData finalData = data;

        populatePagedContent(types, (type, slot) -> {
            int count = finalData.getUpgradeCount(type);
            Icon icon = GuiHelper.createIcon(type.getMaterial(),
                    ChatColor.GREEN + "" + ChatColor.BOLD + type.getDisplayName(),
                    "", ChatColor.GRAY + "Count: " + ChatColor.WHITE + count,
                    ChatColor.GRAY + "CPS each: " + ChatColor.WHITE + FormatUtils.format(type.getCpsPerLevel()),
                    "", ChatColor.YELLOW + "Left-click: set",
                    ChatColor.GREEN + "Right-click: +1",
                    ChatColor.RED + "Shift+Right: -1");
            icon.onClick(e -> {
                if (e.isLeftClick() && !e.isShiftClick()) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "Set " + type.getDisplayName() + " count to: (type 'cancel' to cancel)");
                    ChatInput.request(player, input -> {
                        if (input == null || input.equalsIgnoreCase("cancel")) {
                            new AdminUpgradeManageGui(player, targetUuid, targetName, page).open();
                            return;
                        }
                        try {
                            int newCount = Math.max(0, Integer.parseInt(input.trim()));
                            finalData.setUpgradeCount(type, newCount);
                            finalData.save(true);
                            player.sendMessage(ChatColor.GREEN + "Set " + type.getDisplayName() + " to " + newCount);
                        } catch (NumberFormatException ex) {
                            player.sendMessage(ChatColor.RED + "Invalid number: " + input);
                        }
                        new AdminUpgradeManageGui(player, targetUuid, targetName, page).open();
                    });
                } else if (e.isRightClick() && !e.isShiftClick()) {
                    finalData.setUpgradeCount(type, count + 1);
                    finalData.save(true);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                    new AdminUpgradeManageGui(player, targetUuid, targetName, page).open();
                } else if (e.isRightClick() && e.isShiftClick()) {
                    finalData.setUpgradeCount(type, Math.max(0, count - 1));
                    finalData.save(true);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1f);
                    new AdminUpgradeManageGui(player, targetUuid, targetName, page).open();
                }
            });
            addItem(slot, icon);
        });

        addPaginationArrows(types, newPage -> new AdminUpgradeManageGui(player, targetUuid, targetName, newPage).open());
    }
}
