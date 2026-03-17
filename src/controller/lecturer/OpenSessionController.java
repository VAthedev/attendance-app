package controller.lecturer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class OpenSessionController implements Initializable {

    @FXML private ComboBox<String> cmbSubject, cmbSchedule;
    @FXML private TextField txtDuration, txtLat, txtLng, txtRadius, txtBSSID;
    @FXML private CheckBox  chkGPS, chkWiFi;

    @FXML private VBox  boxActiveSession, boxGPSConfig, boxWiFiConfig, boxOpenBtn;
    @FXML private Label lblSessionSubject, lblSessionInfo;
    @FXML private Label lblCountdown, lblCheckedIn, lblError;
    @FXML private Button btnOpenSession, btnCloseSession;
    @FXML private ProgressBar progressAttendance;

    private Timeline countdownTimer;
    private int      remainingSeconds = 0;
    private int      currentSessionId = -1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Load danh sach mon hoc
        cmbSubject.getItems().addAll(
            "Lập trình mạng - NT204",
            "Cơ sở dữ liệu - IT002",
            "Giải thuật - IT003"
        );
        cmbSubject.setOnAction(e -> loadSchedules());

        // An/hien GPS/WiFi config
        chkGPS.setOnAction(e -> {
            boxGPSConfig.setVisible(chkGPS.isSelected());
            boxGPSConfig.setManaged(chkGPS.isSelected());
        });
        chkWiFi.setOnAction(e -> {
            boxWiFiConfig.setVisible(chkWiFi.isSelected());
            boxWiFiConfig.setManaged(chkWiFi.isSelected());
        });
    }

    private void loadSchedules() {
        cmbSchedule.getItems().clear();
        cmbSchedule.getItems().addAll(
            "Thứ 3 - 07:30~09:10 - P.201",
            "Thứ 5 - 13:00~14:40 - P.305"
        );
    }

    @FXML
    private void handleOpenSession() {
        // Validate
        if (cmbSubject.getValue() == null) { showError("Vui lòng chọn môn học."); return; }
        if (cmbSchedule.getValue() == null) { showError("Vui lòng chọn buổi học."); return; }

        String durationStr = txtDuration.getText().trim();
        int duration;
        try {
            duration = Integer.parseInt(durationStr);
            if (duration < 1 || duration > 60) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Thời gian phải là số từ 1-60 phút.");
            return;
        }

        if (!chkGPS.isSelected() && !chkWiFi.isSelected()) {
            showError("Phải chọn ít nhất 1 phương thức xác minh.");
            return;
        }

        lblError.setText("");
        btnOpenSession.setDisable(true);
        btnOpenSession.setText("Đang mở...");

        // TODO: Gui OPEN_SESSION request qua SocketClient
        // Map<String, Object> payload = new HashMap<>();
        // payload.put("scheduleId", selectedScheduleId);
        // payload.put("duration", duration);
        // payload.put("gpsLat",   txtLat.getText());
        // payload.put("gpsLng",   txtLng.getText());
        // payload.put("gpsRadius",txtRadius.getText());
        // payload.put("bssid",    txtBSSID.getText());
        // SocketClient.getInstance().sendAsync(new Request(RequestType.OPEN_SESSION, payload), res -> {
        //     if (res.isOk()) startCountdown(duration, Integer.parseInt(res.getDataValue("sessionId")));
        //     else showError(res.getMessage());
        // });

        // Demo tam thoi
        startCountdown(duration, 1);
    }

    private void startCountdown(int minutes, int sessionId) {
        currentSessionId  = sessionId;
        remainingSeconds  = minutes * 60;

        // Hien thi panel phien dang mo
        lblSessionSubject.setText(cmbSubject.getValue());
        lblSessionInfo.setText(cmbSchedule.getValue() + "  •  " + minutes + " phút");
        lblCheckedIn.setText("0 / 35 sinh viên");
        progressAttendance.setProgress(0);

        boxActiveSession.setVisible(true);
        boxActiveSession.setManaged(true);
        boxOpenBtn.setVisible(false);
        boxOpenBtn.setManaged(false);
        btnOpenSession.setDisable(false);
        btnOpenSession.setText("▶  Mở phiên điểm danh");

        // Bat dau dem nguoc
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            int min = remainingSeconds / 60;
            int sec = remainingSeconds % 60;
            lblCountdown.setText(String.format("%02d:%02d", min, sec));

            // Canh bao 1 phut cuoi
            if (remainingSeconds == 60) {
                lblCountdown.setStyle("-fx-font-size:28px;-fx-text-fill:#f59e0b;-fx-font-weight:bold;");
            }
            if (remainingSeconds == 0) {
                stopCountdown("Phiên đã tự động đóng.");
            }
        }));
        countdownTimer.setCycleCount(minutes * 60);
        countdownTimer.play();
    }

    @FXML
    private void handleCloseSession() {
        stopCountdown("Phiên đã đóng sớm.");
        // TODO: Gui CLOSE_SESSION request
    }

    @FXML
    private void refreshSession() {
        lblCheckedIn.setText("12 / 35 sinh viên");
        progressAttendance.setProgress(12.0 / 35.0);
        // TODO: Gui GET_SESSION_STATUS request
    }

    private void stopCountdown(String msg) {
        if (countdownTimer != null) countdownTimer.stop();
        boxActiveSession.setVisible(false);
        boxActiveSession.setManaged(false);
        boxOpenBtn.setVisible(true);
        boxOpenBtn.setManaged(true);
        lblError.setText(msg);
        lblError.setStyle("-fx-text-fill:#16a34a;-fx-background-color:#f0fdf4;" +
                          "-fx-background-radius:6;-fx-padding:6 10 6 10;");
        currentSessionId = -1;
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setStyle(null);
        lblError.getStyleClass().setAll("error-label");
    }
}
