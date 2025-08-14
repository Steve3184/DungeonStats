package top.steve3184.dungeonstats.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder; // 引入 GsonBuilder 用于格式化输出
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import top.steve3184.dungeonstats.model.DungeonLog;
import top.steve3184.dungeonstats.model.PlayerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.mojang.logging.LogUtils.getLogger;

public class LogParser {

    // 用于解析的Gson实例，保持高效
    private static final Gson gson = new Gson();
    // 专门用于调试输出的Gson实例，会进行格式化（pretty printing）
    private static final Gson debugGson = new GsonBuilder().setPrettyPrinting().create();

    public static class ParsedResult {
        public final DungeonLog dungeonLog;
        public final List<PlayerLevel> playerLevels;

        public ParsedResult(DungeonLog dungeonLog, List<PlayerLevel> playerLevels) {
            this.dungeonLog = dungeonLog;
            this.playerLevels = playerLevels;
        }
    }

    public static ParsedResult parse(String rawJson) {
        try {
            JsonObject root = gson.fromJson(rawJson, JsonObject.class);
            JsonArray mainExtra = root.getAsJsonArray("extra");

            int recordId = Integer.parseInt(mainExtra.get(0).getAsString());

            List<PlayerLevel> playerLevels = new ArrayList<>();
            int maxLevel = 0;
            for (JsonElement element : mainExtra) {
                if (element.isJsonObject() && Objects.equals(element.getAsJsonObject().get("text").getAsString(), "LV")) {
                    JsonObject playerBlock = element.getAsJsonObject();
                    JsonArray playerExtra = playerBlock.getAsJsonArray("extra");

                    int level = playerExtra.get(0).getAsInt();
                    if (level > maxLevel) {
                        maxLevel = level;
                    }
                    JsonObject playerDetails = playerExtra.get(2).getAsJsonObject();
                    String playerName = playerDetails.get("insertion").getAsString();

                    playerLevels.add(new PlayerLevel(playerName, level));
                }
            }
            int doors = Integer.parseInt(mainExtra.get(mainExtra.size() - 11).getAsString());
            int kills = Integer.parseInt(mainExtra.get(mainExtra.size() - 8).getAsString());
            int bosses = Integer.parseInt(mainExtra.get(mainExtra.size() - 5).getAsString());

            JsonObject timeObject = mainExtra.get(mainExtra.size() - 1).getAsJsonObject();
            JsonArray timeExtra = timeObject.getAsJsonArray("extra");
            long hours = Long.parseLong(timeObject.get("text").getAsString()) * 10 + Long.parseLong(timeExtra.get(0).getAsString());
            long minutes = Long.parseLong(timeExtra.get(3).getAsString());
            long seconds = Long.parseLong(timeExtra.get(6).getAsString());
            long duration = (hours * 3600) + (minutes * 60) + seconds;

            DungeonLog log = new DungeonLog(recordId, maxLevel, doors, kills, bosses, duration);
            return new ParsedResult(log, playerLevels);

        } catch (Exception e) {
            // 在捕获到异常时，打印出导致问题的原始JSON，并打印堆栈跟踪
            getLogger().error("Failed to parse JSON");
            getLogger().error("Raw JSON: {}", rawJson);
            e.printStackTrace(); // 这会打印出详细的错误信息和代码行号
            return null;
        }
    }
}