package gg.drak.lobbyclicker.settings;

import lombok.Getter;

@Getter
public enum SettingType {
    // Sound toggles (boolean: 0 = off, 1 = on)
    SOUND_MASTER(1),
    SOUND_CLICKER(0),
    SOUND_MILESTONE_CURRENT(1),
    SOUND_MILESTONE_TOTAL(1),
    SOUND_MILESTONE_ENTROPY(1),
    SOUND_BUY(1),
    SOUND_FRIEND_REQUEST(1),
    SOUND_FRIEND_JOIN(1),
    SOUND_FRIEND_LEAVE(1),
    SOUND_RANDO_JOIN(0),
    SOUND_RANDO_LEAVE(0),
    SOUND_FRIEND_CLICKER(1),
    SOUND_RANDO_CLICKER(0),

    // Volumes (int: 0, 1, or 2)
    VOLUME_CLICKER(1),
    VOLUME_MILESTONE_CURRENT(1),
    VOLUME_MILESTONE_TOTAL(1),
    VOLUME_MILESTONE_ENTROPY(1),
    VOLUME_BUY(1),
    VOLUME_FRIEND_REQUEST(1),
    VOLUME_FRIEND_JOIN(1),
    VOLUME_FRIEND_LEAVE(1),
    VOLUME_RANDO_JOIN(1),
    VOLUME_RANDO_LEAVE(1),
    VOLUME_FRIEND_CLICKER(1),
    VOLUME_RANDO_CLICKER(1),

    // Other settings (boolean: 0 = off, 1 = on)
    ALLOW_FRIEND_REQUESTS(1),
    AUTO_ACCEPT_FRIENDS(0),
    PUBLIC_FARM(0),
    ALLOW_FRIEND_JOINS(1),
    ALLOW_OFFLINE_REALM(0),
    ;

    private final int defaultValue;

    SettingType(int defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isSound() {
        return name().startsWith("SOUND_");
    }

    public boolean isVolume() {
        return name().startsWith("VOLUME_");
    }

    public boolean isOther() {
        return !isSound() && !isVolume();
    }

    public String displayName() {
        String raw = name().replace("SOUND_", "").replace("VOLUME_", "");
        StringBuilder sb = new StringBuilder();
        for (String part : raw.split("_")) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
