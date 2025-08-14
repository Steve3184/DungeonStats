package top.steve3184.dungeonstats.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import top.steve3184.dungeonstats.model.DungeonLog;
import top.steve3184.dungeonstats.utils.DataManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class StatsHandler extends BaseHandler {

    public StatsHandler(DataManager dataManager, Gson gson) {
        super(dataManager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        int limit = Integer.parseInt(params.getOrDefault("limit", "0"));

        List<DungeonLog> logs = dataManager.getDungeonLogs(limit);
        sendResponse(exchange, 200, logs);
    }
}