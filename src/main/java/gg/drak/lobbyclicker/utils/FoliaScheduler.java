package gg.drak.lobbyclicker.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Scheduler abstraction for Folia (regionized threading) and standard Bukkit/Paper.
 */
public final class FoliaScheduler {
    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    private FoliaScheduler() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static PluginTask runGlobal(Plugin plugin, Runnable task) {
        if (FOLIA) {
            return wrapFolia(Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run()));
        }
        return wrapBukkit(Bukkit.getScheduler().runTask(plugin, task));
    }

    public static PluginTask runGlobalLater(Plugin plugin, Runnable task, long delayTicks) {
        if (FOLIA) {
            return wrapFolia(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delayTicks));
        }
        return wrapBukkit(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    public static PluginTask runGlobalTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (FOLIA) {
            return wrapFolia(Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    plugin, t -> task.run(), delayTicks, periodTicks));
        }
        return wrapBukkit(Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks));
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public static void runForEntity(Entity entity, Plugin plugin, Runnable task) {
        if (entity == null || !entity.isValid()) {
            runGlobal(plugin, task);
            return;
        }
        if (FOLIA) {
            entity.getScheduler().run(plugin, t -> task.run(), null);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static PluginTask runForEntityLater(Entity entity, Plugin plugin, Runnable task, long delayTicks) {
        if (entity == null || !entity.isValid()) {
            return runGlobalLater(plugin, task, delayTicks);
        }
        if (FOLIA) {
            return wrapFolia(entity.getScheduler().runDelayed(plugin, t -> task.run(), null, delayTicks));
        }
        return wrapBukkit(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    public static PluginTask runForEntityTimer(Entity entity, Plugin plugin, Runnable task,
                                               long delayTicks, long periodTicks) {
        if (entity == null || !entity.isValid()) {
            return runGlobalTimer(plugin, task, delayTicks, periodTicks);
        }
        if (FOLIA) {
            return wrapFolia(entity.getScheduler().runAtFixedRate(
                    plugin, t -> task.run(), null, delayTicks, periodTicks));
        }
        return wrapBukkit(Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks));
    }

    public static void runForSender(CommandSender sender, Plugin plugin, Runnable task) {
        if (sender instanceof Player player) {
            runForEntity(player, plugin, task);
        } else {
            runGlobal(plugin, task);
        }
    }

    private static PluginTask wrapBukkit(BukkitTask task) {
        return new PluginTask() {
            @Override
            public void cancel() {
                task.cancel();
            }

            @Override
            public boolean isCancelled() {
                return task.isCancelled();
            }
        };
    }

    private static PluginTask wrapFolia(io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
        return new PluginTask() {
            @Override
            public void cancel() {
                task.cancel();
            }

            @Override
            public boolean isCancelled() {
                return task.isCancelled();
            }
        };
    }

    public interface PluginTask {
        void cancel();

        boolean isCancelled();
    }
}
