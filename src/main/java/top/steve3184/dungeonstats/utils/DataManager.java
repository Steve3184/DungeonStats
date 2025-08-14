package top.steve3184.dungeonstats.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import top.steve3184.dungeonstats.DungeonStats;
import top.steve3184.dungeonstats.model.DungeonLog;
import top.steve3184.dungeonstats.model.PlayerStats;

import java.util.*;
import java.util.stream.Collectors;

        public class DataManager {

            private final DungeonStats plugin;

            public DataManager(DungeonStats plugin) {
                this.plugin = plugin;
            }

            public void saveDungeonLog(DungeonLog log) {
                String path = "logs." + log.recordId();
                plugin.getDataConfig().set(path + ".maxLevel", log.maxLevel());
                plugin.getDataConfig().set(path + ".doorsOpened", log.doorsOpened());
                plugin.getDataConfig().set(path + ".enemiesKilled", log.enemiesKilled());
                plugin.getDataConfig().set(path + ".bossesDefeated", log.bossesDefeated());
                plugin.getDataConfig().set(path + ".durationSeconds", log.durationSeconds());
                // 不在这里保存，由周期性任务统一保存
            }

            public List<DungeonLog> getDungeonLogs(int limit) {
                ConfigurationSection logsSection = plugin.getDataConfig().getConfigurationSection("logs");
                if (logsSection == null) return Collections.emptyList();

                List<DungeonLog> logs = new ArrayList<>();
                List<String> sortedKeys = logsSection.getKeys(false).stream()
                        .sorted(Comparator.comparingInt(s -> Integer.parseInt((String) s)).reversed())
                        .collect(Collectors.toList());

                for (String key : sortedKeys) {
                    if (limit > 0 && logs.size() >= limit) break;
                    String path = "logs." + key;
                    logs.add(new DungeonLog(
                            Integer.parseInt(key),
                            plugin.getDataConfig().getInt(path + ".maxLevel"),
                            plugin.getDataConfig().getInt(path + ".doorsOpened"),
                            plugin.getDataConfig().getInt(path + ".enemiesKilled"),
                            plugin.getDataConfig().getInt(path + ".bossesDefeated"),
                            plugin.getDataConfig().getLong(path + ".durationSeconds")
                    ));
                }
                return logs;
            }

            public void incrementKillCount(Player player) {
                String path = "players." + player.getUniqueId() + ".kills";
                long currentKills = plugin.getDataConfig().getLong(path, 0);
                plugin.getDataConfig().set(path, currentKills + 1);
            }

            public void incrementPlayTime(Player player) {
                String path = "players." + player.getUniqueId() + ".playtime";
                long currentPlaytime = plugin.getDataConfig().getLong(path, 0);
                plugin.getDataConfig().set(path, currentPlaytime + 1);
            }

            public void updatePlayerMaxLevel(String playerName, int level) {
                PlayerStats stats = getPlayerStats(playerName);
                if (stats == null) return; // Should not happen if player exists

                // Inefficient lookup by name, but works for this scope.
                ConfigurationSection playersSection = plugin.getDataConfig().getConfigurationSection("players");
                if (playersSection == null) return;
                for (String uuidStr : playersSection.getKeys(false)) {
                    String name = plugin.getServer().getOfflinePlayer(UUID.fromString(uuidStr)).getName();
                    if (playerName.equalsIgnoreCase(name)) {
                        String path = "players." + uuidStr + ".maxLevel";
                        int currentMax = plugin.getDataConfig().getInt(path, 0);
                        if (level > currentMax) {
                            plugin.getDataConfig().set(path, level);
                        }
                        return;
                    }
                }
            }

            public PlayerStats getPlayerStats(String playerName) {
                ConfigurationSection playersSection = plugin.getDataConfig().getConfigurationSection("players");
                if (playersSection == null) return null;

                for (String uuidStr : playersSection.getKeys(false)) {
                    UUID uuid = UUID.fromString(uuidStr);
                    String name = plugin.getServer().getOfflinePlayer(uuid).getName();
                    if (name != null && playerName.equalsIgnoreCase(name)) {
                        return new PlayerStats(
                                name,
                                plugin.getDataConfig().getLong("players." + uuidStr + ".kills", 0),
                                plugin.getDataConfig().getLong("players." + uuidStr + ".playtime", 0),
                                plugin.getDataConfig().getInt("players." + uuidStr + ".maxLevel", 0)
                        );
                    }
                }
                return null;
            }

            public List<PlayerStats> getTopPlayers(String key, int limit) {
                ConfigurationSection playersSection = plugin.getDataConfig().getConfigurationSection("players");
                if (playersSection == null) return Collections.emptyList();

                List<PlayerStats> allStats = new ArrayList<>();
                for (String uuidStr : playersSection.getKeys(false)) {
                    UUID uuid = UUID.fromString(uuidStr);
                    String name = plugin.getServer().getOfflinePlayer(uuid).getName();
                    if (name == null) continue;
                    allStats.add(new PlayerStats(
                            name,
                            plugin.getDataConfig().getLong("players." + uuidStr + ".kills", 0),
                            plugin.getDataConfig().getLong("players." + uuidStr + ".playtime", 0),
                            plugin.getDataConfig().getInt("players." + uuidStr + ".maxLevel", 0)
                    ));
                }

                Comparator<PlayerStats> comparator = switch (key) {
                    case "kills" -> Comparator.comparingLong(PlayerStats::kills).reversed();
                    case "playtime" -> Comparator.comparingLong(PlayerStats::playtimeSeconds).reversed();
                    case "maxLevel" -> Comparator.comparingInt(PlayerStats::maxLevel).reversed();
                    default -> null;
                };

                if (comparator != null) {
                    // Sorting is done here. The previous issue was likely a subtle logic error. This is a robust way.
                    return allStats.stream().sorted(comparator).limit(limit).collect(Collectors.toList());
                }
                return Collections.emptyList();
            }
        }