package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class GPSVerifyService {

    // CHÚ Ý: Điền API Key của Google Geolocation vào đây.
    // Nếu để trống, hệ thống sẽ tự động dùng toạ độ giả lập (Mock GPS).
    private static final String GOOGLE_API_KEY = ""; 

    public static class Coordinates {
        private final double latitude;
        private final double longitude;

        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        @Override
        public String toString() {
            return String.format("%.6f, %.6f", latitude, longitude);
        }
    }

    /**
     * Lấy danh sách địa chỉ MAC (BSSID) của các mạng Wi-Fi xung quanh (chỉ hỗ trợ Windows).
     */
    private List<String> getNearbyWifiBSSIDs() {
        List<String> bssids = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("netsh wlan show networks mode=bssid");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8")); // netsh output
            
            String line;
            Pattern macPattern = Pattern.compile("BSSID.*?:\\s*([0-9a-fA-F:]+)");
            while ((line = reader.readLine()) != null) {
                Matcher matcher = macPattern.matcher(line);
                if (matcher.find()) {
                    bssids.add(matcher.group(1));
                }
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("Lỗi quét Wi-Fi: " + e.getMessage());
        }
        return bssids;
    }

    /**
     * Lấy tọa độ GPS thực tế qua Google Geolocation API (dựa trên Wi-Fi xung quanh).
     * Nếu không có API Key hoặc bị lỗi, tự động lùi về giả lập (Mock).
     */
    public Coordinates getCurrentLocation() {
        if (GOOGLE_API_KEY == null || GOOGLE_API_KEY.trim().isEmpty()) {
            return getMockLocation();
        }

        List<String> bssids = getNearbyWifiBSSIDs();
        if (bssids.isEmpty()) {
            System.err.println("Không tìm thấy Wi-Fi nào xung quanh. Dùng toạ độ giả lập.");
            return getMockLocation();
        }

        try {
            JSONObject payload = new JSONObject();
            JSONArray wifiList = new JSONArray();
            for (String mac : bssids) {
                JSONObject ap = new JSONObject();
                ap.put("macAddress", mac);
                wifiList.put(ap);
            }
            payload.put("wifiAccessPoints", wifiList);

            URL url = new URL("https://www.googleapis.com/geolocation/v1/geolocate?key=" + GOOGLE_API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONObject location = jsonResponse.getJSONObject("location");
                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");
                return new Coordinates(lat, lng);
            } else {
                System.err.println("Google API lỗi: " + code + ". Dùng toạ độ giả lập.");
                return getMockLocation();
            }
        } catch (Exception e) {
            System.err.println("Lỗi kết nối Geolocation API: " + e.getMessage());
            return getMockLocation();
        }
    }

    /**
     * Cơ chế dự phòng (Mock GPS) cho máy tính bàn khi không có API Key.
     */
    private Coordinates getMockLocation() {
        double baseLat = 10.8700;
        double baseLng = 106.8031;
        
        // Random offset từ -0.0004 đến 0.0004 (khoảng +-45m, max distance ~63m)
        double offsetLat = (Math.random() - 0.5) * 0.0008;
        double offsetLng = (Math.random() - 0.5) * 0.0008;
        
        return new Coordinates(baseLat + offsetLat, baseLng + offsetLng);
    }

    /**
     * Tính toán khoảng cách (theo mét) giữa 2 tọa độ bằng công thức Haversine.
     */
    public double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371000; // Bán kính Trái Đất (m)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Kiểm tra xem tọa độ hiện tại có nằm trong bán kính Geofence hay không.
     */
    public boolean isWithinGeofence(Coordinates current, Coordinates target, double radiusMeters) {
        double distance = calculateDistance(
            current.getLatitude(), current.getLongitude(),
            target.getLatitude(), target.getLongitude()
        );
        return distance <= radiusMeters;
    }
}
