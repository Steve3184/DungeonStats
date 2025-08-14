package top.steve3184.dungeonstats.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import top.steve3184.dungeonstats.utils.DataManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlayersHandler extends BaseHandler {

    public PlayersHandler(DataManager dataManager, Gson gson) {
        super(dataManager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 确保是GET请求
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
            return;
        }

        // 获取服务器核心对象
        Scoreboard scoreboard = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        // 创建在线玩家列表的副本，以防在遍历时发生修改
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());

        // 使用一个List<Map>来构建最终的JSON结构
        List<Map<String, Object>> playersData = new ArrayList<>();

        for (Player player : onlinePlayers) {
            Map<String, Object> playerData = new LinkedHashMap<>();
            String status;

            // 1. 最高优先级：检查旁观模式
            if (player.getGameMode() == GameMode.SPECTATOR) {
                status = "spectator";
            } else {
                // 2. 如果不是旁观者，再检查队伍
                Team team = scoreboard.getEntryTeam(player.getName());
                if (team != null) {
                    switch (team.getName().toLowerCase()) {
                        case "waiting" -> status = "waiting";
                        case "default" -> status = "ingame";
                        default -> status = "unknown"; // 其他队伍名
                    }
                } else {
                    status = "unknown"; // 没有队伍
                }
            }

            // 填充数据
            playerData.put("name", player.getName());
            playerData.put("status", status);
            playerData.put("health", player.getHealth());
            playerData.put("armor", player.getAttribute(Attribute.GENERIC_ARMOR).getValue());

            playersData.add(playerData);
        }

        sendResponse(exchange, 200, playersData);
    }
}