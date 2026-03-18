package gg.drak.lobbyclicker.settings;

import java.util.EnumMap;
import java.util.Map;

public class PlayerSettings {
    private final EnumMap<SettingType, Integer> values;

    public PlayerSettings() {
        values = new EnumMap<>(SettingType.class);
        for (SettingType type : SettingType.values()) {
            values.put(type, type.getDefaultValue());
        }
    }

    public PlayerSettings(String serialized) {
        this();
        if (serialized != null && !serialized.isEmpty()) {
            for (String part : serialized.split(";")) {
                String[] kv = part.split(":");
                if (kv.length == 2) {
                    try {
                        SettingType type = SettingType.valueOf(kv[0]);
                        values.put(type, Integer.parseInt(kv[1]));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    public int get(SettingType type) {
        return values.getOrDefault(type, type.getDefaultValue());
    }

    public boolean getBool(SettingType type) {
        return get(type) != 0;
    }

    public void set(SettingType type, int value) {
        values.put(type, value);
    }

    public void toggle(SettingType type) {
        set(type, getBool(type) ? 0 : 1);
    }

    public boolean isSoundEnabled(SettingType soundType) {
        return getBool(SettingType.SOUND_MASTER) && getBool(soundType);
    }

    /**
     * Get volume as a float (0.0 to 5.0).
     * Stored internally as int (0-500, representing value * 100).
     * Legacy values 0/1/2 are auto-converted (0->0, 1->50, 2->100).
     */
    public float getVolume(SettingType volumeType) {
        int val = get(volumeType);
        // Legacy migration: old values were 0, 1, or 2
        if (val >= 0 && val <= 2) {
            switch (val) {
                case 0: return 0.0f;
                case 1: return 0.5f;
                case 2: return 1.0f;
            }
        }
        return Math.max(0.0f, Math.min(5.0f, val / 100.0f));
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<SettingType, Integer> entry : values.entrySet()) {
            if (entry.getValue() != entry.getKey().getDefaultValue()) {
                if (sb.length() > 0) sb.append(";");
                sb.append(entry.getKey().name()).append(":").append(entry.getValue());
            }
        }
        return sb.toString();
    }
}
