package controller.lecturer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class OpenSessionController implements Initializable {

    // Step 1: Select class
    @FXML private ComboBox<String> cbClasses;
    @FXML private Label lblSelectedSubject, lblLecturerName;

    // Step 2: Configure session
    @FXML private DatePicker dpSessionDate;
    @FXML private Spinner<Integer> spHour, spMinute, spDuration;
    @FXML private CheckBox cbGPS, cbWiFi;
    @FXML private TextField tfRoom, txtLat, txtLng, txtRadius, txtBSSID;
    @FXML private TextArea taSessionNotes;

    // Step 3: Preview & Confirm
    @FXML private Label lblPreviewClass, lblPreviewSubject, lblPreviewLecturer;
    @FXML private Label lblPreviewDateTime, lblPreviewDuration, lblPreviewRoom, lblPreviewVerification;
    @FXML private Label lblSessionSubject, lblSessionInfo, lblCountdown;
    @FXML private HBox boxActiveSessionDisplay, boxCloseButton;
    @FXML private Label lblError;
    @FXML private Button btnOpenSession, btnCloseSession;

    // VBox for GPS and WiFi config
    @FXML private VBox boxGPSConfig, boxWiFiConfig;

    private Timeline countdownTimer;
    private int remainingSeconds = 0;
    private int currentSessionId = -1;
    private ClassInfo selectedClass;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeClasses();
        setupDateTimeSpinners();
        setupConfigVisibility();
    }

    private void initializeClasses() {
        // Mock data - replace with database
        cbClasses.getItems().addAll(
            "CTK43A - Lập trình mạng",
            "CTK43A - Cơ sở dữ liệu",
            "CTK43B - Giải thuật"
        );
    }

    private void setupDateTimeSpinners() {
        dpSessionDate.setValue(LocalDate.now());
        
        LocalTime now = LocalTime.now();
        SpinnerValueFactory<Integer> hourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, now.getHour());
        SpinnerValueFactory<Integer> minuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, now.getMinute());
        SpinnerValueFactory<Integer> durationFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 180, 50, 5);

        spHour.setValueFactory(hourFactory);
        spMinute.setValueFactory(minuteFactory);
        spDuration.setValueFactory(durationFactory);
    }

    private void setupConfigVisibility() {
        cbGPS.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boxGPSConfig.setVisible(newVal);
            boxGPSConfig.setManaged(newVal);
        });

        cbWiFi.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boxWiFiConfig.setVisible(newVal);
            boxWiFiConfig.setManaged(newVal);
        });

        boxGPSConfig.setVisible(true);
        boxWiFiConfig.setVisible(true);
    }

    @FXML
    public void handleClassSelected() {
        String selected = cbClasses.getValue();
        if (selected == null || selected.isEmpty()) {
            lblSelectedSubject.setText("--");
            lblLecturerName.setText("--");
            selectedClass = null;
            return;
        }

        // Parse selected class
        String[] parts = selected.split(" - ");
        selectedClass = new ClassInfo(parts[0], parts.length > 1 ? parts[1] : "", "TS. Nguyễn Văn A");
        
        lblSelectedSubject.setText(selectedClass.subject);
        lblLecturerName.setText(selectedClass.lecturer);

        // Update preview
        updatePreview();
    }

    private void updatePreview() {
        if (selectedClass == null) return;

        lblPreviewClass.setText(selectedClass.className);
        lblPreviewSubject.setText(selectedClass.subject);
        lblPreviewLecturer.setText(selectedClass.lecturer);

        LocalDate date = dpSessionDate.getValue();
        int hour = spHour.getValue();
        int minute = spMinute.getValue();
        String dateTimeStr = String.format("%s lúc %02d:%02d", 
            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), hour, minute);
        lblPreviewDateTime.setText(dateTimeStr);

        int duration = spDuration.getValue();
        lblPreviewDuration.setText(duration + " phút");

        lblPreviewRoom.setText(tfRoom.getText().isEmpty() ? "--" : tfRoom.getText());

        String verification = "";
        if (cbGPS.isSelected()) verification += "GPS ";
        if (cbWiFi.isSelected()) verification += "WiFi";
        lblPreviewVerification.setText(verification.isEmpty() ? "--" : verification);
    }

    @FXML
    public void handleStartSession() {
        // Validate
        if (selectedClass == null) {
            showError("Vui lòng chọn lớp học.");
            return;
        }
        if (tfRoom.getText().trim().isEmpty()) {
            showError("Vui lòng nhập phòng học.");
            return;
        }
        if (!cbGPS.isSelected() && !cbWiFi.isSelected()) {
            showError("Vui lòng chọn ít nhất một phương thức xác minh.");
            return;
        }

        if (cbGPS.isSelected()) {
            if (txtLat.getText().trim().isEmpty() || txtLng.getText().trim().isEmpty()) {
                showError("Vui lòng nhập tọa độ GPS.");
                return;
            }
        }

        if (cbWiFi.isSelected() && txtBSSID.getText().trim().isEmpty()) {
            showError("Vui lòng nhập BSSID WiFi.");
            return;
        }

        lblError.setText("");
        btnOpenSession.setDisable(true);

        // TODO: Send OPEN_SESSION request to server
        // For now, start countdown demo
        int duration = spDuration.getValue();
        startCountdown(duration);
    }

    private void startCountdown(int minutes) {
        remainingSeconds = minutes * 60;
        currentSessionId = 1;

        // Update preview to show active session
        lblSessionSubject.setText(selectedClass.subject);
        lblSessionInfo.setText(selectedClass.className + "  •  " + selectedClass.lecturer + "  •  " + minutes + " phút");

        // Show active session display and close button
        boxActiveSessionDisplay.setVisible(true);
        boxActiveSessionDisplay.setManaged(true);
        boxCloseButton.setVisible(true);
        boxCloseButton.setManaged(true);

        // Start countdown timer
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            int min = remainingSeconds / 60;
            int sec = remainingSeconds % 60;
            lblCountdown.setText(String.format("%02d:%02d", min, sec));

            // Change color when 1 minute left
            if (remainingSeconds == 60) {
                lblCountdown.setStyle("-fx-font-size: 28px; -fx-text-fill: #f59e0b; -fx-font-weight: bold;");
            }
            if (remainingSeconds == 0) {
                stopCountdown("Phiên đã tự động đóng.", true);
            }
        }));
        countdownTimer.setCycleCount(minutes * 60);
        countdownTimer.play();
    }

    @FXML
    public void handleCloseSession() {
        stopCountdown("Phiên đã đóng sớm.", false);
        // TODO: Send CLOSE_SESSION request to server
    }

    @FXML
    public void handleRefresh() {
        // TODO: Send GET_SESSION_STATUS request to get updated attendance count
    }

    @FXML
    public void handleCancel() {
        cbClasses.setValue(null);
        dpSessionDate.setValue(LocalDate.now());
        spHour.getValueFactory().setValue(LocalTime.now().getHour());
        spMinute.getValueFactory().setValue(LocalTime.now().getMinute());
        spDuration.getValueFactory().setValue(50);
        tfRoom.setText("");
        txtLat.setText("");
        txtLng.setText("");
        txtRadius.setText("100");
        txtBSSID.setText("");
        taSessionNotes.setText("");
        cbGPS.setSelected(true);
        cbWiFi.setSelected(true);
        lblError.setText("");
        handleClassSelected();
    }

    private void stopCountdown(String msg, boolean autoClose) {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        
        boxActiveSessionDisplay.setVisible(false);
        boxActiveSessionDisplay.setManaged(false);
        
        if (autoClose) {
            boxCloseButton.setVisible(false);
            boxCloseButton.setManaged(false);
        }

        lblError.setText(msg);
        if (autoClose) {
            lblError.setStyle("-fx-text-fill: #16a34a; -fx-background-color: #f0fdf4;");
        }

        btnOpenSession.setDisable(false);
        btnOpenSession.setText("🚀  Mở phiên điểm danh");
        currentSessionId = -1;
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.getStyleClass().setAll("error-label");
    }

    // ===== STEP UPDATE EVENT HANDLERS =====
    @FXML
    public void handleDateChanged() {
        updatePreview();
    }

    @FXML
    public void handleTimeChanged() {
        updatePreview();
    }

    @FXML
    public void handleDurationChanged() {
        updatePreview();
    }

    @FXML
    public void handleRoomChanged() {
        updatePreview();
    }

    private static class ClassInfo {
        String className, subject, lecturer;

        ClassInfo(String className, String subject, String lecturer) {
            this.className = className;
            this.subject = subject;
            this.lecturer = lecturer;
        }
    }
}
