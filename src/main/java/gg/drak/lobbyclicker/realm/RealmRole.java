package gg.drak.lobbyclicker.realm;

import lombok.Getter;

@Getter
public enum RealmRole {
    VISITOR("Visitor"),
    GARDENER("Gardener"),
    MODERATOR("Moderator"),
    ADMIN("Admin");

    private final String displayName;

    RealmRole(String displayName) {
        this.displayName = displayName;
    }

    public boolean canBuyUpgrades() {
        return ordinal() >= GARDENER.ordinal();
    }

    public boolean canBan() {
        return ordinal() >= MODERATOR.ordinal();
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
