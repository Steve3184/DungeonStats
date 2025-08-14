package top.steve3184.dungeonstats.holograms;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import top.steve3184.dungeonstats.DungeonStats;
import top.steve3184.dungeonstats.model.PlayerStats;
import top.steve3184.dungeonstats.utils.DataManager;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class HologramManager {

    private final DungeonStats plugin;
    private final DataManager dataManager;
    private final List<TextDisplay> activeHolograms = new ArrayList<>();
    private final List<String> leaderboardKeys = Arrays.asList("kills", "playtime", "maxLevel");
    private int currentRotationIndex = 0;

    private BukkitTask refreshTask;
    private BukkitTask rotationTask;

    public HologramManager(DungeonStats plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void initialize() {
        if (!plugin.getConfig().getBoolean("holograms.enabled", false)) {
            return;
        }

        // 先清理旧的全息图，以防插件重载
        cleanup();

        String mode = plugin.getConfig().getString("holograms.display-mode", "SINGLE").toUpperCase();

        if ("SINGLE".equals(mode)) {
            setupSingleMode();
        } else {
            setupMultipleMode();
        }

        long refreshInterval = plugin.getConfig().getLong("holograms.refresh-interval-seconds", 10) * 20L;
        this.refreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateAllHolograms, 0L, refreshInterval);
    }

    private void setupSingleMode() {
        Location loc = parseLocation(plugin.getConfig().getString("holograms.single-display.location"));
        if (loc == null) {
            plugin.getLogger().severe("Single hologram location is invalid!");
            return;
        }
        TextDisplay hologram = createHologram(loc);
        activeHolograms.add(hologram);

        long rotationInterval = plugin.getConfig().getLong("holograms.single-display.rotation-interval-seconds", 5) * 20L;
        this.rotationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::rotateSingleHologram, rotationInterval, rotationInterval * 2);
    }

    private void setupMultipleMode() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("holograms.multiple-displays");
        if (section == null) return;

        for (String key : leaderboardKeys) {
            Location loc = parseLocation(section.getString(key + ".location"));
            if (loc != null) {
                TextDisplay hologram = createHologram(loc);
                hologram.setMetadata("leaderboard_key", new org.bukkit.metadata.FixedMetadataValue(plugin, key));
                activeHolograms.add(hologram);
            }
        }
    }

    private void rotateSingleHologram() {
        if (activeHolograms.isEmpty()) return;

        TextDisplay hologram = activeHolograms.getFirst();
        String key = leaderboardKeys.get(currentRotationIndex);
        updateHologramContent(hologram, key);

        currentRotationIndex = (currentRotationIndex + 1) % leaderboardKeys.size();
    }

    private void updateAllHolograms() {
        if (activeHolograms.isEmpty()) return;

        String mode = plugin.getConfig().getString("holograms.display-mode", "SINGLE").toUpperCase();

        if ("SINGLE".equals(mode)) {
            // 在单模式下，刷新任务会触发一次轮换，以确保数据最新
            rotateSingleHologram();
        } else {
            for (TextDisplay hologram : activeHolograms) {
                if (hologram.hasMetadata("leaderboard_key")) {
                    String key = hologram.getMetadata("leaderboard_key").get(0).asString();
                    updateHologramContent(hologram, key);
                }
            }
        }
    }

    private void updateHologramContent(TextDisplay hologram, String key) {
        List<PlayerStats> topPlayers = dataManager.getTopPlayers(key, 10);
        String title = plugin.getConfig().getString("messages.title-" + key.toLowerCase(), "Leaderboard");
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(format(title + "\n\n&r"));

        for (int i = 0; i < 10; i++) {
            if (i < topPlayers.size()) {
                PlayerStats stats = topPlayers.get(i);
                contentBuilder.append(formatRankEntry(i + 1, stats, key));
            } else {
                contentBuilder.append("\n"); // 补充空行
            }
        }

        hologram.setText(contentBuilder.toString());
    }

    private String formatRankEntry(int rank, PlayerStats stats, String key) {
        String rankColor;
        switch (rank) {
            case 1 -> rankColor = plugin.getConfig().getString("messages.rank-color-1", "&6");
            case 2 -> rankColor = plugin.getConfig().getString("messages.rank-color-2", "&7");
            case 3 -> rankColor = plugin.getConfig().getString("messages.rank-color-3", "&c");
            default -> rankColor = plugin.getConfig().getString("messages.rank-color-default", "&7");
        }

        String valueStr;
        if (key.equals("playtime")) {
            valueStr = formatSeconds(stats.playtimeSeconds());
        } else {
            valueStr = String.valueOf(switch(key) {
                case "kills" -> stats.kills();
                case "maxLevel" -> stats.maxLevel();
                default -> 0;
            });
        }

        String template = plugin.getConfig().getString("messages.rank-entry", "#{rank} &b{player_name}: &f{value}");
        return format(rankColor + template
                .replace("{rank}", String.valueOf(rank))
                .replace("{player_name}", stats.playerName())
                .replace("{value}", valueStr)) + "\n";
    }

    private TextDisplay createHologram(Location location) {
        return location.getWorld().spawn(location, TextDisplay.class, holo -> {
            holo.setBillboard(Display.Billboard.VERTICAL);
            holo.setText("");
            holo.setAlignment(TextDisplay.TextAlignment.CENTER);
            holo.setPersistent(false); // 不保存到区块数据中
        });
    }

    public void cleanup() {
        // 取消任务
        if (refreshTask != null) refreshTask.cancel();
        if (rotationTask != null) rotationTask.cancel();

        // 移除实体
        activeHolograms.forEach(hologram -> {
            if (hologram.isValid()) {
                hologram.remove();
            }
        });
        activeHolograms.clear();
    }

    private Location parseLocation(String locString) {
        if (locString == null || locString.isEmpty()) return null;
        try {
            String[] parts = locString.split(",");
            World world = Bukkit.getWorld(parts[0]);
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Location(world, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    private String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String formatSeconds(long totalSeconds) {
        long hours = TimeUnit.SECONDS.toHours(totalSeconds);
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}