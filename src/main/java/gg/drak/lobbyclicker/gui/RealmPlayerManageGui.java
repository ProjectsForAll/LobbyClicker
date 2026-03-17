package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * When clicking a player head in the realm member list, this menu shows:
 * - Manage Player (friend, block, transfer, pay, gamble, ban)
 * - Promote Player (role selection)
 */
public class RealmPlayerManageGui extends BaseGui {
    private final PlayerData data;
    private final String targetUuid;
    private final String returnContext;

    public RealmPlayerManageGui(Player player, PlayerData data, String targetUuid, String returnContext) {
        super(player, "realm-player-manage", ChatColor.GOLD + "" + ChatColor.BOLD + "Manage Player", 3);
        this.data = data;
        this.targetUuid = targetUuid;
        this.returnContext = returnContext;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        // Home button
        Icon home = GuiHelper.homeButton();
        home.onClick(e -> new ClickerGui(player, data).open());
        addItem(0, home);

        String targetName = targetUuid.substring(0, 8);
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(targetUuid)).getName(); if (n != null) targetName = n; } catch (Exception ignored) {}

        // Player head
        addItem(4, GuiHelper.playerHead(targetUuid, ChatColor.WHITE + targetName));

        // Manage Player (opens standard PlayerActionGui)
        Icon manage = GuiHelper.createIcon(Material.WRITABLE_BOOK,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Manage Player",
                "", ChatColor.GRAY + "Friend, Block, Transfer,",
                ChatColor.GRAY + "Pay, Gamble, Ban");
        manage.onClick(e -> new PlayerActionGui(player, data, targetUuid, "realm-" + returnContext).open());
        addItem(11, manage);

        // Promote Player (role selection)
        Icon promote = GuiHelper.createIcon(Material.GOLDEN_HELMET,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Promote Player",
                "", ChatColor.GRAY + "Assign a realm role");
        promote.onClick(e -> new RoleSelectionGui(player, data, targetUuid, returnContext).open());
        addItem(15, promote);

        // Back
        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> {
            boolean isFriends = returnContext.equals("friends");
            new RealmMemberListGui(player, data, isFriends, 0).open();
        });
        addItem(22, back);
    }
}
