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
    private String              nonce;

    // Constructor
    public Response(String status, String message, Map<String, Object> data) {
        this.status    = status;
        this.message   = message != null ? message : "";
        this.data      = data != null ? data : new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    // optional nonce to correlate request/response
    public void setNonce(String nonce) { this.nonce = nonce; }
    public String getNonce() { return nonce; }

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
        if (nonce != null && !nonce.isEmpty()) {
            sb.append("\"nonce\":\"").append(nonce).append("\",");
        }
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
            org.json.JSONObject obj = new org.json.JSONObject(json);
            String nonce = obj.optString("nonce", null);
            String status = obj.optString("status", "");
            String message = obj.optString("message", "");
            long timestamp = obj.optLong("timestamp", 0);
            
            Map<String, Object> dataMap = new HashMap<>();
            org.json.JSONObject dataObj = obj.optJSONObject("data");
            if (dataObj != null) {
                for (String key : dataObj.keySet()) {
                    Object val = dataObj.get(key);
                    // If it's a JSONArray or JSONObject, we can convert it to string so that client code expecting strings works
                    if (val instanceof org.json.JSONArray || val instanceof org.json.JSONObject) {
                        dataMap.put(key, val.toString());
                    } else {
                        dataMap.put(key, val.toString());
                    }
                }
            }
            
            Response res = new Response(status, message, dataMap);
            res.timestamp = timestamp;
            if (nonce != null && !nonce.isEmpty()) res.setNonce(nonce);
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

    public void putPayload(String key, Object value) {
        if (data == null) data = new HashMap<>();
        data.put(key, value);
    }

    public String getDataValue(String key) {
        Object val = data.get(key);
        return val != null ? val.toString() : null;
    }

    // ===== HELPER =====
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    @Override
    public String toString() { return toJson(); }
}
