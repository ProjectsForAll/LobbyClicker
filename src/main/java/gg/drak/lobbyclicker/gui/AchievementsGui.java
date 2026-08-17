package gg.drak.lobbyclicker.gui;

import gg.drak.lobbyclicker.achievements.Achievement;
import gg.drak.lobbyclicker.achievements.AchievementCatalog;
import gg.drak.lobbyclicker.achievements.AchievementManager;
import gg.drak.lobbyclicker.data.PlayerData;
import gg.drak.lobbyclicker.gui.monitor.MonitorStyle;
import gg.drak.lobbyclicker.gui.monitor.PaginationMonitor;
import gg.drak.lobbyclicker.realm.RealmProfile;
import gg.drak.lobbyclicker.utils.FormatUtils;
import mc.obliviate.inventory.Icon;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.ArrayList;
import java.util.List;

public class AchievementsGui extends PaginationMonitor {
    private final PlayerData data;

    public AchievementsGui(Player player, PlayerData data) {
        this(player, data, 0);
    }

    public AchievementsGui(Player player, PlayerData data, int page) {
        super(player, "achievements", MonitorStyle.title("gold", "Achievements"), page);
        this.data = data;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        setPlayerContext(data, null);
        fillMonitorBorder();
        buildStandardActionBar(p -> new ClickerGui(p, data).open());

        RealmProfile profile = data.getActiveProfile();
        int unlocked = profile == null ? 0 : AchievementManager.normalUnlocked(profile);
        int total = 0;
        for (Achievement a : AchievementCatalog.all()) {
            if (!a.isShadow()) total++;
        }
        addItem(4, ClickerGuiHelper.createIcon(Material.MILK_BUCKET,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Milk",
                "",
                ChatColor.GRAY + "Achievements: " + ChatColor.WHITE + unlocked + "/" + total,
                ChatColor.GRAY + "Milk bonus: " + ChatColor.WHITE + "+" + (unlocked * 4) + "% CPS",
                "",
                ChatColor.DARK_GRAY + "Shadow achievements do not grant milk."));

        List<Achievement> list = new ArrayList<>(AchievementCatalog.all());
        list.sort((a, b) -> {
            boolean au = profile != null && profile.hasCompletedAchievement(a);
            boolean bu = profile != null && profile.hasCompletedAchievement(b);
            if (au != bu) return au ? 1 : -1;
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });

        populatePagedContent(list, (achievement, slot) -> addItem(slot, iconFor(achievement, profile)));
        addPaginationArrows(list, newPage -> new AchievementsGui(player, data, newPage).open());
    }

    private Icon iconFor(Achievement achievement, RealmProfile profile) {
        boolean done = profile != null && profile.hasCompletedAchievement(achievement);
        if (achievement.getType() == gg.drak.lobbyclicker.achievements.AchievementType.HERE_YOU_GO && !done) {
            Icon icon = ClickerGuiHelper.createIcon(Material.MAP,
                    ChatColor.YELLOW + "" + ChatColor.BOLD + achievement.getDisplayName(),
                    "", ChatColor.GRAY + achievement.getDescription(),
                    "", ChatColor.YELLOW + "Click this slot.");
            icon.onClick(e -> {
                AchievementManager.unlockHereYouGo(data);
                new AchievementsGui(player, data, page).open();
            });
            return icon;
        }
        Material mat = done ? achievement.getMaterial() : Material.GRAY_DYE;
        String color = done ? (achievement.isShadow() ? ChatColor.DARK_GRAY.toString() : ChatColor.GOLD.toString())
                : ChatColor.DARK_GRAY.toString();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + achievement.getDescription());
        lore.add("");
        lore.add(achievement.isShadow() ? ChatColor.DARK_GRAY + "Shadow" : ChatColor.AQUA + "+4% milk CPS");
        lore.add(done ? ChatColor.GREEN + "Unlocked" : ChatColor.RED + "Locked");
        if (profile != null && !done) {
            lore.add(ChatColor.GRAY + "Need: " + ChatColor.WHITE + FormatUtils.format(achievement.getRequirement()));
        }
        return ClickerGuiHelper.createIcon(mat, color + ChatColor.BOLD + achievement.getDisplayName(),
                lore.toArray(new String[0]));
    }
}
