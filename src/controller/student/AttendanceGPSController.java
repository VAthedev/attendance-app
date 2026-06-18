package controller.student;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

import service.GPSVerifyService;
import service.GPSVerifyService.Coordinates;
import service.WiFiVerifyService;
import service.WiFiVerifyService.WiFiInfo;

public class AttendanceGPSController implements Initializable {

    @FXML
    private Label lblSessionStatus;
    @FXML
    private Label lblHours;
    @FXML
    private Label lblMinutes;
    @FXML
    private Label lblSeconds;
    @FXML
    private ProgressBar timerProgress;
    @FXML
    private Label lblStartTime;
    @FXML
    private Label lblEndTime;
    @FXML
    private Label lblTimeRemaining;
    @FXML
    private VBox boxTimeWarning;
    @FXML
    private ProgressBar warningProgress;

    @FXML
    private Label lblCountdown;
    @FXML
    private VBox boxNoSession, boxSessionInfo;
    @FXML
    private Label lblSubjectName, lblScheduleInfo;
    @FXML
    private Label lblMethodGPS, lblMethodWiFi;

    @FXML
    private VBox boxGPS, boxWiFi;
    @FXML
    private VBox boxGPSWaiting, boxGPSResult;
    @FXML
    private VBox boxWiFiWaiting, boxWiFiResult;
    @FXML
    private ProgressIndicator gpsProgress;

    @FXML
    private Label lblMyLocation, lblGPSStatus, lblDistance;
    @FXML
    private Label lblClassLocation;
    @FXML
    private Label lblCurrentWiFi, lblWiFiStatus;

    @FXML
    private Label lblError;
    @FXML
    private Button btnCheckin;

    @FXML
    private VBox boxSuccess;
    @FXML
    private Label lblCheckinTime;

    private Timeline countdownTimeline;
    private LocalDateTime sessionStartTime;
    private LocalDateTime sessionEndTime;
    private long totalDurationSeconds;
    private boolean isWarningShown = false;
    private boolean isCriticalShown = false;

    private boolean gpsVerified = false;
    private boolean wifiVerified = false;
    private boolean hasSession = false;
    private String currentSessionId = "SESSION_001";

    private double currentLat = 0.0;
    private double currentLng = 0.0;
    private double classLat = 10.7770;
    private double classLng = 106.7010;
    private String classBSSID = "AA:BB:CC:DD:EE:FF";

    private GPSVerifyService gpsService = new GPSVerifyService();
    private WiFiVerifyService wifiService = new WiFiVerifyService();

