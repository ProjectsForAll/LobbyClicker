package gg.drak.lobbyclicker.upgrades;

public enum ClickerUpgradeEffect {
    CPC_MULTIPLIER,
    CPS_MULTIPLIER,
    BUILDING_MULTIPLIER,
    GOLDEN_FREQ_MULTIPLIER,
    GOLDEN_REWARD_MULTIPLIER,
    GOLDEN_DURATION_MULTIPLIER,
    /** A flag, not a multiplier: presence of any upgrade with this effect enables the behavior. */
    GOLDEN_AUTO_COLLECT,
    FINGER_ADDITIVE,
    FINGER_MULTIPLIER,
    SYNERGY
}
