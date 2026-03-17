package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.realm.RealmRole;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.UUID;

public class RoleSelectionGui extends BaseGui {
    private final PlayerData data;
    private final String targetUuid;
    private final String returnContext;

    public RoleSelectionGui(Player player, PlayerData data, String targetUuid, String returnContext) {
        super(player, "role-selection", ChatColor.GREEN + "" + ChatColor.BOLD + "Assign Role", 3);
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

        RealmProfile profile = data.getActiveProfile();
        RealmRole currentRole = profile != null ? profile.getRole(targetUuid) : RealmRole.VISITOR;

        String targetName = targetUuid.substring(0, 8);
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(targetUuid)).getName(); if (n != null) targetName = n; } catch (Exception ignored) {}

        addItem(4, GuiHelper.playerHead(targetUuid, ChatColor.WHITE + targetName,
                "", ChatColor.GRAY + "Current role: " + ChatColor.WHITE + currentRole.getDisplayName()));

        // Role buttons
        addRoleButton(player, 10, RealmRole.VISITOR, currentRole, Material.LEATHER_BOOTS,
                ChatColor.GRAY, "Default role, can click");
        addRoleButton(player, 12, RealmRole.GARDENER, currentRole, Material.IRON_HOE,
                ChatColor.GREEN, "Can purchase upgrades");
        addRoleButton(player, 14, RealmRole.MODERATOR, currentRole, Material.IRON_SWORD,
                ChatColor.YELLOW, "Can ban players from realm");
        addRoleButton(player, 16, RealmRole.ADMIN, currentRole, Material.DIAMOND_HELMET,
                ChatColor.RED, "Co-owner, full access");

        // Back
        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> new RealmPlayerManageGui(player, data, targetUuid, returnContext).open());
        addItem(22, back);
    }

    private void addRoleButton(Player player, int slot, RealmRole role, RealmRole currentRole, Material mat, ChatColor color, String desc) {
        boolean isCurrent = role == currentRole;
        String status = isCurrent ? ChatColor.GREEN + " (Current)" : "";
        Icon icon = GuiHelper.createIcon(isCurrent ? Material.LIME_DYE : mat,
                color + "" + ChatColor.BOLD + role.getDisplayName() + status,
                "", ChatColor.GRAY + desc,
                "", isCurrent ? ChatColor.GRAY + "Already assigned" : ChatColor.YELLOW + "Click to assign");
        if (!isCurrent) {
            icon.onClick(e -> {
                RealmProfile profile = data.getActiveProfile();
                if (profile != null) {
                    profile.setRole(targetUuid, role);
                    data.save(true);
                    player.sendMessage(ChatColor.GREEN + "Role updated to " + ChatColor.WHITE + role.getDisplayName());
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
                new RoleSelectionGui(player, data, targetUuid, returnContext).open();
            });
        }
        addItem(slot, icon);
    }
}
