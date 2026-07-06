package client.network;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import protocol.Request;
import protocol.RequestType;
import protocol.Response;

public final class ServerApi {

    private ServerApi() {}

    public static Response send(RequestType type, Map<String, ?> payload) {
        Request request = new Request(type);
        if (payload != null) {
            for (Map.Entry<String, ?> entry : payload.entrySet()) {
                request.putPayload(entry.getKey(), entry.getValue());
            }
        }
        return SocketClient.getInstance().send(request);
    }

    public static Response send(RequestType type) {
        return send(type, null);
    }

    public static JSONArray getArray(Response response, String key) {
        String raw = response.getDataValue(key);
        if (raw == null || raw.isBlank()) {
            return new JSONArray();
        }
        return new JSONArray(raw);
    }

    public static JSONObject getObject(Response response, String key) {
        String raw = response.getDataValue(key);
        if (raw == null || raw.isBlank()) {
            return new JSONObject();
        }
        return new JSONObject(raw);
    }

    public static List<String> getStringList(Response response, String key) {
        JSONArray arr = getArray(response, key);
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            values.add(arr.optString(i, ""));
        }
        return values;
    }

    public static String text(JSONObject obj, String key) {
        return obj.optString(key, "");
    }

    public static int integer(JSONObject obj, String key) {
        return obj.optInt(key, 0);
    }

    public static long longValue(JSONObject obj, String key) {
        return obj.optLong(key, 0L);
    }
}
