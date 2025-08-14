package top.steve3184.dungeonstats;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.CommandStorage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import top.steve3184.dungeonstats.api.*;
import top.steve3184.dungeonstats.commands.DunCommand;
import top.steve3184.dungeonstats.holograms.HologramManager;
import top.steve3184.dungeonstats.listeners.KillListener;
import top.steve3184.dungeonstats.model.PlayerLevel;
import top.steve3184.dungeonstats.utils.DataManager;
import top.steve3184.dungeonstats.utils.LogParser;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public final class DungeonStats extends JavaPlugin {

    private HttpServer server;
    private File dataFile;
    private FileConfiguration dataConfig;
    private DataManager dataManager;
    private HologramManager hologramManager;
    private Gson gson;

    private String lastKnownLogContent = "";

    @Override
    public void onEnable() {
        this.gson = new Gson();
        saveDefaultConfig();
        createDataFile();
        this.dataManager = new DataManager(this);
        this.hologramManager = new HologramManager(this, dataManager);

        getServer().getPluginManager().registerEvents(new KillListener(dataManager), this);
        DunCommand dunCommand = new DunCommand(dataManager, getConfig());
        getCommand("dun").setExecutor(dunCommand);
        getCommand("dun").setTabCompleter(dunCommand);

        startLogCheckerTask();
        startPlaytimeTrackerTask();
        setupApiServer();

        // 初始化全息图
        hologramManager.initialize();

        getLogger().info("Plugin DungeonStats Enabled！");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        if (server != null) server.stop(0);
        hologramManager.cleanup(); // 清理全息图实体
        saveDataConfig();
        getLogger().info("Plugin DungeonStats Disabled！");
    }

    private void setupApiServer() {
        if (!getConfig().getBoolean("api-server.enabled", false)) {
            return;
        }
        int port = getConfig().getInt("api-server.port", 8080);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/players", new PlayersHandler(dataManager, gson));
            server.createContext("/stats", new StatsHandler(dataManager, gson));
            server.createContext("/playerstats", new PlayerStatsHandler(dataManager, gson));
            server.createContext("/killtop", new TopHandler(dataManager, gson, "kills"));
            server.createContext("/playtimetop", new TopHandler(dataManager, gson, "playtime"));
            server.createContext("/maxleveltop", new TopHandler(dataManager, gson, "maxLevel"));
            server.setExecutor(null);
            server.start();
            getLogger().info("API Server started on port " + port + "!");
        } catch (IOException e) {
            getLogger().severe("API Server failed to start!");
            e.printStackTrace();
        }
    }

    private void startLogCheckerTask() {
        if (!getConfig().getBoolean("log-checker.enabled", false)) {
            return;
        }
        long interval = getConfig().getLong("log-checker.interval-ticks", 20L);
        getServer().getScheduler().runTaskTimer(this, () -> {
            try {
                MinecraftServer mcServer = ((CraftServer) getServer()).getServer();
                CommandStorage commandStorage = mcServer.getCommandStorage();
                ResourceLocation storageId = ResourceLocation.fromNamespaceAndPath("dun", "log");
                CompoundTag nbt = commandStorage.get(storageId);

                String currentContent = null;
                if (nbt != null && nbt.contains("Page", 10)) {
                    CompoundTag pageTag = nbt.getCompound("Page");
                    if (pageTag.contains("raw", 8)) currentContent = pageTag.getString("raw");
                }

                if (currentContent != null && !currentContent.equals(this.lastKnownLogContent)) {
                    this.lastKnownLogContent = currentContent;
                    LogParser.ParsedResult result = LogParser.parse(currentContent);
                    if (result != null) {
                        getLogger().info("Parsed Dungeon Log #" + result.dungeonLog.recordId());
                        dataManager.saveDungeonLog(result.dungeonLog);
                        for (PlayerLevel pl : result.playerLevels) {
                            dataManager.updatePlayerMaxLevel(pl.playerName(), pl.level());
                        }
                    }
                }
            } catch (Exception e) {
                getLogger().severe("Log checker failed to start!");
                e.printStackTrace();
            }
        }, interval, interval);
    }

    private void startPlaytimeTrackerTask() {
        Scoreboard mainScoreboard = getServer().getScoreboardManager().getMainScoreboard();
        getServer().getScheduler().runTaskTimer(this, () -> {
            Team inGameTeam = mainScoreboard.getTeam("default");
            if (inGameTeam == null) return;
            for (Player player : getServer().getOnlinePlayers()) {
                if (inGameTeam.hasEntry(player.getName())) {
                    dataManager.incrementPlayTime(player);
                }
            }
        }, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, this::saveDataConfig, 6000L, 6000L);
    }

    public FileConfiguration getDataConfig() { return this.dataConfig; }
    public void saveDataConfig() {
        try {
            dataConfig.save(dataFile);
            getLogger().info("PlayerData saved.");
        } catch (IOException e) {
            getLogger().severe("Failed to save PlayerData!");
            e.printStackTrace();
        }
    }
    private void createDataFile() {
        dataFile = new File(getDataFolder(), getConfig().getString("database", "data.yml"));
        if (!dataFile.exists()) {
            getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { getLogger().severe("Failed to create database yml!"); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
}