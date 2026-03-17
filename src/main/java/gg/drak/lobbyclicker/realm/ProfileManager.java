package gg.drak.lobbyclicker.realm;

import gg.drak.lobbyclicker.LobbyClicker;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages RealmProfile instances in memory.
 * Profiles are loaded when their owner joins (or when someone visits them).
 */
public class ProfileManager {
    @Getter @Setter
    private static int maxProfilesPerPlayer = 3;

    // All currently loaded profiles, keyed by profileId
    private static final ConcurrentHashMap<String, RealmProfile> loadedProfiles = new ConcurrentHashMap<>();

    public static void loadProfile(RealmProfile profile) {
        loadedProfiles.put(profile.getProfileId(), profile);
    }

    public static void unloadProfile(String profileId) {
        loadedProfiles.remove(profileId);
    }

    public static Optional<RealmProfile> getProfile(String profileId) {
        if (profileId == null) return Optional.empty();
        return Optional.ofNullable(loadedProfiles.get(profileId));
    }

    public static List<RealmProfile> getProfilesForOwner(String ownerUuid) {
        return loadedProfiles.values().stream()
                .filter(p -> p.getOwnerUuid().equals(ownerUuid))
                .collect(Collectors.toList());
    }

    public static boolean canCreateProfile(String ownerUuid) {
        long count = loadedProfiles.values().stream()
                .filter(p -> p.getOwnerUuid().equals(ownerUuid))
                .count();
        return count < maxProfilesPerPlayer;
    }

    public static RealmProfile createProfile(String ownerUuid, String name) {
        String profileId = UUID.randomUUID().toString();
        RealmProfile profile = new RealmProfile(profileId, ownerUuid, name);
        loadProfile(profile);
        return profile;
    }

    public static void unloadAllForOwner(String ownerUuid) {
        loadedProfiles.entrySet().removeIf(e -> e.getValue().getOwnerUuid().equals(ownerUuid));
    }

    public static Collection<RealmProfile> getAllLoadedProfiles() {
        return loadedProfiles.values();
    }
}
