package gg.drak.lobbyclicker.redis;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.math.CookieMath;
import gg.drak.lobbyclicker.settings.PlayerSettings;
import gg.drak.lobbyclicker.settings.SettingType;
import org.bukkit.Sound;

import java.math.BigDecimal;

/**
 * Handles publishing and receiving sync messages over Redis.
 *
 * Channels:
 * - social: Friend/ban/block sync (bidirectional, all servers process)
 * - data: Profile data sync (home server is authoritative, others receive)
 *
 * Data sync model:
 * - The server where a player is ONLINE is the "home server" and is authoritative.
 * - Home server pushes DATA_SYNC every second for each online player.
 * - Other servers receive and update their local PlayerData copies (read-only mirrors).
 * - Visitor clicks on non-home servers are forwarded via CLICK messages.
 * - The home server applies clicks and the next DATA_SYNC pushes the result.
 */
public class RedisSyncHandler {

    // ===================== SOCIAL CHANNEL =====================

    // --- Publish methods (call these alongside DB writes) ---

    public static void publishFriendAdd(String uuid1, String uuid2, long since) {
        publishSocial("FRIEND_ADD:" + uuid1 + ":" + uuid2 + ":" + since);
    }

    public static void publishFriendRemove(String uuid1, String uuid2) {
        publishSocial("FRIEND_REMOVE:" + uuid1 + ":" + uuid2);
    }

    public static void publishFriendRequest(String sender, String receiver) {
        publishSocial("FRIEND_REQ:" + sender + ":" + receiver);
    }

    public static void publishFriendRequestDelete(String sender, String receiver) {
        publishSocial("FRIEND_REQ_DEL:" + sender + ":" + receiver);
    }

    public static void publishBanAdd(String owner, String banned) {
        publishSocial("BAN_ADD:" + owner + ":" + banned);
    }

    public static void publishBanRemove(String owner, String banned) {
        publishSocial("BAN_REMOVE:" + owner + ":" + banned);
    }

    public static void publishBlockAdd(String owner, String blocked) {
        publishSocial("BLOCK_ADD:" + owner + ":" + blocked);
    }

    public static void publishBlockRemove(String owner, String blocked) {
        publishSocial("BLOCK_REMOVE:" + owner + ":" + blocked);
    }

    private static void publishSocial(String message) {
        RedisManager rm = LobbyClicker.getRedisManager();
        if (rm != null) {
            rm.publishSocial(message);
        }
    }

    // ===================== DATA CHANNEL =====================

    /**
     * Publish a full data snapshot for a player. Called every second by CookieTask
     * for each online player on this (home) server.
     *
     * Format: DATA_SYNC:serverId:uuid:cookies:totalEarned:timesClicked:upgrades:prestige:aura:realmPublic:settings
     * The "|" character is used as the field separator to avoid conflicts with ":" in serialized data.
     */
    public static void publishDataSync(PlayerData data) {
        RedisManager rm = LobbyClicker.getRedisManager();
        if (rm == null) return;

        String msg = "DATA_SYNC|" + rm.getServerId()
                + "|" + data.getIdentifier()
                + "|" + data.getCookies().toPlainString()
                + "|" + data.getTotalCookiesEarned().toPlainString()
                + "|" + data.getTimesClicked()
                + "|" + data.serializeUpgrades()
                + "|" + data.getPrestigeLevel()
                + "|" + data.getAura().toPlainString()
                + "|" + (data.isRealmPublic() ? 1 : 0)
                + "|" + data.getSettings().serialize();
        rm.publishData(msg);
    }

    /**
     * Forward a visitor click to the home server of the realm owner.
     * Format: CLICK|serverId|ownerUuid|clickerName
     */
    public static void publishClick(String ownerUuid, String clickerName) {
        RedisManager rm = LobbyClicker.getRedisManager();
        if (rm == null) return;
        rm.publishData("CLICK|" + rm.getServerId() + "|" + ownerUuid + "|" + clickerName);
    }

    /**
     * Publish settings change for a player.
     * Format: SETTINGS_SYNC|serverId|uuid|serializedSettings
     */
    public static void publishSettingsSync(PlayerData data) {
        RedisManager rm = LobbyClicker.getRedisManager();
        if (rm == null) return;
        rm.publishData("SETTINGS_SYNC|" + rm.getServerId() + "|" + data.getIdentifier()
                + "|" + data.getSettings().serialize());
    }

    // ===================== RECEIVE HANDLERS =====================

    /**
     * Handle social channel messages. Called on main Bukkit thread.
     */
    public static void handleSocialMessage(String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length < 3) return;

