package com.meteorevents.compat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Schedulers {

    public interface TaskHandle {
        void cancel();
    }

    private Schedulers() {}

    public static void runGlobal(Plugin plugin, Runnable task) {
        if (Platform.isFolia()) {
            try {
                Object server = Bukkit.getServer();
                Object scheduler = server.getClass().getMethod("getGlobalRegionScheduler").invoke(server);
                Method method = scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
                method.invoke(scheduler, plugin, task);
                return;
            } catch (Throwable ignored) {}
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static void runAt(Plugin plugin, Location location, Runnable task) {
        if (Platform.isFolia() && location != null && location.getWorld() != null) {
            try {
                Object server = Bukkit.getServer();
                Object scheduler = server.getClass().getMethod("getRegionScheduler").invoke(server);
                Method method = scheduler.getClass().getMethod("execute", Plugin.class, Location.class, Runnable.class);
                method.invoke(scheduler, plugin, location, task);
                return;
            } catch (Throwable ignored) {}
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static void runLaterAt(Plugin plugin, Location location, long ticks, Runnable task) {
        if (Platform.isFolia() && location != null && location.getWorld() != null) {
            try {
                Object server = Bukkit.getServer();
                Object scheduler = server.getClass().getMethod("getRegionScheduler").invoke(server);
                Method method = scheduler.getClass().getMethod("runDelayed", Plugin.class, Location.class, Consumer.class, long.class);
                method.invoke(scheduler, plugin, location, (Consumer<Object>) scheduled -> task.run(), ticks);
                return;
            } catch (Throwable ignored) {}
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }

    public static TaskHandle runTimerGlobal(Plugin plugin, long initialDelayTicks, long periodTicks, Runnable task) {
        if (Platform.isFolia()) {
            try {
                Object server = Bukkit.getServer();
                Object scheduler = server.getClass().getMethod("getGlobalRegionScheduler").invoke(server);
                Method method = scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
                final Object scheduledTask = method.invoke(scheduler, plugin, (Consumer<Object>) scheduled -> task.run(), Math.max(1L, initialDelayTicks), Math.max(1L, periodTicks));
                return () -> {
                    try {
                        scheduledTask.getClass().getMethod("cancel").invoke(scheduledTask);
                    } catch (Throwable ignored) {}
                };
            } catch (Throwable ignored) {}
        }
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelayTicks, periodTicks);
        return bukkitTask::cancel;
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (Platform.isFolia()) {
            try {
                Object server = Bukkit.getServer();
                Object scheduler = server.getClass().getMethod("getAsyncScheduler").invoke(server);
                Method method = scheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
                method.invoke(scheduler, plugin, (Consumer<Object>) scheduled -> task.run());
                return;
            } catch (Throwable ignored) {}
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public static TaskHandle runAsyncTimer(Plugin plugin, long initialDelaySeconds, long periodSeconds, Runnable task) {
        if (Platform.isFolia()) {
            try {
                Object server = Bukkit.getServer();
                Object scheduler = server.getClass().getMethod("getAsyncScheduler").invoke(server);
                Method method = scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
                final Object scheduledTask = method.invoke(scheduler, plugin, (Consumer<Object>) scheduled -> task.run(), initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);
                return () -> {
                    try {
                        scheduledTask.getClass().getMethod("cancel").invoke(scheduledTask);
                    } catch (Throwable ignored) {}
                };
            } catch (Throwable ignored) {}
        }
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, initialDelaySeconds * 20L, periodSeconds * 20L);
        return bukkitTask::cancel;
    }
}
