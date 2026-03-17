package gg.drak.lobbyclicker.social;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.data.PlayerManager;
import gg.drak.lobbyclicker.settings.SettingType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RealmManager {
    // ownerUuid -> set of viewer UUIDs
    private static final ConcurrentHashMap<String, Set<String>> REALM_VIEWERS = new ConcurrentHashMap<>();

    public static void addViewer(String ownerUuid, String viewerUuid) {
        if (ownerUuid.equals(viewerUuid)) return; // viewing own realm
        REALM_VIEWERS.computeIfAbsent(ownerUuid, k -> ConcurrentHashMap.newKeySet()).add(viewerUuid);

        // Notify owner
        notifyOwner(ownerUuid, viewerUuid, true);
    }

    public static void removeViewer(String ownerUuid, String viewerUuid) {
        Set<String> viewers = REALM_VIEWERS.get(ownerUuid);
        if (viewers != null) {
            viewers.remove(viewerUuid);
            if (viewers.isEmpty()) REALM_VIEWERS.remove(ownerUuid);
        }

        // Notify owner
        notifyOwner(ownerUuid, viewerUuid, false);
    }

    public static Set<String> getViewers(String ownerUuid) {
        Set<String> viewers = REALM_VIEWERS.get(ownerUuid);
        return viewers != null ? Collections.unmodifiableSet(viewers) : Collections.emptySet();
    }

    public static void removeAllViewersOf(String ownerUuid) {
        REALM_VIEWERS.remove(ownerUuid);
    }

    public static void removeViewerFromAll(String viewerUuid) {
        REALM_VIEWERS.values().forEach(set -> set.remove(viewerUuid));
        REALM_VIEWERS.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public static String getViewingRealm(String viewerUuid) {
        for (Map.Entry<String, Set<String>> entry : REALM_VIEWERS.entrySet()) {
            if (entry.getValue().contains(viewerUuid)) return entry.getKey();
        }
        return null;
    }

    private static void notifyOwner(String ownerUuid, String viewerUuid, boolean joined) {
        Player owner = Bukkit.getPlayer(UUID.fromString(ownerUuid));
        if (owner == null) return;

        PlayerData ownerData = PlayerManager.getPlayer(ownerUuid).orElse(null);
        if (ownerData == null) return;

        boolean isFriend = ownerData.getFriends().contains(viewerUuid);

        SettingType soundType;
        SettingType volumeType;
        if (joined) {
            soundType = isFriend ? SettingType.SOUND_FRIEND_JOIN : SettingType.SOUND_RANDO_JOIN;
            volumeType = isFriend ? SettingType.VOLUME_FRIEND_JOIN : SettingType.VOLUME_RANDO_JOIN;
        } else {
            soundType = isFriend ? SettingType.SOUND_FRIEND_LEAVE : SettingType.SOUND_RANDO_LEAVE;
            volumeType = isFriend ? SettingType.VOLUME_FRIEND_LEAVE : SettingType.VOLUME_RANDO_LEAVE;
        }

        if (ownerData.getSettings().isSoundEnabled(soundType)) {
            float vol = ownerData.getSettings().getVolume(volumeType);
            Sound sound = joined ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.ENTITY_ITEM_PICKUP;
            owner.playSound(owner.getLocation(), sound, vol, joined ? 1.2f : 0.8f);
        }

        // Get viewer name
        String viewerName = viewerUuid;
        PlayerData viewerData = PlayerManager.getPlayer(viewerUuid).orElse(null);
        if (viewerData != null) viewerName = viewerData.getName();

        String prefix = isFriend ? "\u00a7a[Friend] " : "\u00a77";
        String action = joined ? " joined your realm." : " left your realm.";
        owner.sendMessage(prefix + viewerName + "\u00a77" + action);
    }
}
