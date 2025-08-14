package top.steve3184.dungeonstats.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import top.steve3184.dungeonstats.model.PlayerStats;
import top.steve3184.dungeonstats.utils.DataManager;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DunCommand implements CommandExecutor, TabCompleter {

    private final DataManager dataManager;
    private final FileConfiguration config;

    public DunCommand(DataManager dataManager, FileConfiguration config) {
        this.dataManager = dataManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "stats" -> showPlayerStats(sender, args);
            case "killtop" -> showTopList(sender, "kills");
            case "playtimetop" -> showTopList(sender, "playtime");
            case "maxleveltop" -> showTopList(sender, "maxlevel");
            default -> sendUsage(sender);
        }
        return true;
    }

    private void showPlayerStats(CommandSender sender, String[] args) {
        String targetName;
        if (args.length > 1) {
            targetName = args[1];
        } else if (sender instanceof Player) {
            targetName = sender.getName();
        } else {
            sender.sendMessage(format(config.getString("messages.command-usage")));
            return;
        }

        PlayerStats stats = dataManager.getPlayerStats(targetName);
        if (stats == null) {
            sender.sendMessage(format(config.getString("messages.command-player-not-found").replace("{player_name}", targetName)));
            return;
        }

        sender.sendMessage(format(config.getString("messages.stats-title").replace("{player_name}", stats.playerName())));
        sender.sendMessage(format(config.getString("messages.stats-line-maxlevel").replace("{value}", String.valueOf(stats.maxLevel()))));
        sender.sendMessage(format(config.getString("messages.stats-line-kills").replace("{value}", String.valueOf(stats.kills()))));
        sender.sendMessage(format(config.getString("messages.stats-line-playtime").replace("{value}", formatSeconds(stats.playtimeSeconds()))));
    }

    private void showTopList(CommandSender sender, String key) {
        sender.sendMessage(format(config.getString("messages.title-" + key)));
        List<PlayerStats> topPlayers = dataManager.getTopPlayers(key, 10);
        if (topPlayers.isEmpty()) {
            sender.sendMessage(format(config.getString("messages.command-no-data")));
            return;
        }

        for (int i = 0; i < 10; i++) {
            if (i < topPlayers.size()) {
                PlayerStats stats = topPlayers.get(i);
                String valueStr = switch (key) {
                    case "playtime" -> formatSeconds(stats.playtimeSeconds());
                    case "kills" -> String.valueOf(stats.kills());
                    case "maxlevel" -> String.valueOf(stats.maxLevel());
                    default -> "";
                };
                String rankColor = switch (i) {
                    case 0 -> config.getString("messages.rank-color-1");
                    case 1 -> config.getString("messages.rank-color-2");
                    case 2 -> config.getString("messages.rank-color-3");
                    default -> config.getString("messages.rank-color-default");
                };
                String entry = config.getString("messages.rank-entry")
                        .replace("{rank}", String.valueOf(i + 1))
                        .replace("{player_name}", stats.playerName())
                        .replace("{value}", valueStr);
                sender.sendMessage(format(rankColor + entry));
            } else {
                // sender.sendMessage(" "); // 补充空行
            }
        }
    }

    private String formatSeconds(long totalSeconds) {
        long hours = TimeUnit.SECONDS.toHours(totalSeconds);
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(format(config.getString("messages.command-usage")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("stats", "killtop", "playtimetop", "maxleveltop").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}