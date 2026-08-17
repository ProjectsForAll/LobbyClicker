package gg.drak.lobbyclicker.achievements;

import gg.drak.lobbyclicker.upgrades.UpgradeType;
import lombok.Getter;
import org.bukkit.Material;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class Achievement {
    private final String id;
    private final String displayName;
    private final String description;
    private final Material material;
    private final AchievementType type;
    private final AchievementTier tier;
    private final BigDecimal requirement;
    private final UpgradeType building;
    private final int centennialCount;

    public Achievement(String id, String displayName, String description, Material material,
                       AchievementType type, AchievementTier tier, String requirement,
                       UpgradeType building, int centennialCount) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.material = material;
        this.type = type;
        this.tier = tier;
        this.requirement = new BigDecimal(requirement);
        this.building = building;
        this.centennialCount = centennialCount;
    }

    public String name() {
        return id;
    }

    public boolean isShadow() {
        return tier == AchievementTier.SHADOW;
    }

    public static Achievement[] values() {
        return AchievementCatalog.all().toArray(new Achievement[0]);
    }

    public static Achievement valueOf(String id) {
        Achievement found = AchievementCatalog.byId(id);
        if (found == null) throw new IllegalArgumentException(id);
        return found;
    }

    public static Set<Achievement> deserialize(String data) {
        Set<Achievement> set = new LinkedHashSet<>();
        if (data != null && !data.isEmpty()) {
            for (String name : data.split(",")) {
                Achievement a = AchievementCatalog.byId(name.trim());
                if (a != null) set.add(a);
                else {
                    Achievement mapped = AchievementCatalog.fromLegacyQuest(name.trim());
                    if (mapped != null) set.add(mapped);
                }
            }
        }
        return set;
    }

    public static String serialize(Collection<Achievement> achievements) {
        if (achievements == null || achievements.isEmpty()) return "";
        return achievements.stream().map(Achievement::getId).collect(Collectors.joining(","));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Achievement that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
