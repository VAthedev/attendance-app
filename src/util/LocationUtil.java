package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiện ích hỗ trợ xác thực vị trí bằng cả Wi-Fi và GPS (Windows Location Services).
 */
public class LocationUtil {

    /**
     * Lấy thông tin mạng Wi-Fi đang kết nối trên Windows.
     * 
     * @return Map chứa khóa "SSID" (Tên mạng) và "BSSID" (Mã cục phát)
     */
    public static Map<String, String> getCurrentWifiInfo() {
        Map<String, String> wifiInfo = new HashMap<>();
        
        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "netsh wlan show interfaces");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("SSID") && !line.startsWith("SSID name") && !line.startsWith("BSSID")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        wifiInfo.put("SSID", parts[1].trim());
                    }
                } 
                else if (line.startsWith("BSSID")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        wifiInfo.put("BSSID", parts[1].trim().toUpperCase());
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return wifiInfo;
    }

    /**
     * Lấy tọa độ GPS (Vĩ độ, Kinh độ) sử dụng Windows Location API thông qua PowerShell.
     * Yêu cầu: "Location Services" trên Windows 10/11 phải được bật (Settings > Privacy & security > Location).
     * 
     * @return Chuỗi tọa độ "Vĩ độ,Kinh độ" (vd: "10.8231,106.6297") hoặc null nếu thất bại.
     */
    public static String getCurrentGPSLocation() {
        try {
            // Lệnh PowerShell gọi API vị trí của Windows. Đợi 2 giây để API khởi động và bắt sóng
            String psCommand = "Add-Type -AssemblyName System.Device; " +
                               "$watcher = New-Object System.Device.Location.GeoCoordinateWatcher; " +
                               "$watcher.Start(); " +
                               "Start-Sleep -Seconds 2; " +
                               "$loc = $watcher.Position.Location; " +
                               "if ($loc.IsUnknown -eq $false) { Write-Output ($loc.Latitude.ToString() + ',' + $loc.Longitude.ToString()) } " +
                               "else { Write-Output 'UNKNOWN' }";
                               
            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", psCommand);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            String result = null;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains(",")) {
                    result = line;
                }
            }
            process.waitFor();
            
            if (result != null && !result.equals("UNKNOWN") && !result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Không lấy được vị trí
    }
    
    // Hàm main để test trực tiếp
    public static void main(String[] args) {
        System.out.println("Đang quét thông tin Wi-Fi...");
        Map<String, String> wifi = getCurrentWifiInfo();
        System.out.println("- Wi-Fi (SSID): " + wifi.get("SSID"));
        System.out.println("- Mã cục phát (BSSID): " + wifi.get("BSSID"));
        
        System.out.println("\nĐang lấy tọa độ GPS từ Windows (đợi khoảng 2 giây)...");
        String gps = getCurrentGPSLocation();
        if (gps != null) {
            System.out.println("✅ Tọa độ GPS (Vĩ độ, Kinh độ): " + gps);
        } else {
            System.out.println("❌ Không lấy được GPS. Hãy kiểm tra xem Settings > Location trên Windows đã bật chưa!");
        }
    }
}
