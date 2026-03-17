package protocol;

import java.util.HashMap;
import java.util.Map;

public class Response {

    public static final String STATUS_OK    = "OK";
    public static final String STATUS_ERROR = "ERROR";

    private String              status;
    private String              message;
    private Map<String, Object> data;
    private long                timestamp;

    // Constructor
    public Response(String status, String message, Map<String, Object> data) {
        this.status    = status;
        this.message   = message != null ? message : "";
        this.data      = data != null ? data : new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    // Factory methods
    public static Response ok(Map<String, Object> data) {
        return new Response(STATUS_OK, "", data);
    }

    public static Response ok(String message) {
        return new Response(STATUS_OK, message, null);
    }

    public static Response error(String message) {
        return new Response(STATUS_ERROR, message, null);
    }

    // ===== SERIALIZATION =====
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"status\":\"").append(status).append("\",");
        sb.append("\"message\":\"").append(escapeJson(message)).append("\",");
        sb.append("\"timestamp\":").append(timestamp).append(",");
        sb.append("\"data\":{");

        int i = 0;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String) {
                sb.append("\"").append(escapeJson(val.toString())).append("\"");
            } else {
                sb.append(val);
            }
        }
        sb.append("}}");
        return sb.toString();
    }

    public static Response fromJson(String json) {
        try {
            String status    = extractString(json, "status");
            String message   = extractString(json, "message");
            long   timestamp = extractLong(json, "timestamp");

            Map<String, Object> data = new HashMap<>();
            int dataStart = json.indexOf("\"data\":{") + 8;
            int dataEnd   = json.lastIndexOf("}");
            if (dataStart > 7 && dataEnd > dataStart) {
                String dataStr = json.substring(dataStart, dataEnd);
                data = parseSimpleObject(dataStr);
            }

            Response res   = new Response(status, message, data);
            res.timestamp  = timestamp;
            return res;
        } catch (Exception e) {
            return Response.error("Parse error: " + e.getMessage());
        }
    }

    // ===== GETTERS =====
    public boolean isOk()                   { return STATUS_OK.equals(status); }
    public String  getStatus()              { return status; }
    public String  getMessage()             { return message; }
    public Map<String, Object> getData()    { return data; }
    public long    getTimestamp()           { return timestamp; }

    public String getDataValue(String key) {
        Object val = data.get(key);
        return val != null ? val.toString() : null;
    }

    // ===== HELPER =====
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : "";
    }

    private static long extractLong(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start < 0) return 0;
        start += search.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        try { return Long.parseLong(json.substring(start, end)); }
        catch (Exception e) { return 0; }
    }

    private static Map<String, Object> parseSimpleObject(String json) {
        Map<String, Object> map = new HashMap<>();
        json = json.trim();
        if (json.isEmpty() || json.equals("{}")) return map;
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String val = kv[1].trim().replace("\"", "");
                map.put(key, val);
            }
        }
        return map;
    }

    @Override
    public String toString() { return toJson(); }
}
