package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.ConfirmationMonitor;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.function.Consumer;

public class FriendRequestConfirmGui extends ConfirmationMonitor {
    private final PlayerData data;
    private final String senderUuid;
    private final String senderName;
    private final Consumer<Player> backAction;

    public FriendRequestConfirmGui(Player player, PlayerData data, String senderUuid, String senderName, Consumer<Player> backAction) {
        super(player, "friend-confirm", ChatColor.GREEN + "" + ChatColor.BOLD + "Friend Request");
        this.data = data;
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.backAction = backAction;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);

        Icon info = GuiHelper.playerHead(senderUuid,
                ChatColor.GREEN + "" + ChatColor.BOLD + senderName,
                "", ChatColor.GRAY + "wants to be your friend!",
                "", ChatColor.YELLOW + "Accept to add them to your friends list.");

        buildConfirmation(info, "Accept", "Accept friend request?",
                p -> {
                    FriendRequestsGui.acceptRequestStatic(p, data, senderUuid, senderName);
                    backAction.accept(p);
                },
                backAction);
    }
}
