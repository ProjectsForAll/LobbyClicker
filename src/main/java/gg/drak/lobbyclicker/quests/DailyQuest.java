package gg.drak.lobbyclicker.quests;

import gg.drak.lobbyclicker.realm.RealmProfile;
import lombok.Getter;
import org.bukkit.Material;

import java.math.BigDecimal;

/**
 * A dynamically generated daily quest. Unlike {@link Quest}, daily quests are not
 * an enum -- they are created fresh each day by {@link DailyQuestManager}.
 * Daily quests reward cookies only (no permanent effects).
 * Progress is tracked relative to a baseline snapshot taken when the player
 * first opens the quest GUI each day.
 */
@Getter
public class DailyQuest {
    private final String id;           // unique identifier for serialization (e.g. "CLICKS_500_20260319")
    private final String displayName;
    private final String description;
    private final Material material;
    private final BigDecimal reward;
    private final QuestType type;
    private final long requirement;

    public DailyQuest(String id, String displayName, String description, Material material,
                      BigDecimal reward, QuestType type, long requirement) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.material = material;
        this.reward = reward;
        this.type = type;
        this.requirement = requirement;
    }

    /**
     * Get the player's progress toward this daily quest.
     * For accumulative types (clicks, cookies, golden cookies), progress is measured
     * from the baseline snapshot taken when the player first opened the quest GUI today.
     * For current-value types (CPS, entropy), the raw current value is used.
     */
    public long getProgress(RealmProfile profile) {
        long currentValue;
        switch (type) {
            case CLICKS:
                currentValue = profile.getTimesClicked();
                break;
            case LIFETIME_COOKIES:
                currentValue = profile.getLifetimeCookiesEarned().longValue();
                break;
            case GOLDEN_COOKIES:
                currentValue = profile.getGoldenCookiesCollected();
                break;
            case CPS:
                currentValue = profile.getCps().longValue();
                break;
            case ENTROPY:
                currentValue = profile.getClickerEntropy().longValue();
                break;
            default:
                currentValue = 0;
        }
        // For accumulative types, use daily progress (current - baseline)
        if (type == QuestType.CLICKS || type == QuestType.LIFETIME_COOKIES || type == QuestType.GOLDEN_COOKIES) {
            return DailyQuestManager.getDailyProgress(profile.getOwnerUuid(), type, currentValue);
        }
        return currentValue;
    }

    /**
     * Whether this daily quest's requirement is met.
     */
    public boolean isCompleted(RealmProfile profile) {
        return getProgress(profile) >= requirement;
    }
}
