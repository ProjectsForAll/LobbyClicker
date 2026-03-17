package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.redis.RedisSyncHandler;
import gg.drak.lobbyclicker.settings.SettingType;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

public class PlayerActionGui extends Gui {
    private final PlayerData viewerData;
    private final String targetUuid;
    private final String returnTo; // "friends", "all", "viewers", "social"

    public PlayerActionGui(Player player, PlayerData viewerData, String targetUuid, String returnTo) {
        super(player, "player-action", ChatColor.YELLOW + "" + ChatColor.BOLD + "Player Actions", 3);
        this.viewerData = viewerData;
        this.targetUuid = targetUuid;
        this.returnTo = returnTo;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        fillGui(GuiHelper.filler());

        String targetName = targetUuid.substring(0, 8);
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(targetUuid)).getName(); if (n != null) targetName = n; } catch (Exception ignored) {}
        boolean isFriend = viewerData.getFriends().contains(targetUuid);
        boolean isBanned = viewerData.getBans().contains(targetUuid);
        boolean isBlocked = viewerData.getBlocks().contains(targetUuid);

        // Target player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(targetUuid))); } catch (Exception ignored) {}
            meta.setDisplayName(ChatColor.WHITE + targetName);
            meta.setLore(Arrays.asList(
                    isFriend ? ChatColor.GREEN + "Friend" : ChatColor.GRAY + "Not a friend",
                    isBanned ? ChatColor.RED + "Banned" : "",
                    isBlocked ? ChatColor.DARK_RED + "Blocked" : ""
            ));
            head.setItemMeta(meta);
        }
        addItem(4, new Icon(head));

        // Friend / Unfriend
        if (isFriend) {
            Icon unfriend = GuiHelper.createIcon(Material.RED_DYE, ChatColor.RED + "Unfriend",
                    "", ChatColor.GRAY + "Remove from friends list");
            unfriend.onClick(e -> {
                viewerData.getFriends().remove(targetUuid);
                LobbyClicker.getDatabase().deleteFriendThreaded(viewerData.getIdentifier(), targetUuid);
                RedisSyncHandler.publishFriendRemove(viewerData.getIdentifier(), targetUuid);
                PlayerManager.getPlayer(targetUuid).ifPresent(td -> td.getFriends().remove(viewerData.getIdentifier()));
                player.sendMessage(ChatColor.RED + "Removed " + ChatColor.WHITE + getTargetName() + ChatColor.RED + " from friends.");
                new PlayerActionGui(player, viewerData, targetUuid, returnTo).open();
            });
            addItem(10, unfriend);
        } else {
            boolean hasOutgoing = viewerData.getOutgoingFriendRequests().contains(targetUuid);
            boolean hasIncoming = viewerData.getIncomingFriendRequests().contains(targetUuid);

            if (hasIncoming) {
                Icon accept = GuiHelper.createIcon(Material.LIME_DYE, ChatColor.GREEN + "Accept Friend Request",
                        "", ChatColor.GRAY + "They sent you a request!");
                accept.onClick(e -> acceptFriendRequest(player));
                addItem(10, accept);
            } else if (hasOutgoing) {
                addItem(10, GuiHelper.createIcon(Material.CLOCK, ChatColor.YELLOW + "Request Pending",
                        "", ChatColor.GRAY + "Waiting for response..."));
            } else if (!isBlocked) {
                Icon sendReq = GuiHelper.createIcon(Material.LIME_DYE, ChatColor.GREEN + "Send Friend Request",
                        "", ChatColor.GRAY + "Send a friend request");
                sendReq.onClick(e -> {
                    viewerData.getOutgoingFriendRequests().add(targetUuid);
                    LobbyClicker.getDatabase().pushFriendRequestThreaded(viewerData.getIdentifier(), targetUuid, System.currentTimeMillis());
                    RedisSyncHandler.publishFriendRequest(viewerData.getIdentifier(), targetUuid);
                    PlayerManager.getPlayer(targetUuid).ifPresent(td -> {
                        td.getIncomingFriendRequests().add(viewerData.getIdentifier());
                        // Auto-accept check
                        if (td.getSettings().getBool(SettingType.AUTO_ACCEPT_FRIENDS)) {
                            acceptFriendRequestFor(td, viewerData);
                            player.sendMessage(ChatColor.GREEN + getTargetName() + " auto-accepted your friend request!");
                            return;
                        }
                        // Notify target
                        td.asPlayer().ifPresent(tp -> {
                            if (td.getSettings().isSoundEnabled(SettingType.SOUND_FRIEND_REQUEST)) {
                                tp.playSound(tp.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                                        td.getSettings().getVolume(SettingType.VOLUME_FRIEND_REQUEST), 1.0f);
                            }
                            tp.sendMessage(ChatColor.GREEN + viewerData.getName() + " sent you a friend request!");
                        });
                    });
                    player.sendMessage(ChatColor.GREEN + "Friend request sent to " + getTargetName() + "!");
                    new PlayerActionGui(player, viewerData, targetUuid, returnTo).open();
                });
                addItem(10, sendReq);
            }
        }

        // Visit Realm
        Icon visit = GuiHelper.createIcon(Material.ENDER_PEARL, ChatColor.AQUA + "Visit Realm",
                "", ChatColor.GRAY + "Visit their clicker realm");
        visit.onClick(e -> {
            PlayerData targetData = PlayerManager.getPlayer(targetUuid).orElse(null);
            boolean targetOnline = targetData != null && targetData.isFullyLoaded();

            if (!targetOnline) {
                // Try offline realm visit
                if (!isFriend) {
                    player.sendMessage(ChatColor.RED + "Player is not online.");
                    return;
                }
                player.sendMessage(ChatColor.YELLOW + "Loading offline realm...");
                java.util.Optional<PlayerData> offlineOpt = PlayerManager.getOrGetPlayer(targetUuid);
                if (offlineOpt.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Could not load player data.");
                    return;
                }
                PlayerData offlineData = offlineOpt.get().waitUntilFullyLoaded();
                if (!offlineData.getSettings().getBool(SettingType.ALLOW_OFFLINE_REALM)) {
                    player.sendMessage(ChatColor.RED + "This player does not allow offline realm visits.");
                    PlayerManager.unloadPlayer(offlineData);
                    return;
                }
                boolean banned = offlineData.getBans().contains(viewerData.getIdentifier())
                        || offlineData.getBlocks().contains(viewerData.getIdentifier());
                if (banned) {
                    player.sendMessage(ChatColor.RED + "You are not allowed to visit this realm.");
                    PlayerManager.unloadPlayer(offlineData);
                    return;
                }
                new ClickerGui(player, viewerData, offlineData).open();
                return;
            }

            boolean canVisit = targetData.isRealmPublic()
                    || (targetData.getFriends().contains(viewerData.getIdentifier())
                    && targetData.getSettings().getBool(SettingType.ALLOW_FRIEND_JOINS));
            boolean banned = targetData.getBans().contains(viewerData.getIdentifier())
                    || targetData.getBlocks().contains(viewerData.getIdentifier());
            if (banned) {
                player.sendMessage(ChatColor.RED + "You are not allowed to visit this realm.");
                return;
            }
            if (!canVisit) {
                player.sendMessage(ChatColor.RED + "This realm is private.");
                return;
            }
            new ClickerGui(player, viewerData, targetData).open();
        });
        addItem(12, visit);

        // Pay
        Icon pay = GuiHelper.createIcon(Material.GOLD_INGOT, ChatColor.GOLD + "Pay Cookies",
                "", ChatColor.GRAY + "Send cookies to this player");
        pay.onClick(e -> new PaymentGui(player, viewerData, targetUuid, BigDecimal.ZERO).open());
        addItem(13, pay);

        // Gamble
        Icon gamble = GuiHelper.createIcon(Material.EMERALD, ChatColor.GREEN + "Gamble",
                "", ChatColor.GRAY + "Bet cookies against this player");
        gamble.onClick(e -> new GambleGui(player, viewerData, targetUuid, BigDecimal.ZERO).open());
        addItem(14, gamble);

        // Ban / Unban
        if (isBanned) {
            Icon unban = GuiHelper.createIcon(Material.IRON_DOOR, ChatColor.YELLOW + "Unban",
                    "", ChatColor.GRAY + "Allow them to visit your realm again");
            unban.onClick(e -> {
                viewerData.getBans().remove(targetUuid);
                LobbyClicker.getDatabase().deleteBanThreaded(viewerData.getIdentifier(), targetUuid);
                RedisSyncHandler.publishBanRemove(viewerData.getIdentifier(), targetUuid);
                player.sendMessage(ChatColor.YELLOW + "Unbanned " + getTargetName());
                new PlayerActionGui(player, viewerData, targetUuid, returnTo).open();
            });
            addItem(15, unban);
        } else {
            Icon ban = GuiHelper.createIcon(Material.IRON_DOOR, ChatColor.RED + "Ban",
                    "", ChatColor.GRAY + "Prevent from visiting your realm");
            ban.onClick(e -> {
                viewerData.getBans().add(targetUuid);
                LobbyClicker.getDatabase().pushBanThreaded(viewerData.getIdentifier(), targetUuid);
                RedisSyncHandler.publishBanAdd(viewerData.getIdentifier(), targetUuid);
                player.sendMessage(ChatColor.RED + "Banned " + getTargetName() + " from your realm.");
                new PlayerActionGui(player, viewerData, targetUuid, returnTo).open();
            });
            addItem(15, ban);
        }

        // Block / Unblock
        if (isBlocked) {
            Icon unblock = GuiHelper.createIcon(Material.BARRIER, ChatColor.YELLOW + "Unblock",
                    "", ChatColor.GRAY + "Allow friend requests & realm visits");
            unblock.onClick(e -> {
                viewerData.getBlocks().remove(targetUuid);
                LobbyClicker.getDatabase().deleteBlockThreaded(viewerData.getIdentifier(), targetUuid);
                RedisSyncHandler.publishBlockRemove(viewerData.getIdentifier(), targetUuid);
                player.sendMessage(ChatColor.YELLOW + "Unblocked " + getTargetName());
                new PlayerActionGui(player, viewerData, targetUuid, returnTo).open();
            });
            addItem(16, unblock);
        } else {
            Icon block = GuiHelper.createIcon(Material.BARRIER, ChatColor.DARK_RED + "Block",
                    "", ChatColor.GRAY + "Ban + prevent friend requests");
            block.onClick(e -> {
                viewerData.getBlocks().add(targetUuid);
                viewerData.getBans().add(targetUuid);
                LobbyClicker.getDatabase().pushBlockThreaded(viewerData.getIdentifier(), targetUuid);
                LobbyClicker.getDatabase().pushBanThreaded(viewerData.getIdentifier(), targetUuid);
                RedisSyncHandler.publishBlockAdd(viewerData.getIdentifier(), targetUuid);
                RedisSyncHandler.publishBanAdd(viewerData.getIdentifier(), targetUuid);
                player.sendMessage(ChatColor.DARK_RED + "Blocked " + getTargetName());
                new PlayerActionGui(player, viewerData, targetUuid, returnTo).open();
            });
            addItem(16, block);
        }

        // Back
        Icon back = GuiHelper.backButton("Back");
        back.onClick(e -> {
            switch (returnTo) {
                case "friends": new FriendsListGui(player, viewerData, 0).open(); break;
                case "all": new AllPlayersGui(player, viewerData, 0).open(); break;
                case "viewers": new RealmViewersGui(player, viewerData).open(); break;
                default: new SocialMainGui(player, viewerData).open(); break;
            }
        });
        addItem(22, back);
    }

    private String getTargetName() {
        try { String n = Bukkit.getOfflinePlayer(UUID.fromString(targetUuid)).getName(); if (n != null) return n; } catch (Exception ignored) {}
        return targetUuid.substring(0, 8);
    }

    private void acceptFriendRequest(Player player) {
        long now = System.currentTimeMillis();
        viewerData.getIncomingFriendRequests().remove(targetUuid);
        viewerData.getFriends().add(targetUuid);
        LobbyClicker.getDatabase().deleteFriendRequestThreaded(targetUuid, viewerData.getIdentifier());
        LobbyClicker.getDatabase().pushFriendThreaded(viewerData.getIdentifier(), targetUuid, now);
        RedisSyncHandler.publishFriendRequestDelete(targetUuid, viewerData.getIdentifier());
        RedisSyncHandler.publishFriendAdd(viewerData.getIdentifier(), targetUuid, now);
        PlayerManager.getPlayer(targetUuid).ifPresent(td -> {
            td.getOutgoingFriendRequests().remove(viewerData.getIdentifier());
            td.getFriends().add(viewerData.getIdentifier());
            td.asPlayer().ifPresent(tp -> tp.sendMessage(ChatColor.GREEN + viewerData.getName() + " accepted your friend request!"));
        });
        player.sendMessage(ChatColor.GREEN + "You are now friends with " + getTargetName() + "!");
        new PlayerActionGui(player, viewerData, targetUuid, returnTo).open();
    }

    private static void acceptFriendRequestFor(PlayerData receiver, PlayerData sender) {
        long now = System.currentTimeMillis();
        receiver.getIncomingFriendRequests().remove(sender.getIdentifier());
        receiver.getFriends().add(sender.getIdentifier());
        sender.getOutgoingFriendRequests().remove(receiver.getIdentifier());
        sender.getFriends().add(receiver.getIdentifier());
        LobbyClicker.getDatabase().deleteFriendRequestThreaded(sender.getIdentifier(), receiver.getIdentifier());
        LobbyClicker.getDatabase().pushFriendThreaded(sender.getIdentifier(), receiver.getIdentifier(), now);
        RedisSyncHandler.publishFriendRequestDelete(sender.getIdentifier(), receiver.getIdentifier());
        RedisSyncHandler.publishFriendAdd(sender.getIdentifier(), receiver.getIdentifier(), now);
    }
}