    private static final double GPS_RADIUS_METERS = 100.0;
    private static final int WARNING_THRESHOLD_SECONDS = 300;
    private static final int CRITICAL_THRESHOLD_SECONDS = 60;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        gpsProgress.setVisible(false);
        setupTimerDisplay();
        checkSession();
    }

    private void setupTimerDisplay() {
        updateTimerDisplay(0, 0, 0);
        timerProgress.setProgress(0);
        boxTimeWarning.setVisible(false);
        boxTimeWarning.setManaged(false);
    }

    @FXML
    private void checkSession() {
        // TODO: Gui GET_ACTIVE_SESSION request qua SocketClient
        // Demo: gia lap co phien dang mo với thời gian cụ thể
        LocalDateTime now = LocalDateTime.now();
        sessionStartTime = now;
        sessionEndTime = now.plusMinutes(10);

        showSession("Lập trình mạng - NT204",
                "Thứ 3 - 07:30~09:10 - P.201",
                true, true, sessionStartTime, sessionEndTime);
    }

    private void showSession(String subject, String info,
            boolean gps, boolean wifi,
            LocalDateTime startTime, LocalDateTime endTime) {
        hasSession = true;
        lblSubjectName.setText(subject);
        lblScheduleInfo.setText(info);

        // Lưu thời gian
        sessionStartTime = startTime;
        sessionEndTime = endTime;
        totalDurationSeconds = ChronoUnit.SECONDS.between(sessionStartTime, sessionEndTime);

        // Hiển thị thời gian trên UI
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        lblStartTime.setText("Bắt đầu: " + sessionStartTime.format(timeFormatter));
        lblEndTime.setText("Kết thúc: " + sessionEndTime.format(timeFormatter));

        lblMethodGPS.setVisible(gps);
        lblMethodGPS.setManaged(gps);
        lblMethodWiFi.setVisible(wifi);
        lblMethodWiFi.setManaged(wifi);

        boxNoSession.setVisible(false);
        boxNoSession.setManaged(false);
        boxSessionInfo.setVisible(true);
        boxSessionInfo.setManaged(true);

        boxGPS.setVisible(gps);
        boxGPS.setManaged(gps);
        boxWiFi.setVisible(wifi);
        boxWiFi.setManaged(wifi);

        // Khởi động timer
        startTimer();
    }

    private void startTimer() {
        if (countdownTimeline != null && countdownTimeline.getStatus() == Timeline.Status.RUNNING) {
            countdownTimeline.stop();
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            updateTimer();
        }));

        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        updateTimer();
    }

    private void updateTimer() {
        LocalDateTime now = LocalDateTime.now();

        // Kiểm tra phiên đã kết thúc
        if (now.isAfter(sessionEndTime)) {
            handleSessionEnded();
            return;
        }

        // Kiểm tra phiên chưa bắt đầu
        if (now.isBefore(sessionStartTime)) {
            handleSessionNotStarted();
            return;
        }

        // Tính thời gian còn lại
        long secondsRemaining = ChronoUnit.SECONDS.between(now, sessionEndTime);
        long secondsElapsed = ChronoUnit.SECONDS.between(sessionStartTime, now);

        // Cập nhật hiển thị
        updateTimerDisplay(secondsRemaining);

        // Cập nhật progress bar
        double progress = (double) secondsElapsed / totalDurationSeconds;
        timerProgress.setProgress(progress);

        // Cập nhật trạng thái
        updateSessionStatus(secondsRemaining);

        // Cập nhật label thời gian còn lại
        updateTimeRemainingLabel(secondsRemaining);

        // Kiểm tra cảnh báo
        checkAndShowWarnings(secondsRemaining);
    }

    private void updateTimerDisplay(long secondsRemaining) {
        long hours = secondsRemaining / 3600;
        long minutes = (secondsRemaining % 3600) / 60;
        long seconds = secondsRemaining % 60;

        lblHours.setText(String.format("%02d", hours));
        lblMinutes.setText(String.format("%02d", minutes));
        lblSeconds.setText(String.format("%02d", seconds));

        lblCountdown.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void updateTimerDisplay(long hours, long minutes, long seconds) {
        lblHours.setText(String.format("%02d", hours));
        lblMinutes.setText(String.format("%02d", minutes));
        lblSeconds.setText(String.format("%02d", seconds));
        lblCountdown.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void updateTimeRemainingLabel(long secondsRemaining) {
        if (secondsRemaining >= 3600) {
            long hours = secondsRemaining / 3600;
            long minutes = (secondsRemaining % 3600) / 60;
            lblTimeRemaining.setText(String.format("Còn lại: %d giờ %d phút", hours, minutes));
        } else if (secondsRemaining >= 60) {
            long minutes = secondsRemaining / 60;
            lblTimeRemaining.setText(String.format("Còn lại: %d phút", minutes));
        } else {
            lblTimeRemaining.setText(String.format("Còn lại: %d giây", secondsRemaining));
        }
    }

    private void updateSessionStatus(long secondsRemaining) {
        if (secondsRemaining <= 0) {
            lblSessionStatus.setText("ĐÃ KẾT THÚC");
            lblSessionStatus.getStyleClass().clear();
            lblSessionStatus.getStyleClass().add("session-status-closed");
        } else if (secondsRemaining <= CRITICAL_THRESHOLD_SECONDS) {
            lblSessionStatus.setText("SẮP KẾT THÚC");
            lblSessionStatus.getStyleClass().clear();
            lblSessionStatus.getStyleClass().add("session-status-warning");
        } else if (secondsRemaining <= WARNING_THRESHOLD_SECONDS) {
            lblSessionStatus.setText("CÒN ÍT THỜI GIAN");
            lblSessionStatus.getStyleClass().clear();
            lblSessionStatus.getStyleClass().add("session-status-warning");
        } else {
            lblSessionStatus.setText("ĐANG MỞ");
            lblSessionStatus.getStyleClass().clear();
            lblSessionStatus.getStyleClass().add("session-status-open");
        }
    }

    private void checkAndShowWarnings(long secondsRemaining) {
        if (secondsRemaining <= WARNING_THRESHOLD_SECONDS && !isWarningShown && secondsRemaining > 0) {
            isWarningShown = true;
            showTimerWarning("⚠️ CÒN " + (secondsRemaining / 60) + " PHÚT ĐỂ ĐIỂM DANH! ⚠️");

            lblHours.getStyleClass().add("warning");
            lblMinutes.getStyleClass().add("warning");
            lblSeconds.getStyleClass().add("warning");
            timerProgress.getStyleClass().add("warning");
        }

        if (secondsRemaining <= CRITICAL_THRESHOLD_SECONDS && !isCriticalShown && secondsRemaining > 0) {
            isCriticalShown = true;
            showTimerWarning("🔴 HẾT GIỜ! CHỈ CÒN " + secondsRemaining + " GIÂY ĐỂ ĐIỂM DANH! 🔴");

            lblHours.getStyleClass().add("critical");
            lblMinutes.getStyleClass().add("critical");
            lblSeconds.getStyleClass().add("critical");
            timerProgress.getStyleClass().add("critical");
        }
    }

    private void showTimerWarning(String message) {
        javafx.application.Platform.runLater(() -> {
            boxTimeWarning.setVisible(true);
            boxTimeWarning.setManaged(true);

            Label warningLabel = (Label) boxTimeWarning.getChildren().get(0);
            warningLabel.setText(message);

            Timeline warningTimeline = new Timeline(new KeyFrame(Duration.seconds(0.05), e -> {
                double currentProgress = warningProgress.getProgress();
                if (currentProgress < 1.0) {
                    warningProgress.setProgress(currentProgress + 0.01);
                }
            }));
            warningTimeline.setCycleCount(100);
            warningTimeline.play();

            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    javafx.application.Platform.runLater(() -> {
                        boxTimeWarning.setVisible(false);
                        boxTimeWarning.setManaged(false);
                        warningProgress.setProgress(0);
                    });
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });
    }

    private void handleSessionEnded() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        javafx.application.Platform.runLater(() -> {
            updateTimerDisplay(0, 0, 0);
            timerProgress.setProgress(1.0);
            lblSessionStatus.setText("ĐÃ KẾT THÚC");
            lblSessionStatus.getStyleClass().clear();
            lblSessionStatus.getStyleClass().add("session-status-closed");
            lblTimeRemaining.setText("Đã hết thời gian điểm danh");

            btnCheckin.setDisable(true);
            if (!boxSuccess.isVisible()) {
                showError("Đã quá thời gian điểm danh!");
            }
        });
    }

    private void handleSessionNotStarted() {
        LocalDateTime now = LocalDateTime.now();
        long secondsToStart = ChronoUnit.SECONDS.between(now, sessionStartTime);

        javafx.application.Platform.runLater(() -> {
            updateTimerDisplay(secondsToStart);
            lblSessionStatus.setText("CHƯA BẮT ĐẦU");
            lblSessionStatus.getStyleClass().clear();
            lblSessionStatus.getStyleClass().add("session-status-closed");
            lblTimeRemaining.setText("Còn " + secondsToStart + " giây nữa bắt đầu");
            btnCheckin.setDisable(true);
        });
    }

    @FXML
    private void getGPSLocation() {
        gpsProgress.setVisible(true);
        btnCheckin.setDisable(true);
        lblError.setText("");

        new Thread(() -> {
            try {
                // Giả lập thời gian delay lấy GPS
                Thread.sleep(1500);
            } catch (InterruptedException e) {
            }
            
            Coordinates currentLoc = gpsService.getCurrentLocation();
            Coordinates classLoc = new Coordinates(classLat, classLng);
            
            double dist = gpsService.calculateDistance(
                currentLoc.getLatitude(), currentLoc.getLongitude(),
                classLoc.getLatitude(), classLoc.getLongitude()
            );

            boolean isWithin = gpsService.isWithinGeofence(currentLoc, classLoc, GPS_RADIUS_METERS);

            javafx.application.Platform.runLater(() -> {
                gpsProgress.setVisible(false);
                currentLat = currentLoc.getLatitude();
                currentLng = currentLoc.getLongitude();

                lblMyLocation.setText(currentLoc.toString());
                lblClassLocation.setText(classLoc.toString());
                lblDistance.setText(String.format("%.0fm", dist));

                boxGPSWaiting.setVisible(false);
                boxGPSWaiting.setManaged(false);
                boxGPSResult.setVisible(true);
                boxGPSResult.setManaged(true);

                if (isWithin) {
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
        boxWiFiWaiting.setVisible(false);
        boxWiFiWaiting.setManaged(false);

        new Thread(() -> {
            try {
                // Giả lập thời gian delay scan WiFi
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            WiFiInfo currentWiFi = wifiService.getCurrentWiFi();
            boolean isVerified = wifiService.verifyBSSID(currentWiFi.getBssid(), classBSSID);

            javafx.application.Platform.runLater(() -> {
                lblCurrentWiFi.setText(currentWiFi.toString());
                boxWiFiResult.setVisible(true);
                boxWiFiResult.setManaged(true);

                if (isVerified) {
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
            });
        }).start();
    }

    private void updateCheckinButton() {
        boolean canCheckin = (boxGPS.isVisible() ? gpsVerified : true)
                && (boxWiFi.isVisible() ? wifiVerified : true);

        boolean isWithinTime = LocalDateTime.now().isBefore(sessionEndTime);

        btnCheckin.setDisable(!(canCheckin && isWithinTime && !boxSuccess.isVisible()));

        if (canCheckin && !isWithinTime) {
            showError("Đã hết thời gian điểm danh!");
        }
    }

    @FXML
    private void handleCheckin() {
        btnCheckin.setDisable(true);
        btnCheckin.setText("Đang điểm danh...");

        String method = "";
        if (boxGPS.isVisible() && boxWiFi.isVisible())
            method = "GPS + WiFi";
        else if (boxGPS.isVisible())
            method = "GPS";
        else if (boxWiFi.isVisible())
            method = "WiFi";
            
        final String finalMethod = method;

        new Thread(() -> {
            try {
                protocol.Request req = new protocol.Request(protocol.RequestType.SUBMIT_ATTENDANCE);
                req.putPayload("session_id", currentSessionId);
                req.putPayload("device_id", service.DeviceFingerprintService.getDeviceId());
                req.putPayload("method", finalMethod);

                protocol.Response res = client.network.SocketClient.getInstance().sendRequest(req);

                javafx.application.Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        lblCheckinTime.setText("Thời gian: " + time + "  •  Phương thức: " + finalMethod);
                        boxSuccess.setVisible(true);
                        boxSuccess.setManaged(true);
                        btnCheckin.setVisible(false);
                        btnCheckin.setManaged(false);

                        if (countdownTimeline != null) {
                            countdownTimeline.stop();
                        }
                    } else {
                        btnCheckin.setDisable(false);
                        btnCheckin.setText("Điểm danh ngay");
                        showError(res.getMessage());
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    btnCheckin.setDisable(false);
                    btnCheckin.setText("Điểm danh ngay");
                    showError("Lỗi kết nối server: " + e.getMessage());
                });
            }
        }).start();
    }



    private void showError(String msg) {
        lblError.setText(msg);
    }

    public void cleanup() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }
}