            String action = parts[0];
            String uuid1 = parts[1];
            String uuid2 = parts[2];

            switch (action) {
                case "FRIEND_ADD":
                    PlayerManager.getPlayer(uuid1).ifPresent(d -> d.getFriends().add(uuid2));
                    PlayerManager.getPlayer(uuid2).ifPresent(d -> {
                        d.getFriends().add(uuid1);
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
                    PlayerManager.getPlayer(uuid1).ifPresent(d -> d.getOutgoingFriendRequests().add(uuid2));
                    PlayerManager.getPlayer(uuid2).ifPresent(d -> {
                        d.getIncomingFriendRequests().add(uuid1);
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

    /**
     * Handle data channel messages. Called on main Bukkit thread.
     * @param message The raw message
     * @param localServerId This server's ID (to ignore own messages)
     */
    public static void handleDataMessage(String message, String localServerId) {
        try {
            String[] parts = message.split("\\|");
            if (parts.length < 3) return;

            String action = parts[0];
            String sourceServerId = parts[1];

            // Ignore our own messages
            if (sourceServerId.equals(localServerId)) return;

            switch (action) {
                case "DATA_SYNC":
                    handleDataSync(parts);
                    break;
                case "CLICK":
                    handleRemoteClick(parts);
                    break;
                case "SETTINGS_SYNC":
                    handleSettingsSync(parts);
                    break;
            }
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to handle data sync message: " + message, e);
        }
    }

    /**
     * Handle DATA_SYNC: update local PlayerData copy from authoritative home server.
     * Only updates if the player is NOT online on this server (home server is authoritative).
     * Format: DATA_SYNC|serverId|uuid|cookies|totalEarned|timesClicked|upgrades|prestige|aura|realmPublic|settings
     */
    private static void handleDataSync(String[] parts) {
        if (parts.length < 11) return;

        String uuid = parts[2];

        // Don't overwrite data for players who are online HERE — we are their home server
        if (org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(uuid)) != null) return;

        // Only update if this player's data is loaded on this server (e.g., someone is viewing their realm)
        PlayerManager.getPlayer(uuid).ifPresent(data -> {
            data.setCookies(CookieMath.parse(parts[3]));
            data.setTotalCookiesEarned(CookieMath.parse(parts[4]));
            try { data.setTimesClicked(Long.parseLong(parts[5])); } catch (NumberFormatException ignored) {}
            data.setUpgrades(PlayerData.deserializeUpgrades(parts[6]));
            try { data.setPrestigeLevel(Integer.parseInt(parts[7])); } catch (NumberFormatException ignored) {}
            data.setAura(CookieMath.parse(parts[8]));
            try { data.setRealmPublic(Integer.parseInt(parts[9]) != 0); } catch (NumberFormatException ignored) {}
            if (parts.length > 10) {
                data.setSettings(new PlayerSettings(parts[10]));
            }
        });
    }

    /**
     * Handle CLICK: a visitor on another server clicked in a realm owned by a player on THIS server.
     * Only apply if the owner is online HERE (we are the home server).
     * Format: CLICK|serverId|ownerUuid|clickerName
     */
    private static void handleRemoteClick(String[] parts) {
        if (parts.length < 4) return;

        String ownerUuid = parts[2];
        String clickerName = parts[3];

        // Only apply if the owner is online on THIS server
        if (org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(ownerUuid)) == null) return;

        PlayerManager.getPlayer(ownerUuid).ifPresent(ownerData -> {
            if (!ownerData.isFullyLoaded()) return;

            ownerData.addCookies(ownerData.getCpc());
            ownerData.setTimesClicked(ownerData.getTimesClicked() + 1);

            // Notify owner about remote click
            ownerData.asPlayer().ifPresent(owner -> {
                if (ownerData.getSettings().isSoundEnabled(SettingType.SOUND_RANDO_CLICKER)) {
                    float vol = ownerData.getSettings().getVolume(SettingType.VOLUME_RANDO_CLICKER);
                    owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, vol, 1.5f);
                }
            });
        });
    }

    /**
     * Handle SETTINGS_SYNC: update settings for a player loaded on this server.
     * Format: SETTINGS_SYNC|serverId|uuid|serializedSettings
     */
    private static void handleSettingsSync(String[] parts) {
        if (parts.length < 4) return;

        String uuid = parts[2];

        // Don't overwrite settings for local players
        if (org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(uuid)) != null) return;

        PlayerManager.getPlayer(uuid).ifPresent(data -> {
            data.setSettings(new PlayerSettings(parts[3]));
        });
    }
}
