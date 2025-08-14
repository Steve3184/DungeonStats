package top.steve3184.dungeonstats.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import top.steve3184.dungeonstats.model.PlayerStats;
import top.steve3184.dungeonstats.utils.DataManager;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopHandler extends BaseHandler {

    private final String key; // "kills", "playtime", or "maxlevel"

    public TopHandler(DataManager dataManager, Gson gson, String key) {
        super(dataManager, gson);
        this.key = key;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 1. 获取包含完整信息的已排序玩家列表
        List<PlayerStats> topPlayers = dataManager.getTopPlayers(key, 100);

        // 2. 将完整的PlayerStats列表转换为一个只包含相关字段的Map列表
        List<Map<String, Object>> filteredResults = topPlayers.stream()
                .map(stats -> {
                    // 使用 LinkedHashMap 来保持插入顺序，让"playerName"总是在前面
                    Map<String, Object> playerMap = new LinkedHashMap<>();
                    playerMap.put("playerName", stats.playerName());

                    // 根据当前处理器的key，只添加相关的数据字段
                    switch (this.key) {
                        case "kills" -> playerMap.put("kills", stats.kills());
                        case "playtime" -> playerMap.put("playtimeSeconds", stats.playtimeSeconds());
                        case "maxLevel" -> playerMap.put("maxLevel", stats.maxLevel());
                    }
                    return playerMap;
                })
                .collect(Collectors.toList());

        // 3. 将这个精简后的列表作为JSON返回
        sendResponse(exchange, 200, filteredResults);
    }
}