package gg.drak.lobbyclicker.database;

import host.plas.bou.sql.ConnectorSet;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Statements {
    @Getter
    public enum MySQL {
        CREATE_DATABASE("CREATE DATABASE IF NOT EXISTS `%database%`;"),
        CREATE_TABLES(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%Players` ( " +
                "Uuid VARCHAR(36) NOT NULL, " +
                "Name VARCHAR(16) NOT NULL, " +
                "Settings TEXT NOT NULL DEFAULT '', " +
                "ActiveProfileId VARCHAR(36) NOT NULL DEFAULT '', " +
                "GlobalClicks BIGINT NOT NULL DEFAULT 0, " +
                "LastLogoutEpochMs BIGINT NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (Uuid) " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;"
        ),
        CREATE_PROFILES_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%Profiles` ( " +
                "ProfileId VARCHAR(36) NOT NULL, " +
                "OwnerUuid VARCHAR(36) NOT NULL, " +
                "ProfileName VARCHAR(32) NOT NULL DEFAULT 'Main', " +
                "Cookies TEXT NOT NULL DEFAULT '0', " +
                "TotalCookiesEarned TEXT NOT NULL DEFAULT '0', " +
                "TotalCookiesDigits INT NOT NULL DEFAULT 0, " +
                "TimesClicked BIGINT NOT NULL DEFAULT 0, " +
                "OwnerClicks BIGINT NOT NULL DEFAULT 0, " +
                "OtherClicks BIGINT NOT NULL DEFAULT 0, " +
                "Upgrades TEXT NOT NULL DEFAULT '', " +
                "PrestigeLevel INT NOT NULL DEFAULT 0, " +
                "Aura TEXT NOT NULL DEFAULT '0', " +
                "RealmPublic TINYINT NOT NULL DEFAULT 0, " +
                "PurchasedUpgrades TEXT NOT NULL DEFAULT '', " +
                "LifetimeCookiesEarned TEXT NOT NULL DEFAULT '0', " +
                "LifetimeCookiesDigits INT NOT NULL DEFAULT 0, " +
                "CompletedQuests TEXT NOT NULL DEFAULT '', " +
                "GoldenCookiesCollected BIGINT NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (ProfileId), " +
                "INDEX idx_owner (OwnerUuid) " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;"
        ),
        CREATE_PROFILE_ROLES_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%ProfileRoles` ( " +
                "ProfileId VARCHAR(36) NOT NULL, " +
                "PlayerUuid VARCHAR(36) NOT NULL, " +
                "Role VARCHAR(20) NOT NULL DEFAULT 'VISITOR', " +
                "PRIMARY KEY (ProfileId, PlayerUuid) " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;"
        ),
        CREATE_PROFILE_BANS_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%ProfileBans` ( " +
                "ProfileId VARCHAR(36) NOT NULL, " +
                "BannedUuid VARCHAR(36) NOT NULL, " +
                "IsShadowBan TINYINT NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (ProfileId, BannedUuid) " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;"
        ),
        CREATE_FRIENDS_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%Friends` ( " +
                "Uuid1 VARCHAR(36) NOT NULL, " +
                "Uuid2 VARCHAR(36) NOT NULL, " +
                "Since BIGINT NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (Uuid1, Uuid2) " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;"
        ),
        CREATE_FRIEND_REQUESTS_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%FriendRequests` ( " +
                "Sender VARCHAR(36) NOT NULL, " +
                "Receiver VARCHAR(36) NOT NULL, " +
                "SentAt BIGINT NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (Sender, Receiver) " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;"
        ),
        CREATE_BANS_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%Bans` ( " +
                "Owner VARCHAR(36) NOT NULL, " +
                "Banned VARCHAR(36) NOT NULL, " +
                "PRIMARY KEY (Owner, Banned) " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;"
        ),
        CREATE_BLOCKS_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%Blocks` ( " +
                "Owner VARCHAR(36) NOT NULL, " +
                "Blocked VARCHAR(36) NOT NULL, " +
                "PRIMARY KEY (Owner, Blocked) " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;"
        ),

        // Player CRUD (slim: just UUID, name, settings, active profile)
        PUSH_PLAYER_MAIN("INSERT INTO `%table_prefix%Players` ( " +
                "Uuid, Name, Settings, ActiveProfileId, GlobalClicks, LastLogoutEpochMs " +
                ") VALUES ( " +
                "?, ?, ?, ?, ?, ? " +
                ") ON DUPLICATE KEY UPDATE " +
                "Name = VALUES(Name), " +
                "Settings = VALUES(Settings), " +
                "ActiveProfileId = VALUES(ActiveProfileId), " +
                "GlobalClicks = VALUES(GlobalClicks), " +
                "LastLogoutEpochMs = VALUES(LastLogoutEpochMs)" +
                ";"),
        PULL_PLAYER_MAIN("SELECT * FROM `%table_prefix%Players` WHERE Uuid = ?;"),
        PLAYER_EXISTS("SELECT COUNT(*) FROM `%table_prefix%Players` WHERE Uuid = ?;"),
        PULL_ALL_PLAYERS("SELECT * FROM `%table_prefix%Players`;"),

        // Profile CRUD
        PUSH_PROFILE("INSERT INTO `%table_prefix%Profiles` ( " +
                "ProfileId, OwnerUuid, ProfileName, Cookies, TotalCookiesEarned, TotalCookiesDigits, " +
                "TimesClicked, OwnerClicks, OtherClicks, Upgrades, PrestigeLevel, Aura, RealmPublic, " +
                "PurchasedUpgrades, LifetimeCookiesEarned, LifetimeCookiesDigits, " +
                "CompletedQuests, GoldenCookiesCollected " +
                ") VALUES ( " +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? " +
                ") ON DUPLICATE KEY UPDATE " +
                "ProfileName = VALUES(ProfileName), " +
                "Cookies = VALUES(Cookies), " +
                "TotalCookiesEarned = VALUES(TotalCookiesEarned), " +
                "TotalCookiesDigits = VALUES(TotalCookiesDigits), " +
                "TimesClicked = VALUES(TimesClicked), " +
                "OwnerClicks = VALUES(OwnerClicks), " +
                "OtherClicks = VALUES(OtherClicks), " +
                "Upgrades = VALUES(Upgrades), " +
                "PrestigeLevel = VALUES(PrestigeLevel), " +
                "Aura = VALUES(Aura), " +
                "RealmPublic = VALUES(RealmPublic), " +
                "PurchasedUpgrades = VALUES(PurchasedUpgrades), " +
                "LifetimeCookiesEarned = VALUES(LifetimeCookiesEarned), " +
                "LifetimeCookiesDigits = VALUES(LifetimeCookiesDigits), " +
                "CompletedQuests = VALUES(CompletedQuests), " +
                "GoldenCookiesCollected = VALUES(GoldenCookiesCollected)" +
                ";"),
        PULL_PROFILE("SELECT * FROM `%table_prefix%Profiles` WHERE ProfileId = ?;"),
        PULL_PROFILES_BY_OWNER("SELECT * FROM `%table_prefix%Profiles` WHERE OwnerUuid = ?;"),
        DELETE_PROFILE("DELETE FROM `%table_prefix%Profiles` WHERE ProfileId = ?;"),
        PULL_LEADERBOARD("SELECT p.ProfileId, p.OwnerUuid, p.ProfileName, p.Cookies, p.TotalCookiesEarned, p.LifetimeCookiesEarned, p.PrestigeLevel, pl.Name " +
                "FROM `%table_prefix%Profiles` p LEFT JOIN `%table_prefix%Players` pl ON p.OwnerUuid = pl.Uuid " +
                "ORDER BY p.LifetimeCookiesDigits DESC, p.LifetimeCookiesEarned DESC LIMIT 500;"),

        // Profile Roles
        PUSH_PROFILE_ROLE("INSERT INTO `%table_prefix%ProfileRoles` (ProfileId, PlayerUuid, Role) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE Role = VALUES(Role);"),
        DELETE_PROFILE_ROLE("DELETE FROM `%table_prefix%ProfileRoles` WHERE ProfileId = ? AND PlayerUuid = ?;"),
        PULL_PROFILE_ROLES("SELECT PlayerUuid, Role FROM `%table_prefix%ProfileRoles` WHERE ProfileId = ?;"),

        // Profile Bans
        PUSH_PROFILE_BAN("INSERT IGNORE INTO `%table_prefix%ProfileBans` (ProfileId, BannedUuid, IsShadowBan) VALUES (?, ?, ?);"),
        DELETE_PROFILE_BAN("DELETE FROM `%table_prefix%ProfileBans` WHERE ProfileId = ? AND BannedUuid = ?;"),
        PULL_PROFILE_BANS("SELECT BannedUuid FROM `%table_prefix%ProfileBans` WHERE ProfileId = ?;"),

        // Friends
        PUSH_FRIEND("INSERT IGNORE INTO `%table_prefix%Friends` (Uuid1, Uuid2, Since) VALUES (?, ?, ?);"),
        DELETE_FRIEND("DELETE FROM `%table_prefix%Friends` WHERE Uuid1 = ? AND Uuid2 = ?;"),
        PULL_FRIENDS("SELECT Uuid2 FROM `%table_prefix%Friends` WHERE Uuid1 = ?;"),

        // Friend Requests
        PUSH_FRIEND_REQUEST("INSERT IGNORE INTO `%table_prefix%FriendRequests` (Sender, Receiver, SentAt) VALUES (?, ?, ?);"),
        DELETE_FRIEND_REQUEST("DELETE FROM `%table_prefix%FriendRequests` WHERE Sender = ? AND Receiver = ?;"),
        PULL_INCOMING_REQUESTS("SELECT Sender FROM `%table_prefix%FriendRequests` WHERE Receiver = ?;"),
        PULL_OUTGOING_REQUESTS("SELECT Receiver FROM `%table_prefix%FriendRequests` WHERE Sender = ?;"),

        // Legacy Bans (kept for migration, new bans use ProfileBans)
        PUSH_BAN("INSERT IGNORE INTO `%table_prefix%Bans` (Owner, Banned) VALUES (?, ?);"),
        DELETE_BAN("DELETE FROM `%table_prefix%Bans` WHERE Owner = ? AND Banned = ?;"),
        PULL_BANS("SELECT Banned FROM `%table_prefix%Bans` WHERE Owner = ?;"),

        // Blocks
        PUSH_BLOCK("INSERT IGNORE INTO `%table_prefix%Blocks` (Owner, Blocked) VALUES (?, ?);"),
        DELETE_BLOCK("DELETE FROM `%table_prefix%Blocks` WHERE Owner = ? AND Blocked = ?;"),
        PULL_BLOCKS("SELECT Blocked FROM `%table_prefix%Blocks` WHERE Owner = ?;"),
        ;

        private final String statement;

        MySQL(String statement) {
            this.statement = statement;
        }
    }

    @Getter
    public enum SQLite {
        CREATE_DATABASE(""),
        CREATE_TABLES(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%Players` ( " +
                "Uuid TEXT NOT NULL, " +
                "Name TEXT NOT NULL, " +
                "Settings TEXT NOT NULL DEFAULT '', " +
                "ActiveProfileId TEXT NOT NULL DEFAULT '', " +
                "GlobalClicks INTEGER NOT NULL DEFAULT 0, " +
                "LastLogoutEpochMs INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (Uuid) " +
                ");;"
        ),
        CREATE_PROFILES_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%Profiles` ( " +
                "ProfileId TEXT NOT NULL, " +
                "OwnerUuid TEXT NOT NULL, " +
                "ProfileName TEXT NOT NULL DEFAULT 'Main', " +
                "Cookies TEXT NOT NULL DEFAULT '0', " +
                "TotalCookiesEarned TEXT NOT NULL DEFAULT '0', " +
                "TotalCookiesDigits INTEGER NOT NULL DEFAULT 0, " +
                "TimesClicked INTEGER NOT NULL DEFAULT 0, " +
                "OwnerClicks INTEGER NOT NULL DEFAULT 0, " +
                "OtherClicks INTEGER NOT NULL DEFAULT 0, " +
                "Upgrades TEXT NOT NULL DEFAULT '', " +
                "PrestigeLevel INTEGER NOT NULL DEFAULT 0, " +
                "Aura TEXT NOT NULL DEFAULT '0', " +
                "RealmPublic INTEGER NOT NULL DEFAULT 0, " +
                "PurchasedUpgrades TEXT NOT NULL DEFAULT '', " +
                "LifetimeCookiesEarned TEXT NOT NULL DEFAULT '0', " +
                "LifetimeCookiesDigits INTEGER NOT NULL DEFAULT 0, " +
                "CompletedQuests TEXT NOT NULL DEFAULT '', " +
                "GoldenCookiesCollected INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (ProfileId) " +
                ");;"
        ),
        CREATE_PROFILE_ROLES_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%ProfileRoles` ( " +
                "ProfileId TEXT NOT NULL, " +
                "PlayerUuid TEXT NOT NULL, " +
                "Role TEXT NOT NULL DEFAULT 'VISITOR', " +
                "PRIMARY KEY (ProfileId, PlayerUuid) " +
                ");;"
        ),
        CREATE_PROFILE_BANS_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%ProfileBans` ( " +
                "ProfileId TEXT NOT NULL, " +
                "BannedUuid TEXT NOT NULL, " +
                "IsShadowBan INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (ProfileId, BannedUuid) " +
                ");;"
        ),
        CREATE_FRIENDS_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%Friends` ( " +
                "Uuid1 TEXT NOT NULL, " +
                "Uuid2 TEXT NOT NULL, " +
                "Since INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (Uuid1, Uuid2) " +
                ");;"
        ),
        CREATE_FRIEND_REQUESTS_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%FriendRequests` ( " +
                "Sender TEXT NOT NULL, " +
                "Receiver TEXT NOT NULL, " +
                "SentAt INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (Sender, Receiver) " +
                ");;"
        ),
        CREATE_BANS_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%Bans` ( " +
                "Owner TEXT NOT NULL, " +
                "Banned TEXT NOT NULL, " +
                "PRIMARY KEY (Owner, Banned) " +
                ");;"
        ),
        CREATE_BLOCKS_TABLE(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%Blocks` ( " +
                "Owner TEXT NOT NULL, " +
                "Blocked TEXT NOT NULL, " +
                "PRIMARY KEY (Owner, Blocked) " +
                ");;"
        ),

        // Player CRUD
        PUSH_PLAYER_MAIN("INSERT OR REPLACE INTO `%table_prefix%Players` ( " +
                "Uuid, Name, Settings, ActiveProfileId, GlobalClicks, LastLogoutEpochMs " +
                ") VALUES ( " +
                "?, ?, ?, ?, ?, ? " +
                ");"),
        PULL_PLAYER_MAIN("SELECT * FROM `%table_prefix%Players` WHERE Uuid = ?;"),
        PLAYER_EXISTS("SELECT COUNT(*) FROM `%table_prefix%Players` WHERE Uuid = ?;"),
        PULL_ALL_PLAYERS("SELECT * FROM `%table_prefix%Players`;"),

        // Profile CRUD
        PUSH_PROFILE("INSERT OR REPLACE INTO `%table_prefix%Profiles` ( " +
                "ProfileId, OwnerUuid, ProfileName, Cookies, TotalCookiesEarned, TotalCookiesDigits, " +
                "TimesClicked, OwnerClicks, OtherClicks, Upgrades, PrestigeLevel, Aura, RealmPublic, " +
                "PurchasedUpgrades, LifetimeCookiesEarned, LifetimeCookiesDigits, " +
                "CompletedQuests, GoldenCookiesCollected " +
                ") VALUES ( " +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? " +
                ");"),
        PULL_PROFILE("SELECT * FROM `%table_prefix%Profiles` WHERE ProfileId = ?;"),
        PULL_PROFILES_BY_OWNER("SELECT * FROM `%table_prefix%Profiles` WHERE OwnerUuid = ?;"),
        DELETE_PROFILE("DELETE FROM `%table_prefix%Profiles` WHERE ProfileId = ?;"),
        PULL_LEADERBOARD("SELECT p.ProfileId, p.OwnerUuid, p.ProfileName, p.Cookies, p.TotalCookiesEarned, p.LifetimeCookiesEarned, p.PrestigeLevel, pl.Name " +
                "FROM `%table_prefix%Profiles` p LEFT JOIN `%table_prefix%Players` pl ON p.OwnerUuid = pl.Uuid " +
                "ORDER BY p.LifetimeCookiesDigits DESC, p.LifetimeCookiesEarned DESC LIMIT 500;"),

        // Profile Roles
        PUSH_PROFILE_ROLE("INSERT OR REPLACE INTO `%table_prefix%ProfileRoles` (ProfileId, PlayerUuid, Role) VALUES (?, ?, ?);"),
        DELETE_PROFILE_ROLE("DELETE FROM `%table_prefix%ProfileRoles` WHERE ProfileId = ? AND PlayerUuid = ?;"),
        PULL_PROFILE_ROLES("SELECT PlayerUuid, Role FROM `%table_prefix%ProfileRoles` WHERE ProfileId = ?;"),

        // Profile Bans
        PUSH_PROFILE_BAN("INSERT OR IGNORE INTO `%table_prefix%ProfileBans` (ProfileId, BannedUuid, IsShadowBan) VALUES (?, ?, ?);"),
        DELETE_PROFILE_BAN("DELETE FROM `%table_prefix%ProfileBans` WHERE ProfileId = ? AND BannedUuid = ?;"),
        PULL_PROFILE_BANS("SELECT BannedUuid FROM `%table_prefix%ProfileBans` WHERE ProfileId = ?;"),

        // Friends
        PUSH_FRIEND("INSERT OR IGNORE INTO `%table_prefix%Friends` (Uuid1, Uuid2, Since) VALUES (?, ?, ?);"),
        DELETE_FRIEND("DELETE FROM `%table_prefix%Friends` WHERE Uuid1 = ? AND Uuid2 = ?;"),
        PULL_FRIENDS("SELECT Uuid2 FROM `%table_prefix%Friends` WHERE Uuid1 = ?;"),

        // Friend Requests
        PUSH_FRIEND_REQUEST("INSERT OR IGNORE INTO `%table_prefix%FriendRequests` (Sender, Receiver, SentAt) VALUES (?, ?, ?);"),
        DELETE_FRIEND_REQUEST("DELETE FROM `%table_prefix%FriendRequests` WHERE Sender = ? AND Receiver = ?;"),
        PULL_INCOMING_REQUESTS("SELECT Sender FROM `%table_prefix%FriendRequests` WHERE Receiver = ?;"),
        PULL_OUTGOING_REQUESTS("SELECT Receiver FROM `%table_prefix%FriendRequests` WHERE Sender = ?;"),

        // Legacy Bans
        PUSH_BAN("INSERT OR IGNORE INTO `%table_prefix%Bans` (Owner, Banned) VALUES (?, ?);"),
        DELETE_BAN("DELETE FROM `%table_prefix%Bans` WHERE Owner = ? AND Banned = ?;"),
        PULL_BANS("SELECT Banned FROM `%table_prefix%Bans` WHERE Owner = ?;"),

        // Blocks
        PUSH_BLOCK("INSERT OR IGNORE INTO `%table_prefix%Blocks` (Owner, Blocked) VALUES (?, ?);"),
        DELETE_BLOCK("DELETE FROM `%table_prefix%Blocks` WHERE Owner = ? AND Blocked = ?;"),
        PULL_BLOCKS("SELECT Blocked FROM `%table_prefix%Blocks` WHERE Owner = ?;"),
        ;

        private final String statement;

        SQLite(String statement) {
            this.statement = statement;
        }
    }

    public enum StatementType {
        CREATE_DATABASE,
        CREATE_TABLES,
        CREATE_PROFILES_TABLE,
        CREATE_PROFILE_ROLES_TABLE,
        CREATE_PROFILE_BANS_TABLE,
        CREATE_FRIENDS_TABLE,
        CREATE_FRIEND_REQUESTS_TABLE,
        CREATE_BANS_TABLE,
        CREATE_BLOCKS_TABLE,
        PUSH_PLAYER_MAIN,
        PULL_PLAYER_MAIN,
        PLAYER_EXISTS,
        PULL_ALL_PLAYERS,
        PUSH_PROFILE,
        PULL_PROFILE,
        PULL_PROFILES_BY_OWNER,
        DELETE_PROFILE,
        PULL_LEADERBOARD,
        PUSH_PROFILE_ROLE,
        DELETE_PROFILE_ROLE,
        PULL_PROFILE_ROLES,
        PUSH_PROFILE_BAN,
        DELETE_PROFILE_BAN,
        PULL_PROFILE_BANS,
        PUSH_FRIEND,
        DELETE_FRIEND,
        PULL_FRIENDS,
        PUSH_FRIEND_REQUEST,
        DELETE_FRIEND_REQUEST,
        PULL_INCOMING_REQUESTS,
        PULL_OUTGOING_REQUESTS,
        PUSH_BAN,
        DELETE_BAN,
        PULL_BANS,
        PUSH_BLOCK,
        DELETE_BLOCK,
        PULL_BLOCKS,
        ;
    }

    public static String getStatement(StatementType type, ConnectorSet connectorSet) {
        switch (connectorSet.getType()) {
            case MYSQL:
                return MySQL.valueOf(type.name()).getStatement()
                        .replace("%database%", connectorSet.getDatabase())
                        .replace("%table_prefix%", connectorSet.getTablePrefix());
            case SQLITE:
                return SQLite.valueOf(type.name()).getStatement()
                        .replace("%table_prefix%", connectorSet.getTablePrefix());
            default:
                return "";
        }
    }
}
