package gg.drak.lobbyclicker.hooks;

import gg.drak.lobbyclicker.LobbyClicker;
import gg.drak.friendsprovider.api.FriendsAPI;
import lombok.Getter;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Bridge to the optional FriendsProvider plugin.
 * When enabled (config + plugin present), all friend/request operations
 * delegate to FriendsAPI instead of the built-in DB + Redis approach.
 */
public class FriendsProviderHook {
    @Getter
    private static boolean hooked = false;

    /**
     * Called on plugin enable. Activates the hook only if both the config
     * toggle is on AND the FriendsProvider plugin is loaded.
     */
    public static void init() {
        boolean configEnabled = LobbyClicker.getMainConfig().isFriendsProviderEnabled();
        boolean pluginPresent = org.bukkit.Bukkit.getPluginManager().getPlugin("FriendsProvider") != null;
        hooked = configEnabled && pluginPresent;

        if (configEnabled && !pluginPresent) {
            LobbyClicker.getInstance().logWarning("friends-provider.enabled is true but FriendsProvider plugin is not loaded!");
        }
        if (hooked) {
            LobbyClicker.getInstance().logInfo("Hooked into FriendsProvider for friends management.");
        }
    }

    // ===================== LOADING =====================

    public static CompletableFuture<Set<String>> loadFriends(String uuid) {
        return FriendsAPI.getFriendsAsync(UUID.fromString(uuid))
                .thenApply(uuids -> uuids.stream().map(UUID::toString).collect(Collectors.toSet()));
    }

    public static CompletableFuture<Set<String>> loadIncomingRequests(String uuid) {
        return FriendsAPI.getPendingReceivedAsync(UUID.fromString(uuid))
                .thenApply(uuids -> uuids.stream().map(UUID::toString).collect(Collectors.toSet()));
    }

    public static CompletableFuture<Set<String>> loadOutgoingRequests(String uuid) {
        return FriendsAPI.getPendingSentAsync(UUID.fromString(uuid))
                .thenApply(uuids -> uuids.stream().map(UUID::toString).collect(Collectors.toSet()));
    }

    // ===================== MUTATIONS =====================

    public static void addFriend(String uuid1, String uuid2) {
        FriendsAPI.acceptRequest(UUID.fromString(uuid1), UUID.fromString(uuid2));
    }

    public static void removeFriend(String uuid1, String uuid2) {
        FriendsAPI.removeFriend(UUID.fromString(uuid1), UUID.fromString(uuid2));
    }

    public static void sendRequest(String sender, String receiver) {
        FriendsAPI.sendRequest(UUID.fromString(sender), UUID.fromString(receiver));
    }

    public static void acceptRequest(String sender, String receiver) {
        FriendsAPI.acceptRequest(UUID.fromString(sender), UUID.fromString(receiver));
    }

    public static void denyRequest(String sender, String receiver) {
        FriendsAPI.denyRequest(UUID.fromString(sender), UUID.fromString(receiver));
    }

    public static void retractRequest(String sender, String receiver) {
        FriendsAPI.retractRequest(UUID.fromString(sender), UUID.fromString(receiver));
    }
}
