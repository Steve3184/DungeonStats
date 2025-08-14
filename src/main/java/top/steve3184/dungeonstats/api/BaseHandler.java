package top.steve3184.dungeonstats.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import top.steve3184.dungeonstats.utils.DataManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseHandler implements HttpHandler {

    protected final DataManager dataManager;
    protected final Gson gson;

    public BaseHandler(DataManager dataManager, Gson gson) {
        this.dataManager = dataManager;
        this.gson = gson;
    }

    protected void sendResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String jsonResponse = gson.toJson(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
        os.close();
    }

    protected Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) {
            return result;
        }
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }
}