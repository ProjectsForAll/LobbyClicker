package gg.drak.lobbyclicker.redis;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.math.CookieMath;
import gg.drak.lobbyclicker.realm.ProfileManager;
import gg.drak.lobbyclicker.realm.RealmProfile;
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
            // Prepend source server ID so receivers can skip duplicate local handling
            rm.publishSocial(rm.getServerId() + ":" + message);
        }
    }

    // ===================== DATA CHANNEL =====================

    /**
     * Publish a full data snapshot for a cur player's active profile.
     * Called every second by CookieTask for each cur player on this server.
     *
     * Format: DATA_SYNC|serverId|uuid|profileId|cookies|totalEarned|timesClicked|upgrades|prestige|aura|realmPublic|settings
     */
    public static void publishDataSync(PlayerData data) {
        RedisManager rm = LobbyClicker.getRedisManager();
        if (rm == null) return;

        RealmProfile profile = data.getActiveProfile();
        if (profile == null) return;

        String msg = "DATA_SYNC|" + rm.getServerId()
                + "|" + data.getIdentifier()
                + "|" + profile.getProfileId()
                + "|" + profile.getCookies().toPlainString()
                + "|" + profile.getTotalCookiesEarned().toPlainString()
                + "|" + profile.getTimesClicked()
                + "|" + profile.getOwnerClicks()
                + "|" + profile.getOtherClicks()
                + "|" + profile.serializeUpgrades()
                + "|" + profile.getPrestigeLevel()
                + "|" + profile.getAura().toPlainString()
                + "|" + (profile.isRealmPublic() ? 1 : 0)
                + "|" + data.getSettings().serialize()
                + "|" + profile.serializePurchasedUpgrades()
                + "|" + profile.getLifetimeCookiesEarned().toPlainString();
        rm.publishData(msg);
    }

    /**
     * Forward a visitor click to the home server of the realm owner.
     * Format: CLICK|serverId|ownerUuid|clickerUuid|clickerName
     */
    public static void publishClick(String ownerUuid, String clickerUuid, String clickerName) {
        RedisManager rm = LobbyClicker.getRedisManager();
        if (rm == null) return;
        rm.publishData("CLICK|" + rm.getServerId() + "|" + ownerUuid + "|" + clickerUuid + "|" + clickerName);
    }

    /**
     * Forward an upgrade purchase to the home server of the realm owner.
     * Format: BUY_UPGRADE|serverId|ownerUuid|buyerUuid|upgradeTypeName
     */
    public static void publishBuyUpgrade(String ownerUuid, String buyerUuid, String upgradeTypeName) {
        RedisManager rm = LobbyClicker.getRedisManager();
        if (rm == null) return;
        rm.publishData("BUY_UPGRADE|" + rm.getServerId() + "|" + ownerUuid + "|" + buyerUuid + "|" + upgradeTypeName);
    }

    /**
     * Publish a sound event to be played for a specific player on their conn server.
     * Format: SOUND|serverId|targetUuid|soundName|volume|pitch
     */
    public static void publishSound(String targetUuid, String soundName, float volume, float pitch) {
        RedisManager rm = LobbyClicker.getRedisManager();
        if (rm == null) return;
        rm.publishData("SOUND|" + rm.getServerId() + "|" + targetUuid + "|" + soundName + "|" + volume + "|" + pitch);
    }

    /**
     * Publish a message to be sent to a specific player on their conn server.
     * Format: MESSAGE|serverId|targetUuid|message
     */
    public static void publishMessage(String targetUuid, String message) {
        RedisManager rm = LobbyClicker.getRedisManager();
        if (rm == null) return;
        rm.publishData("MESSAGE|" + rm.getServerId() + "|" + targetUuid + "|" + message);
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
            if (parts.length < 4) return;

            String sourceServerId = parts[0];
            String action = parts[1];
            String uuid1 = parts[2];
            String uuid2 = parts[3];

            // If this message originated from our own server, skip —
            // the local code already handled data updates, sounds, and messages.
            RedisManager rm = LobbyClicker.getRedisManager();
            if (rm != null && sourceServerId.equals(rm.getServerId())) return;

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
            String[] parts = message.split("\\|", -1); // -1 preserves trailing empty strings
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
                case "BUY_UPGRADE":
                    handleRemoteBuyUpgrade(parts);
                    break;
                case "SETTINGS_SYNC":
                    handleSettingsSync(parts);
                    break;
                case "SOUND":
                    handleSound(parts);
                    break;
                case "MESSAGE":
                    handleMessage(parts);
                    break;
            }
        } catch (Throwable e) {
            LobbyClicker.getInstance().logWarning("Failed to handle data sync message: " + message, e);
        }
    }

    /**
     * Handle DATA_SYNC: update the specific profile from the authoritative home server.
     * Only updates if the player is NOT a cur player on this server.
     * Updates the profile directly in ProfileManager so the GUI sees fresh data.
     *
     * Format: DATA_SYNC|serverId|uuid|profileId|cookies|totalEarned|timesClicked|upgrades|prestige|aura|realmPublic|settings
     */
    private static void handleDataSync(String[] parts) {
        // Format: DATA_SYNC|serverId|uuid|profileId|cookies|totalEarned|timesClicked|ownerClicks|otherClicks|upgrades|prestige|aura|realmPublic[|settings]
        // Indices:    0         1      2      3        4         5            6            7           8         9        10      11      12        13
        if (parts.length < 13) return;

        String uuid = parts[2];
        String profileId = parts[3];

        // Don't overwrite data for cur players — this server is their home server
        if (org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(uuid)) != null) return;

        // Update the profile directly if it's loaded (e.g., someone is viewing this OBO player's realm)
        // Try exact profileId first, then fallback to any profile owned by this player
        java.util.Optional<RealmProfile> profileOpt = ProfileManager.getProfile(profileId);
        if (profileOpt.isEmpty()) {
            java.util.List<RealmProfile> ownerProfiles = ProfileManager.getProfilesForOwner(uuid);
            if (!ownerProfiles.isEmpty()) {
                profileOpt = java.util.Optional.of(ownerProfiles.get(0));
            }
        }
        profileOpt.ifPresent(profile -> {
            profile.setCookies(CookieMath.parse(parts[4]));
            profile.setTotalCookiesEarned(CookieMath.parse(parts[5]));
            try { profile.setTimesClicked(Long.parseLong(parts[6])); } catch (NumberFormatException ignored) {}
            try { profile.setOwnerClicks(Long.parseLong(parts[7])); } catch (NumberFormatException ignored) {}
            try { profile.setOtherClicks(Long.parseLong(parts[8])); } catch (NumberFormatException ignored) {}
            profile.setUpgrades(RealmProfile.deserializeUpgrades(parts[9]));
            try { profile.setPrestigeLevel(Integer.parseInt(parts[10])); } catch (NumberFormatException ignored) {}
            profile.setAura(CookieMath.parse(parts[11]));
            try { profile.setRealmPublic(Integer.parseInt(parts[12]) != 0); } catch (NumberFormatException ignored) {}
            if (parts.length > 14) {
                profile.setPurchasedUpgrades(gg.drak.lobbyclicker.upgrades.ClickerUpgrade.deserialize(parts[14]));
            }
            if (parts.length > 15) {
                profile.setLifetimeCookiesEarned(CookieMath.parse(parts[15]));
            }
        });

        // Update player-level settings if loaded
        if (parts.length > 13) {
            PlayerManager.getPlayer(uuid).ifPresent(data -> {
                data.setSettings(new PlayerSettings(parts[13]));
            });
        }
    }

    /**
     * Handle CLICK: a visitor on another server clicked in a realm owned by a player on THIS server.
     * Only apply if the owner is online HERE (we are the home server).
     * Format: CLICK|serverId|ownerUuid|clickerUuid|clickerName
     */
    private static void handleRemoteClick(String[] parts) {
        if (parts.length < 5) return;

        String ownerUuid = parts[2];
        String clickerUuid = parts[3];
        String clickerName = parts[4];

        // Only apply if the owner is online on THIS server
        if (org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(ownerUuid)) == null) return;

        PlayerManager.getPlayer(ownerUuid).ifPresent(ownerData -> {
            if (!ownerData.isFullyLoaded()) return;

            ownerData.addCookies(ownerData.getCpc());
            ownerData.setTimesClicked(ownerData.getTimesClicked() + 1);

            // Track other clicks on the profile (remote clicks are always from visitors)
            RealmProfile profile = ownerData.getActiveProfile();
            if (profile != null) {
                profile.setOtherClicks(profile.getOtherClicks() + 1);
            }

            // Notify owner about remote click with friend-aware sounds
            ownerData.asPlayer().ifPresent(owner -> {
                boolean isFriend = ownerData.getFriends().contains(clickerUuid);
                SettingType st = isFriend ? SettingType.SOUND_FRIEND_CLICKER : SettingType.SOUND_RANDO_CLICKER;
                SettingType vt = isFriend ? SettingType.VOLUME_FRIEND_CLICKER : SettingType.VOLUME_RANDO_CLICKER;
                if (ownerData.getSettings().isSoundEnabled(st)) {
                    float vol = ownerData.getSettings().getVolume(vt);
                    owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, vol, 1.5f);
                }
            });
        });
    }

    /**
     * Handle SOUND: play a sound for a cur player on this server.
     * Format: SOUND|serverId|targetUuid|soundName|volume|pitch
     */
    /**
     * Handle BUY_UPGRADE: a visitor on another server wants to buy an upgrade for a realm owned by a cur player.
     * Only apply if the owner is online HERE (we are the home server).
     * Format: BUY_UPGRADE|serverId|ownerUuid|buyerUuid|upgradeTypeName
     */
    /**
     * Handle BUY_UPGRADE: a visitor on another conn server wants to buy an upgrade
     * for a realm owned by a cur player on THIS server.
     *
     * The home server checks if the owner can afford it and applies the purchase.
     * No sounds are sent back to the buyer — the buyer plays an optimistic buy sound
     * locally, and the next DATA_SYNC (within 1 second) will update the buyer's GUI
     * with the real values.
     */
    private static void handleRemoteBuyUpgrade(String[] parts) {
        if (parts.length < 5) return;

        String ownerUuid = parts[2];
        String upgradeTypeName = parts[4];

        // Only apply if the owner is online on THIS server (we are authoritative)
        if (org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(ownerUuid)) == null) return;

        PlayerManager.getPlayer(ownerUuid).ifPresent(ownerData -> {
            if (!ownerData.isFullyLoaded()) return;

            try {
                gg.drak.lobbyclicker.upgrades.UpgradeType type = gg.drak.lobbyclicker.upgrades.UpgradeType.valueOf(upgradeTypeName);
                if (ownerData.buyUpgrade(type)) {
                    // Notify the realm owner about the purchase
                    ownerData.asPlayer().ifPresent(owner -> {
                        if (ownerData.getSettings().isSoundEnabled(SettingType.SOUND_BUY)) {
                            float vol = ownerData.getSettings().getVolume(SettingType.VOLUME_BUY);
                            owner.playSound(owner.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, vol, 1.5f);
                        }
                    });
                }
                // No sounds sent back to buyer — they already played an optimistic sound.
                // The next DATA_SYNC will reflect the actual state.
            } catch (IllegalArgumentException ignored) {}
        });
    }

    private static void handleSound(String[] parts) {
        if (parts.length < 6) return;
        String targetUuid = parts[2];
        org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(targetUuid));
        if (target == null) return;
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(parts[3]);
            float volume = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);
            target.playSound(target.getLocation(), sound, volume, pitch);
        } catch (Throwable ignored) {}
    }

    /**
     * Handle MESSAGE: send a chat message to a cur player on this server.
     * Format: MESSAGE|serverId|targetUuid|message
     */
    private static void handleMessage(String[] parts) {
        if (parts.length < 4) return;
        String targetUuid = parts[2];
        org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(targetUuid));
        if (target == null) return;
        // Rejoin remaining parts in case message contained "|"
        StringBuilder msg = new StringBuilder(parts[3]);
        for (int i = 4; i < parts.length; i++) {
            msg.append("|").append(parts[i]);
        }
        target.sendMessage(msg.toString());
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
