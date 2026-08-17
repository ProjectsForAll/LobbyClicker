package gg.drak.lobbyclicker.upgrades;

import gg.drak.lobbyclicker.realm.RealmProfile;
import lombok.Getter;
import org.bukkit.Material;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One-time Cookie Clicker upgrade. Instances are created by {@link ClickerUpgradeCatalog}.
 */
@Getter
public class ClickerUpgrade {
    private final String id;
    private final String displayName;
    private final String description;
    private final Material material;
    private final BigDecimal cost;
    private final ClickerUpgradeEffect effect;
    private final BigDecimal effectValue;
    private final UpgradeType targetBuilding;
    private final UpgradeType synergyPartner;
    private final int requiredCount;

    public ClickerUpgrade(String id, String displayName, String description, Material material,
                          String cost, ClickerUpgradeEffect effect, String effectValue,
                          UpgradeType targetBuilding, int requiredCount) {
        this(id, displayName, description, material, cost, effect, effectValue, targetBuilding, null, requiredCount);
    }

    public ClickerUpgrade(String id, String displayName, String description, Material material,
                          String cost, ClickerUpgradeEffect effect, String effectValue,
                          UpgradeType targetBuilding, UpgradeType synergyPartner, int requiredCount) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.material = material;
        this.cost = new BigDecimal(cost);
        this.effect = effect;
        this.effectValue = new BigDecimal(effectValue);
        this.targetBuilding = targetBuilding;
        this.synergyPartner = synergyPartner;
        this.requiredCount = requiredCount;
    }

    public String name() {
        return id;
    }

    public boolean isUnlocked(RealmProfile profile) {
        if (profile == null) return false;
        if (requiredCount <= 0) return true;
        if (effect == ClickerUpgradeEffect.CPC_MULTIPLIER && targetBuilding == null) {
            return profile.getTimesClicked() >= requiredCount
                    || profile.getCookiesFromClicks().compareTo(BigDecimal.valueOf(requiredCount)) >= 0;
        }
        if (targetBuilding != null) {
            return profile.getUpgradeCount(targetBuilding) >= requiredCount;
        }
        return true;
    }

    public boolean isHidden(RealmProfile profile) {
        if (targetBuilding == null) return false;
        if (profile == null) return true;
        return profile.getUpgradeCount(targetBuilding) == 0
                && profile.getTotalCookiesEarned().compareTo(targetBuilding.getBaseCost()) < 0;
    }

    public static ClickerUpgrade[] values() {
        return ClickerUpgradeCatalog.all().toArray(new ClickerUpgrade[0]);
    }

    public static ClickerUpgrade valueOf(String id) {
        ClickerUpgrade found = ClickerUpgradeCatalog.byId(id);
        if (found == null) throw new IllegalArgumentException(id);
        return found;
    }

    public static Set<ClickerUpgrade> deserialize(String data) {
        Set<ClickerUpgrade> set = new LinkedHashSet<>();
        if (data != null && !data.isEmpty()) {
            for (String name : data.split(",")) {
                ClickerUpgrade upgrade = ClickerUpgradeCatalog.byId(name.trim());
                if (upgrade != null) set.add(upgrade);
            }
        }
        return set;
    }

    public static String serialize(Collection<ClickerUpgrade> upgrades) {
        if (upgrades == null || upgrades.isEmpty()) return "";
        return upgrades.stream().map(ClickerUpgrade::getId).collect(Collectors.joining(","));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClickerUpgrade that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
