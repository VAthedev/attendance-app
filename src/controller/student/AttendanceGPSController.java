package controller.student;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class AttendanceGPSController implements Initializable {

    @FXML private Label lblCountdown;
    @FXML private VBox  boxNoSession, boxSessionInfo;
    @FXML private Label lblSubjectName, lblScheduleInfo;
    @FXML private Label lblMethodGPS, lblMethodWiFi;

    @FXML private VBox  boxGPS, boxWiFi;
    @FXML private VBox  boxGPSWaiting, boxGPSResult;
    @FXML private VBox  boxWiFiWaiting, boxWiFiResult;
    @FXML private ProgressIndicator gpsProgress;

    @FXML private Label  lblMyLocation, lblGPSStatus, lblDistance;
    @FXML private Label  lblClassLocation;
    @FXML private Label  lblCurrentWiFi, lblWiFiStatus;

    @FXML private Label  lblError;
    @FXML private Button btnCheckin;

    @FXML private VBox  boxSuccess;
    @FXML private Label lblCheckinTime;

    private boolean gpsVerified  = false;
    private boolean wifiVerified = false;
    private boolean hasSession   = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        gpsProgress.setVisible(false);
        checkSession();
    }

    @FXML
    private void checkSession() {
        // TODO: Gui GET_ACTIVE_SESSION request qua SocketClient
        // Demo: gia lap co phien dang mo
        showSession("Lập trình mạng - NT204",
                    "Thứ 3 - 07:30~09:10 - P.201",
                    true, true, "12:00");
    }

    private void showSession(String subject, String info,
                              boolean gps, boolean wifi, String countdown) {
        hasSession = true;
        lblSubjectName.setText(subject);
        lblScheduleInfo.setText(info);
        lblCountdown.setText(countdown);

        lblMethodGPS.setVisible(gps);   lblMethodGPS.setManaged(gps);
        lblMethodWiFi.setVisible(wifi); lblMethodWiFi.setManaged(wifi);

        boxNoSession.setVisible(false);  boxNoSession.setManaged(false);
        boxSessionInfo.setVisible(true); boxSessionInfo.setManaged(true);

        boxGPS.setVisible(gps);   boxGPS.setManaged(gps);
        boxWiFi.setVisible(wifi); boxWiFi.setManaged(wifi);
    }

    @FXML
    private void getGPSLocation() {
        gpsProgress.setVisible(true);
        btnCheckin.setDisable(true);

        // TODO: Dung LocationUtil.getCurrentLocation() thuc te
        // Gia lap sau 1.5 giay lay duoc vi tri
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> {
                gpsProgress.setVisible(false);

                // Gia lap: vi tri hop le (trong ban kinh 100m)
                double myLat  = 10.7769;
                double myLng  = 106.7009;
                double clsLat = 10.7770;
                double clsLng = 106.7010;
                double dist   = calcDistance(myLat, myLng, clsLat, clsLng);

                lblMyLocation.setText(String.format("%.6f, %.6f", myLat, myLng));
                lblClassLocation.setText(String.format("%.6f, %.6f", clsLat, clsLng));
                lblDistance.setText(String.format("%.0fm", dist));

                boxGPSWaiting.setVisible(false); boxGPSWaiting.setManaged(false);
                boxGPSResult.setVisible(true);   boxGPSResult.setManaged(true);

                if (dist <= 100) {
                    gpsVerified = true;
                    lblGPSStatus.setText("✓ Hợp lệ");
                    lblGPSStatus.getStyleClass().setAll("badge-present");
                    lblDistance.getStyleClass().setAll("badge-present");
                } else {
                    gpsVerified = false;
                    lblGPSStatus.setText("✗ Ngoài phạm vi");
                    lblGPSStatus.getStyleClass().setAll("badge-absent");
                    lblDistance.getStyleClass().setAll("badge-absent");
                    showError("Bạn đang ngoài phạm vi lớp học (" + String.format("%.0f", dist) + "m).");
                }
                updateCheckinButton();
            });
        }).start();
    }

    @FXML
    private void scanWiFi() {
        // TODO: Dung WiFiVerifyService thuc te
        // Gia lap: dung dung WiFi truong
        String currentBSSID = "AA:BB:CC:DD:EE:FF";
        String classBSSID   = "AA:BB:CC:DD:EE:FF";

        lblCurrentWiFi.setText("UIT-Student  (" + currentBSSID + ")");
        boxWiFiWaiting.setVisible(false); boxWiFiWaiting.setManaged(false);
        boxWiFiResult.setVisible(true);   boxWiFiResult.setManaged(true);

        if (currentBSSID.equals(classBSSID)) {
            wifiVerified = true;
            lblWiFiStatus.setText("✓ Hợp lệ");
            lblWiFiStatus.getStyleClass().setAll("badge-present");
        } else {
            wifiVerified = false;
            lblWiFiStatus.setText("✗ Sai mạng");
            lblWiFiStatus.getStyleClass().setAll("badge-absent");
            showError("Bạn không kết nối đúng WiFi của lớp học.");
        }
        updateCheckinButton();
    }

    private void updateCheckinButton() {
        boolean canCheckin = (boxGPS.isVisible() ? gpsVerified : true)
                          && (boxWiFi.isVisible() ? wifiVerified : true);
        btnCheckin.setDisable(!canCheckin);
        if (canCheckin) lblError.setText("");
    }

    @FXML
    private void handleCheckin() {
        btnCheckin.setDisable(true);
        btnCheckin.setText("Đang điểm danh...");

        // TODO: Gui CHECKIN_GPS hoac CHECKIN_WIFI request
        // Map payload = Map.of("sessionId", currentSessionId,
        //                      "gpsLat", myLat, "gpsLng", myLng,
        //                      "deviceId", DeviceFingerprintService.getDeviceId());

        // Demo thanh cong
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        lblCheckinTime.setText("Thời gian: " + time + "  •  Phương thức: GPS + WiFi");
        boxSuccess.setVisible(true);
        boxSuccess.setManaged(true);
        btnCheckin.setVisible(false);
        btnCheckin.setManaged(false);
    }

    private double calcDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371000; // ban kinh Trai Dat (m)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng/2) * Math.sin(dLng/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    private void showError(String msg) { lblError.setText(msg); }
}
