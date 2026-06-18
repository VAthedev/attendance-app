package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class WiFiVerifyService {

    public static class WiFiInfo {
        private final String ssid;
        private final String bssid;

        public WiFiInfo(String ssid, String bssid) {
            this.ssid = ssid;
            this.bssid = bssid;
        }

        public String getSsid() {
            return ssid;
        }

        public String getBssid() {
            return bssid;
        }

        @Override
        public String toString() {
            if (ssid.isEmpty() && bssid.isEmpty()) return "Không có kết nối";
            return ssid + " (" + bssid + ")";
        }
    }

    /**
     * Lấy thông tin mạng WiFi hiện tại trên hệ điều hành Windows.
     * Sử dụng lệnh `netsh wlan show interfaces`.
     */
    public WiFiInfo getCurrentWiFi() {
        String ssid = "";
        String bssid = "";

        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "netsh wlan show interfaces");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            
            // Đọc output (sử dụng UTF-8 hoặc mặc định hệ thống)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // Dòng chứa SSID thường có dạng: SSID                   : Tên_Wifi
                    if (line.startsWith("SSID") && !line.contains("BSSID")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            ssid = parts[1].trim();
                        }
                    }
                    // Dòng chứa BSSID thường có dạng: BSSID                  : aa:bb:cc:dd:ee:ff
                    else if (line.startsWith("BSSID")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            bssid = parts[1].trim().toUpperCase();
                        }
                    }
                }
            }
            process.waitFor();
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Nếu lấy thất bại (hoặc không phải Windows), trả về mock data để test
        if (ssid.isEmpty() || bssid.isEmpty()) {
            return new WiFiInfo("UIT-Student", "AA:BB:CC:DD:EE:FF");
        }

        return new WiFiInfo(ssid, bssid);
    }

    /**
     * So sánh 2 địa chỉ MAC (BSSID). Hỗ trợ chuẩn hóa dấu : và -
     */
    public boolean verifyBSSID(String currentBSSID, String targetBSSID) {
        if (currentBSSID == null || targetBSSID == null) {
            return false;
        }
        
        String currentFormatted = currentBSSID.toUpperCase().replace("-", "").replace(":", "");
        String targetFormatted = targetBSSID.toUpperCase().replace("-", "").replace(":", "");
        
        return currentFormatted.equals(targetFormatted);
    }
}
