package com.meteorevents.command;

import com.meteorevents.config.ConfigManager;
import com.meteorevents.config.MeteorType;
import com.meteorevents.event.ActiveMeteorEvent;
import com.meteorevents.event.MeteorEventManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public final class MeteorCommand implements CommandExecutor, TabCompleter {
    private final MeteorEventManager eventManager;
    private final ConfigManager configManager;

    public MeteorCommand(MeteorEventManager eventManager, ConfigManager configManager) {
        this.eventManager = eventManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "start":
                handleStart(sender, args);
                break;
            case "stop":
            case "stopall":
            case "remove":
            case "removeall":
            case "clear":
                handleStop(sender);
                break;
            case "next":
                handleNext(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "types":
                handleTypes(sender);
                break;
            case "test":
                handleTest(sender, args);
                break;
            case "info":
                handleInfo(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "meteorevents.start")) {
            sendMsg(sender, "admin.no-permission", null);
            return;
        }

        MeteorType type;
        if (args.length > 1) {
            type = configManager.getMeteorType(args[1]);
            if (type == null) {
                Map<String, String> ph = new HashMap<>();
                ph.put("type", args[1]);
                sendMsg(sender, "admin.unknown-type", ph);
                return;
            }
        } else {
            type = eventManager.pickRandomType();
            if (type == null) {
                sender.sendMessage(configManager.getMessage("prefix") + ChatColor.RED + "No meteor types configured.");
                return;
            }
        }

        final MeteorType selectedType = type;
        if (sender instanceof Player) {
            Location target = ((Player) sender).getLocation();
            eventManager.startEvent(selectedType, target, false);
            Map<String, String> ph = new HashMap<>();
            ph.put("type", selectedType.getDisplayName());
            ph.put("world", target.getWorld().getName());
            sendMsg(sender, "admin.event-started", ph);
        } else {
            org.bukkit.World world = eventManager.pickAllowedWorld();
            if (world == null) {
                sendMsg(sender, "admin.no-valid-world", null);
                return;
            }
            eventManager.findImpactLocationAsync(world, target -> {
                if (target == null || target.getWorld() == null) {
                    sendMsg(sender, "admin.no-valid-world", null);
                    return;
                }
                eventManager.startEvent(selectedType, target, false);
                Map<String, String> ph = new HashMap<>();
                ph.put("type", selectedType.getDisplayName());
                ph.put("world", target.getWorld().getName());
                sendMsg(sender, "admin.event-started", ph);
            });
        }
    }

    private void handleStop(CommandSender sender) {
        if (!hasPermission(sender, "meteorevents.stop") && !hasPermission(sender, "meteorevents.remove")) {
            sendMsg(sender, "admin.no-permission", null);
            return;
        }

        if (eventManager.getActiveEvents().isEmpty()) {
            sendMsg(sender, "admin.no-active-events", null);
            return;
        }

        int count = eventManager.getActiveEvents().size();
        eventManager.stopAllEvents();
        Map<String, String> ph = new HashMap<>();
        ph.put("count", String.valueOf(count));
        sendMsg(sender, "admin.event-stopped", ph);
    }

    private void handleNext(CommandSender sender) {
        if (!hasPermission(sender, "meteorevents.info")) {
            sendMsg(sender, "admin.no-permission", null);
            return;
        }

        Map<String, String> ph = new HashMap<>();
        ph.put("time", String.valueOf(eventManager.getNextEventSeconds()));
        sendMsg(sender, "admin.next-scheduled", ph);
    }

    private void handleReload(CommandSender sender) {
        if (!hasPermission(sender, "meteorevents.reload")) {
            sendMsg(sender, "admin.no-permission", null);
            return;
        }

        configManager.load();
        sendMsg(sender, "admin.reloaded", null);
    }

    private void handleTypes(CommandSender sender) {
        if (!hasPermission(sender, "meteorevents.info")) {
            sendMsg(sender, "admin.no-permission", null);
            return;
        }

        Set<String> typeKeys = configManager.getMeteorTypes().keySet();
        String typesJoined = String.join(", ", typeKeys);

        Map<String, String> ph = new HashMap<>();
        ph.put("types", typesJoined);
        sendMsg(sender, "admin.types-list", ph);
    }

    private void handleTest(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "meteorevents.test")) {
            sendMsg(sender, "admin.no-permission", null);
            return;
        }

        MeteorType type;
        if (args.length > 1) {
            type = configManager.getMeteorType(args[1]);
            if (type == null) {
                Map<String, String> ph = new HashMap<>();
                ph.put("type", args[1]);
                sendMsg(sender, "admin.unknown-type", ph);
                return;
            }
        } else {
            type = eventManager.pickRandomType();
            if (type == null) {
                sender.sendMessage(configManager.getMessage("prefix") + ChatColor.RED + "No meteor types configured.");
                return;
            }
        }

        final MeteorType selectedType = type;
        if (sender instanceof Player) {
            Location target = ((Player) sender).getLocation();
            eventManager.startEvent(selectedType, target, true);
            Map<String, String> ph = new HashMap<>();
            ph.put("type", selectedType.getDisplayName());
            sendMsg(sender, "admin.test-started", ph);
        } else {
            org.bukkit.World world = eventManager.pickAllowedWorld();
            if (world == null) {
                sendMsg(sender, "admin.no-valid-world", null);
                return;
            }
            eventManager.findImpactLocationAsync(world, target -> {
                if (target == null || target.getWorld() == null) {
                    sendMsg(sender, "admin.no-valid-world", null);
                    return;
                }
                eventManager.startEvent(selectedType, target, true);
                Map<String, String> ph = new HashMap<>();
                ph.put("type", selectedType.getDisplayName());
                sendMsg(sender, "admin.test-started", ph);
            });
        }
    }

    private void handleInfo(CommandSender sender) {
        if (!hasPermission(sender, "meteorevents.info")) {
            sendMsg(sender, "admin.no-permission", null);
            return;
        }

        Collection<ActiveMeteorEvent> events = eventManager.getActiveEvents();
        if (events.isEmpty()) {
            sendMsg(sender, "admin.no-active-events", null);
            return;
        }

        Map<String, String> headerPh = new HashMap<>();
        headerPh.put("count", String.valueOf(events.size()));
        sender.sendMessage(configManager.getFormatted("admin.info-header", headerPh));

        for (ActiveMeteorEvent ev : events) {
            Map<String, String> ph = ev.getPlaceholders();
            sender.sendMessage(configManager.getFormatted("admin.info-entry", ph));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(com.meteorevents.util.TextUtil.format("<gold>=== <gradient:#ffaa00:#ffff55>MeteorEvents Commands</gradient> <gold>==="));
        sender.sendMessage(com.meteorevents.util.TextUtil.format("<yellow>/meteorevents start [type]<gray> - Start a meteor event manually"));
        sender.sendMessage(com.meteorevents.util.TextUtil.format("<yellow>/meteorevents stop<gray> - Cancel and remove all active meteor events"));
        sender.sendMessage(com.meteorevents.util.TextUtil.format("<yellow>/meteorevents remove<gray> - Remove all fallen meteors and regenerate craters"));
        sender.sendMessage(com.meteorevents.util.TextUtil.format("<yellow>/meteorevents next<gray> - View next scheduled strike time"));
        sender.sendMessage(com.meteorevents.util.TextUtil.format("<yellow>/meteorevents types<gray> - List available meteor types"));
        sender.sendMessage(com.meteorevents.util.TextUtil.format("<yellow>/meteorevents test [type]<gray> - Safe visual test (no crater damage)"));
        sender.sendMessage(com.meteorevents.util.TextUtil.format("<yellow>/meteorevents info<gray> - View active events status"));
        sender.sendMessage(com.meteorevents.util.TextUtil.format("<yellow>/meteorevents reload<gray> - Reload configurations"));
    }

    private void sendMsg(CommandSender sender, String path, Map<String, String> ph) {
        String msg = configManager.getFormatted(path, ph);
        if (!msg.isEmpty()) {
            sender.sendMessage(configManager.getMessage("prefix") + msg);
        }
    }

    private boolean hasPermission(CommandSender sender, String perm) {
        return sender.hasPermission("meteorevents.admin") || sender.hasPermission(perm);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("start", "stop", "remove", "clear", "next", "types", "test", "info", "reload", "help");
            for (String s : subcommands) {
                if (s.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    list.add(s);
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("test"))) {
            for (String t : configManager.getMeteorTypes().keySet()) {
                if (t.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    list.add(t);
                }
            }
        }
        return list;
    }
}
