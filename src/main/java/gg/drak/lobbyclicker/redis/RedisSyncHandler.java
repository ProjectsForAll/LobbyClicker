package gg.drak.lobbyclicker.redis;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.settings.SettingType;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Handles publishing and receiving social sync messages over Redis.
 * When a social action happens on any server, it publishes a message.
 * Other servers receive it and update their in-memory PlayerData accordingly.
 */
public class RedisSyncHandler {

    // --- Publish methods (call these alongside DB writes) ---

    public static void publishFriendAdd(String uuid1, String uuid2, long since) {
        publish("FRIEND_ADD:" + uuid1 + ":" + uuid2 + ":" + since);
    }

    public static void publishFriendRemove(String uuid1, String uuid2) {
        publish("FRIEND_REMOVE:" + uuid1 + ":" + uuid2);
    }

    public static void publishFriendRequest(String sender, String receiver) {
        publish("FRIEND_REQ:" + sender + ":" + receiver);
    }

    public static void publishFriendRequestDelete(String sender, String receiver) {
        publish("FRIEND_REQ_DEL:" + sender + ":" + receiver);
    }

    public static void publishBanAdd(String owner, String banned) {
        publish("BAN_ADD:" + owner + ":" + banned);
    }

    public static void publishBanRemove(String owner, String banned) {
        publish("BAN_REMOVE:" + owner + ":" + banned);
    }

    public static void publishBlockAdd(String owner, String blocked) {
        publish("BLOCK_ADD:" + owner + ":" + blocked);
    }

    public static void publishBlockRemove(String owner, String blocked) {
        publish("BLOCK_REMOVE:" + owner + ":" + blocked);
    }

    private static void publish(String message) {
        RedisManager rm = LobbyClicker.getRedisManager();
        if (rm != null) {
            rm.publishSocial(message);
        }
    }

    // --- Receive handler (called by RedisManager when social message arrives) ---

    public static void handleSocialMessage(String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length < 3) return;

            String action = parts[0];
            String uuid1 = parts[1];
            String uuid2 = parts[2];

            switch (action) {
                case "FRIEND_ADD":
                    // If either player is loaded on this server, add the other to their friends
                    PlayerManager.getPlayer(uuid1).ifPresent(d -> d.getFriends().add(uuid2));
                    PlayerManager.getPlayer(uuid2).ifPresent(d -> {
                        d.getFriends().add(uuid1);
                        // Notify if online
                        d.asPlayer().ifPresent(p -> {
                            String name = PlayerManager.getPlayer(uuid1).map(PlayerData::getName).orElse("Someone");
                            p.sendMessage("\u00a7a" + name + " is now your friend!");
                        });
                    });
                    break;

                case "FRIEND_REMOVE":
                    PlayerManager.getPlayer(uuid1).ifPresent(d -> d.getFriends().remove(uuid2));
                    PlayerManager.getPlayer(uuid2).ifPresent(d -> d.getFriends().remove(uuid1));
                    break;

                case "FRIEND_REQ":
                    // uuid1 = sender, uuid2 = receiver
                    PlayerManager.getPlayer(uuid1).ifPresent(d -> d.getOutgoingFriendRequests().add(uuid2));
                    PlayerManager.getPlayer(uuid2).ifPresent(d -> {
                        d.getIncomingFriendRequests().add(uuid1);
                        // Notify receiver
                        d.asPlayer().ifPresent(p -> {
                            if (d.getSettings().isSoundEnabled(SettingType.SOUND_FRIEND_REQUEST)) {
                                float vol = d.getSettings().getVolume(SettingType.VOLUME_FRIEND_REQUEST);
                                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, vol, 1.0f);
                            }
                            String senderName = PlayerManager.getPlayer(uuid1).map(PlayerData::getName).orElse("Someone");
                            p.sendMessage("\u00a7a" + senderName + " sent you a friend request!");
                        });
                    });
                    break;

                case "FRIEND_REQ_DEL":
                    PlayerManager.getPlayer(uuid1).ifPresent(d -> d.getOutgoingFriendRequests().remove(uuid2));
                    PlayerManager.getPlayer(uuid2).ifPresent(d -> d.getIncomingFriendRequests().remove(uuid1));
                    break;

                case "BAN_ADD":
                    PlayerManager.getPlayer(uuid1).ifPresent(d -> d.getBans().add(uuid2));
                    break;

                case "BAN_REMOVE":
                    PlayerManager.getPlayer(uuid1).ifPresent(d -> d.getBans().remove(uuid2));
                    break;

                case "BLOCK_ADD":
                    PlayerManager.getPlayer(uuid1).ifPresent(d -> d.getBlocks().add(uuid2));
                    break;

                case "BLOCK_REMOVE":
                    PlayerManager.getPlayer(uuid1).ifPresent(d -> d.getBlocks().remove(uuid2));
                    break;
            }
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to handle social sync message: " + message, e);
        }
    }
}
