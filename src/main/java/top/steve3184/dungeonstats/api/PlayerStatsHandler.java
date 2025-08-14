package top.steve3184.dungeonstats.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import top.steve3184.dungeonstats.model.PlayerStats;
import top.steve3184.dungeonstats.utils.DataManager;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class PlayerStatsHandler extends BaseHandler {

    public PlayerStatsHandler(DataManager dataManager, Gson gson) {
        super(dataManager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String playerName = params.get("name");

        if (playerName == null || playerName.isEmpty()) {
            sendResponse(exchange, 400, Collections.singletonMap("error", "Player name query parameter is required."));
            return;
        }

        PlayerStats stats = dataManager.getPlayerStats(playerName);
        if (stats == null) {
            sendResponse(exchange, 404, Collections.singletonMap("error", "Player not found."));
            return;
        }
        sendResponse(exchange, 200, stats);
    }
}