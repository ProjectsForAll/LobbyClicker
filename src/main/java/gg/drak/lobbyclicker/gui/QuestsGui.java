package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.quests.DailyQuest;
import gg.drak.lobbyclicker.quests.DailyQuestManager;
import gg.drak.lobbyclicker.quests.Quest;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class QuestsGui extends PaginationMonitor {
    private final PlayerData data;

    public QuestsGui(Player player, PlayerData data) {
        this(player, data, 0);
    }

    public QuestsGui(Player player, PlayerData data, int page) {
        super(player, "quests", MonitorStyle.title("light_purple", "Quests"), page);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new ClickerGui(p, data).open());

        RealmProfile profile = data.getActiveProfile();
        if (profile == null) {
            addItem(4, GuiHelper.createIcon(Material.BARRIER,
                    ChatColor.RED + "" + ChatColor.BOLD + "Profile unavailable",
                    "",
                    ChatColor.GRAY + "Your realm profile is not loaded yet.",
                    ChatColor.GRAY + "Close and open the clicker again in a moment.",
                    "",
                    ChatColor.YELLOW + "If this persists, rejoin the server."));
            return;
        }

        // Initialize daily quest system
        DailyQuestManager.checkAndRefresh();
        DailyQuestManager.ensureBaseline(data.getIdentifier(), profile);
        List<DailyQuest> dailies = DailyQuestManager.getTodayQuests();

        // Sort permanent quests: claimable first, then in-progress, then claimed
        List<Quest> sortedQuests = new ArrayList<>(Arrays.asList(Quest.values()));
        sortedQuests.sort(Comparator.comparingInt(q -> {
            boolean completed = q.isCompleted(profile);
            boolean claimed = profile.hasCompletedQuest(q);
            if (completed && !claimed) return 0; // claimable
            if (!completed) return 1;             // in-progress
            return 2;                              // claimed
        }));

        // Build combined display list: dailies first, then permanent quests
        List<Object> displayItems = new ArrayList<>();
        displayItems.addAll(dailies);
        displayItems.addAll(sortedQuests);

        populatePagedContent(displayItems, (item, slot) -> {
            if (item instanceof DailyQuest) {
                buildDailyQuestIcon((DailyQuest) item, slot, profile);
            } else {
                buildPermanentQuestIcon((Quest) item, slot, profile);
            }
        });

        addPaginationArrows(displayItems, newPage -> new QuestsGui(player, data, newPage).open());
    }

    private void buildDailyQuestIcon(DailyQuest dq, int slot, RealmProfile profile) {
        boolean completed = dq.isCompleted(profile);
        boolean claimed = DailyQuestManager.hasClaimed(data.getIdentifier(), dq.getId());
        long progress = dq.getProgress(profile);
        long req = dq.getRequirement();

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + dq.getDescription());
        lore.add("");

        if (claimed) {
            lore.add(ChatColor.GOLD + "Claimed!");
        } else if (completed) {
            lore.add(ChatColor.GREEN + "Completed! " + FormatUtils.format(progress) + "/" + FormatUtils.format(req));
        } else {
            double pct = Math.min(100.0, (double) progress / req * 100.0);
            lore.add(ChatColor.YELLOW + "Progress: " + FormatUtils.format(progress) + "/" + FormatUtils.format(req)
                    + ChatColor.GRAY + " (" + String.format("%.1f", pct) + "%)");
            int barLen = 20;
            int filled = (int) (barLen * Math.min(1.0, (double) progress / req));
            StringBuilder bar = new StringBuilder(ChatColor.GREEN + "");
            for (int i = 0; i < barLen; i++) {
                if (i == filled) bar.append(ChatColor.GRAY);
                bar.append("|");
            }
            lore.add(bar.toString());
        }

        lore.add("");
        lore.add(ChatColor.AQUA + "Reward: " + ChatColor.WHITE + FormatUtils.format(dq.getReward()) + " cookies");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Resets daily at midnight");

        if (completed && !claimed) {
            lore.add("");
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Click to claim!");
        }

        ChatColor nameColor;
        if (claimed) {
            nameColor = ChatColor.GOLD;
        } else if (completed) {
            nameColor = ChatColor.GREEN;
        } else {
            nameColor = ChatColor.YELLOW;
        }

        Icon icon = GuiHelper.createIcon(Material.CLOCK,
                nameColor + "" + ChatColor.BOLD + "[Daily] " + dq.getDisplayName(),
                lore.toArray(new String[0]));

        if (completed && !claimed) {
            icon.onClick(e -> {
                Player p = (Player) e.getWhoClicked();
                if (!dq.isCompleted(profile) || DailyQuestManager.hasClaimed(data.getIdentifier(), dq.getId())) return;
                DailyQuestManager.claim(data.getIdentifier(), dq.getId());
                profile.addCookies(dq.getReward());
                p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Daily quest complete! " +
                        ChatColor.YELLOW + dq.getDisplayName() + ChatColor.GREEN + " - +" +
                        FormatUtils.format(dq.getReward()) + " cookies!");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                new QuestsGui(p, data, page).open();
            });
        }

        addItem(slot, icon);
    }

    private void buildPermanentQuestIcon(Quest quest, int slot, RealmProfile profile) {
        boolean completed = quest.isCompleted(profile);
        boolean claimed = profile.hasCompletedQuest(quest);
        long progress = quest.getProgress(profile);
        long req = quest.getRequirement();

        // Build lore
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + quest.getDescription());
        lore.add("");

        // Progress line
        if (claimed) {
            lore.add(ChatColor.GOLD + "Claimed!");
        } else if (completed) {
            lore.add(ChatColor.GREEN + "Completed! " + FormatUtils.format(progress) + "/" + FormatUtils.format(req));
        } else {
            double pct = Math.min(100.0, (double) progress / req * 100.0);
            lore.add(ChatColor.YELLOW + "Progress: " + FormatUtils.format(progress) + "/" + FormatUtils.format(req)
                    + ChatColor.GRAY + " (" + String.format("%.1f", pct) + "%)");
            // Progress bar
            int barLen = 20;
            int filled = (int) (barLen * Math.min(1.0, (double) progress / req));
            StringBuilder bar = new StringBuilder(ChatColor.GREEN + "");
            for (int i = 0; i < barLen; i++) {
                if (i == filled) bar.append(ChatColor.GRAY);
                bar.append("|");
            }
            lore.add(bar.toString());
        }

        lore.add("");
        lore.add(ChatColor.AQUA + "Reward: " + ChatColor.WHITE + FormatUtils.format(quest.getReward()) + " cookies");
        lore.add(ChatColor.LIGHT_PURPLE + "Permanent: " + ChatColor.WHITE + quest.getEffectDescription());

        if (completed && !claimed) {
            lore.add("");
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Click to claim!");
        }

        // Icon material and name color
        ChatColor nameColor;
        Material iconMat = quest.getMaterial();
        if (claimed) {
            nameColor = ChatColor.GOLD;
        } else if (completed) {
            nameColor = ChatColor.GREEN;
        } else {
            nameColor = ChatColor.YELLOW;
        }

        Icon icon = GuiHelper.createIcon(iconMat,
                nameColor + "" + ChatColor.BOLD + quest.getDisplayName(),
                lore.toArray(new String[0]));

        if (completed && !claimed) {
            icon.onClick(e -> {
                Player p = (Player) e.getWhoClicked();
                // Double-check still claimable
                if (!quest.isCompleted(profile) || profile.hasCompletedQuest(quest)) return;
                profile.completeQuest(quest);
                profile.addCookies(quest.getReward());
                p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Quest complete! " +
                        ChatColor.YELLOW + quest.getDisplayName() + ChatColor.GREEN + " - +" +
                        FormatUtils.format(quest.getReward()) + " cookies!");
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                // Reopen to refresh
                new QuestsGui(p, data, page).open();
            });
        }

        addItem(slot, icon);
    }
}
