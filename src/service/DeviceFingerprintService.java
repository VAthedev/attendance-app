package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.prefs.Preferences;

public class DeviceFingerprintService {

    private static String cachedDeviceId = null;

    /**
     * Lấy chuỗi định danh duy nhất của thiết bị vật lý.
     * Thử lấy UUID phần cứng của Windows (wmic csproduct get uuid).
     * Nếu thất bại, tạo một UUID lưu vào Registry (Preferences).
     */
    public static synchronized String getDeviceId() {
        if (cachedDeviceId != null) {
            return cachedDeviceId;
        }

        String deviceId = getHardwareUUID();

        if (deviceId == null || deviceId.isEmpty()) {
            // Fallback: Lưu vào thư mục Registry cục bộ (Java Preferences)
            Preferences prefs = Preferences.userNodeForPackage(DeviceFingerprintService.class);
            deviceId = prefs.get("app_device_fingerprint", null);
            if (deviceId == null) {
                deviceId = UUID.randomUUID().toString();
                prefs.put("app_device_fingerprint", deviceId);
            }
        }

        cachedDeviceId = deviceId;
        return cachedDeviceId;
    }

    private static String getHardwareUUID() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                ProcessBuilder builder = new ProcessBuilder("wmic", "csproduct", "get", "uuid");
                builder.redirectErrorStream(true);
                Process process = builder.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        // Dòng output thường có "UUID" ở dòng đầu, dòng 2 là mã thực sự
                        if (!line.isEmpty() && !line.equalsIgnoreCase("UUID")) {
                            process.waitFor();
                            return line;
                        }
                    }
                }
            } else if (os.contains("mac")) {
                ProcessBuilder builder = new ProcessBuilder("ioreg", "-rd1", "-c", "IOPlatformExpertDevice");
                Process process = builder.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("IOPlatformUUID")) {
                            return line.split("=")[1].trim().replaceAll("\"", "");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
