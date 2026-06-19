package service;

public class GPSVerifyService {

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
     * Lấy tọa độ GPS hiện tại của thiết bị.
     * Do đặc thù Java Desktop, ta sẽ mô phỏng vị trí gần trường để có thể test.
     */
    public Coordinates getCurrentLocation() {
        // Mô phỏng tọa độ ngẫu nhiên gần vị trí trường (bán kính khoảng 150m đổ lại)
        // Lấy tọa độ gốc là 10.8700, 106.8031 (UIT campus) và sai số một chút
        double baseLat = 10.8700;
        double baseLng = 106.8031;
        
        // Random offset từ -0.001 đến 0.001 (khoảng +-110m)
        double offsetLat = (Math.random() - 0.5) * 0.002;
        double offsetLng = (Math.random() - 0.5) * 0.002;
        
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
