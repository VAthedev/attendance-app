package protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Request {

    private String              type;
    private Map<String, Object> payload;
    private String              nonce;
    private long                timestamp;

    public Request(RequestType type, Map<String, Object> payload) {
        this.type      = type.name();
        this.payload   = payload != null ? new HashMap<>(payload) : new HashMap<>();
        this.nonce     = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        this.timestamp = System.currentTimeMillis();
    }

    public Request(RequestType type) {
        this(type, null);
    }

    // ===== SERIALIZE sang JSON =====
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"type\":\"").append(type).append("\",");
        sb.append("\"nonce\":\"").append(nonce).append("\",");
        sb.append("\"timestamp\":").append(timestamp).append(",");
        sb.append("\"payload\":{");
        int i = 0;
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(escapeJson(e.getKey())).append("\":");
            sb.append("\"").append(escapeJson(String.valueOf(e.getValue()))).append("\"");
        }
        sb.append("}}");
        return sb.toString();
    }

    // ===== PARSE tu JSON =====
    public static Request fromJson(String json) {
        try {
            String type  = extractString(json, "type");
            String nonce = extractString(json, "nonce");
            long   ts    = extractLong(json, "timestamp");

            Map<String, Object> payload = new HashMap<>();
            int ps = json.indexOf("\"payload\":{");
            if (ps >= 0) {
                ps += 11;
                int pe = findClosingBrace(json, ps - 1);
                if (pe > ps) {
                    String payloadStr = json.substring(ps, pe);
                    payload = parseObject(payloadStr);
                }
            }

            Request req   = new Request(RequestType.valueOf(type), payload);
            req.nonce     = nonce;
            req.timestamp = ts;
            return req;
        } catch (Exception e) {
            Request err = new Request(RequestType.ERROR);
            err.payload.put("message", "Parse error: " + e.getMessage());
            return err;
        }
    }

    // ===== GETTERS =====
    public String              getType()        { return type; }
    public RequestType         getRequestType() { return RequestType.valueOf(type); }
    public Map<String, Object> getPayload()     { return payload; }
    public String              getNonce()       { return nonce; }
    public long                getTimestamp()   { return timestamp; }

    public String getPayloadValue(String key) {
        Object v = payload.get(key);
        return v != null ? v.toString() : null;
    }

    // ===== HELPER =====
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int s = json.indexOf(search);
        if (s < 0) return "";
        s += search.length();
        int e = s;
        while (e < json.length()) {
            if (json.charAt(e) == '"' && json.charAt(e - 1) != '\\') break;
            e++;
        }
        return json.substring(s, e);
    }

    private static long extractLong(String json, String key) {
        String search = "\"" + key + "\":";
        int s = json.indexOf(search);
        if (s < 0) return 0;
        s += search.length();
        int e = s;
        while (e < json.length() && Character.isDigit(json.charAt(e))) e++;
        try { return Long.parseLong(json.substring(s, e)); }
        catch (Exception ex) { return 0; }
    }

    // Tim vi tri dong ngoac } tuong ung
    private static int findClosingBrace(String json, int openPos) {
        int depth = 0;
        for (int i = openPos; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return json.length() - 1;
    }

    private static Map<String, Object> parseObject(String json) {
        Map<String, Object> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;
        json = json.trim();

        int i = 0;
        while (i < json.length()) {
            // Tim key
            int ks = json.indexOf('"', i);
            if (ks < 0) break;
            int ke = json.indexOf('"', ks + 1);
            if (ke < 0) break;
            String key = json.substring(ks + 1, ke);

            // Tim dau :
            int colon = json.indexOf(':', ke + 1);
            if (colon < 0) break;

            // Tim value
            int vs = colon + 1;
            while (vs < json.length() && json.charAt(vs) == ' ') vs++;

            String value;
            if (vs < json.length() && json.charAt(vs) == '"') {
                // String value
                int ve = vs + 1;
                while (ve < json.length()) {
                    if (json.charAt(ve) == '"' && json.charAt(ve - 1) != '\\') break;
                    ve++;
                }
                value = json.substring(vs + 1, ve);
                i = ve + 1;
            } else {
                // Number / boolean value
                int ve = vs;
                while (ve < json.length() && json.charAt(ve) != ',' && json.charAt(ve) != '}') ve++;
                value = json.substring(vs, ve).trim();
                i = ve + 1;
            }
            map.put(key, value);
        }
        return map;
    }

    @Override
    public String toString() { return toJson(); }
}
