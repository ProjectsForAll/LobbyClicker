package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.social.PendingTransaction;
import gg.drak.lobbyclicker.social.TransactionType;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class GambleRequestsGui extends PaginationMonitor {
    private final PlayerData data;
    private final Consumer<Player> backAction;

    public GambleRequestsGui(Player player, PlayerData data, Consumer<Player> backAction) {
        this(player, data, 0, backAction);
    }

    public GambleRequestsGui(Player player, PlayerData data, int page, Consumer<Player> backAction) {
        super(player, "gamble-requests", MonitorStyle.title(ChatColor.GREEN, "Gambling Requests"), page);
        this.data = data;
        this.backAction = backAction;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(backAction);

        List<PendingTransaction> gambles = PendingTransaction.getAllForReceiver(data.getIdentifier())
                .stream().filter(tx -> tx.getType() == TransactionType.GAMBLE).toList();

        if (gambles.isEmpty()) {
            addItem(22, GuiHelper.createIcon(Material.PAPER,
                    ChatColor.GRAY + "No pending bets",
                    "", ChatColor.GRAY + "You have no incoming gambling requests."));
        }

        populatePagedContent(gambles, (tx, slot) -> {
            if (tx == null) return;
            String senderName = tx.getSenderUuid().substring(0, 8);
            try { String n = Bukkit.getOfflinePlayer(UUID.fromString(tx.getSenderUuid())).getName(); if (n != null) senderName = n; } catch (Exception ignored) {}

            boolean canAfford = data.canAfford(tx.getAmount());
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(tx.getSenderUuid()))); } catch (Exception ignored) {}
                meta.setDisplayName(ChatColor.GREEN + senderName);
                meta.setLore(Arrays.asList(
                        "", ChatColor.GRAY + "Bet: " + ChatColor.GOLD + FormatUtils.format(tx.getAmount()) + " cookies",
                        canAfford ? ChatColor.GREEN + "You can afford this" : ChatColor.RED + "You cannot afford this",
                        "",
                        ChatColor.YELLOW + "Left-click: " + ChatColor.WHITE + "View details",
                        canAfford ? ChatColor.GREEN + "Shift+left-click: " + ChatColor.WHITE + "Accept" : "",
                        ChatColor.RED + "Shift+right-click: " + ChatColor.WHITE + "Decline"));
                head.setItemMeta(meta);
            }
            Icon icon = new Icon(head);
            Consumer<Player> returnHere = p -> new GambleRequestsGui(p, data, page, backAction).open();
            icon.onClick(e -> {
                if (e.isShiftClick() && e.isLeftClick() && canAfford) {
                    GambleAcceptGui.acceptGamble(player, data, tx, returnHere);
                } else if (e.isShiftClick() && e.isRightClick()) {
                    GambleAcceptGui.declineGamble(player, data, tx, returnHere);
                } else if (e.isLeftClick()) {
                    new GambleAcceptGui(player, data, tx, returnHere).open();
                }
            });
            addItem(slot, icon);
        });

        addPaginationArrows(gambles, newPage -> new GambleRequestsGui(player, data, newPage, backAction).open());
    }
}
