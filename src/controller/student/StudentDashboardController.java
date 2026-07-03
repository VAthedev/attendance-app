package controller.student;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

import util.FxmlUtil;
import controller.widgets.StudentStatsWidgetController;
import controller.widgets.StudentTodayScheduleWidgetController;
import controller.widgets.StudentRecentAttendanceWidgetController;
import controller.widgets.StudentNotificationsWidgetController;

public class StudentDashboardController implements Initializable {

    @FXML private Button btnDashboard, btnScheduleDay, btnScheduleWeek;
    @FXML private Button btnScheduleSubject, btnAttendance, btnHistory;
    @FXML private Button btnStats, btnChat, btnNotification;

    @FXML private Label lblPageTitle, lblPageDate;
    @FXML private Label lblUserName, lblUserId;
    @FXML private Label lblNotifBadge;

    @FXML private ScrollPane paneDashboard;
    @FXML private Label      paneComingSoon;
    @FXML private StackPane  contentArea;
    @FXML private StackPane  aiChatWidget;
    @FXML private FlowPane   widgetContainer;

    private Node currentSubPane = null;
    public static String currentStudentId = "";

    private static final String[] WIDGET_FXMLS = {
        "/fxml/widgets/StudentStatsWidget.fxml",
        "/fxml/widgets/StudentTodayScheduleWidget.fxml",
        "/fxml/widgets/StudentRecentAttendanceWidget.fxml",
        "/fxml/widgets/StudentNotificationsWidget.fxml"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String today = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy",
            new java.util.Locale("vi", "VN")));
        lblPageDate.setText(today);
    }

    public void setUserInfo(String fullName, String studentId, String token) {
        lblUserName.setText(fullName.isEmpty() ? "Sinh vien" : fullName);
        lblUserId.setText("MSSV: " + studentId);
        currentStudentId = studentId;
        
        client.network.SocketClient.getInstance().addPushListener(res -> {
            if ("ANNOUNCEMENT".equals(res.getMessage())) {
                String message = res.getDataValue("message");
                if ("SCHEDULE_UPDATED".equals(message)) {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Thông báo");
                        alert.setHeaderText("Đồng bộ Thời khóa biểu");
                        alert.setContentText("Thời khóa biểu đã có sự thay đổi. Vui lòng làm mới trang.");
                        alert.show();
                        loadDashboardData(studentId);
                    });
                } else if (message != null && message.startsWith("SESSION_CLOSED:")) {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                        alert.setTitle("Thông báo Điểm danh");
                        alert.setHeaderText("Phiên điểm danh đã đóng");
                        alert.setContentText("Hệ thống đã tự động đóng phiên điểm danh.");
                        alert.show();
                        showDashboard(); 
                    });
                } else if (message != null && message.equals("SESSION_OPENED")) {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Thông báo Điểm danh");
                        alert.setHeaderText("Phiên điểm danh mới");
                        alert.setContentText("Giảng viên vừa mở một phiên điểm danh mới! Vui lòng tải lại trang.");
                        alert.show();
                        if (lblPageTitle.getText().equals("Điểm danh")) {
                            showAttendance(); 
                        }
                    });
                }
            }
        });

        loadDashboardData(studentId);
    }

    private void loadDashboardData(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;

        javafx.application.Platform.runLater(() -> {
            widgetContainer.getChildren().clear();
            for (int i = 0; i < WIDGET_FXMLS.length; i++) {
                loadWidget(WIDGET_FXMLS[i], i);
            }
        });
    }

    private void loadWidget(String fxmlPath, int index) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node widgetNode = loader.load();
            
            // Drag and Drop
            setupDragAndDrop(widgetNode, index);
            
            // Pass data
            Object controller = loader.getController();
            if (controller instanceof StudentStatsWidgetController) {
                ((StudentStatsWidgetController) controller).loadData(currentStudentId);
            } else if (controller instanceof StudentTodayScheduleWidgetController) {
                ((StudentTodayScheduleWidgetController) controller).loadData(currentStudentId);
            } else if (controller instanceof StudentRecentAttendanceWidgetController) {
                ((StudentRecentAttendanceWidgetController) controller).loadData(currentStudentId);
            } else if (controller instanceof StudentNotificationsWidgetController) {
                ((StudentNotificationsWidgetController) controller).loadData(currentStudentId);
            }

            widgetContainer.getChildren().add(widgetNode);
        } catch (Exception e) {
            System.err.println("Lỗi load widget " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupDragAndDrop(Node node, int index) {
        node.setId("student_widget_" + index);
        node.setStyle(node.getStyle() + "; -fx-cursor: hand;"); 

        node.setOnDragDetected((MouseEvent event) -> {
            Dragboard db = node.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(node.getId());
            db.setContent(content);
            event.consume();
        });

        node.setOnDragOver((DragEvent event) -> {
            if (event.getGestureSource() != node && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        node.setOnDragDropped((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String sourceId = db.getString();
                Node sourceNode = widgetContainer.lookup("#" + sourceId);
                if (sourceNode != null && sourceNode != node) {
                    int sourceIndex = widgetContainer.getChildren().indexOf(sourceNode);
                    int targetIndex = widgetContainer.getChildren().indexOf(node);
                    
                    widgetContainer.getChildren().remove(sourceNode);
                    widgetContainer.getChildren().remove(node);
                    
                    if (sourceIndex < targetIndex) {
                        widgetContainer.getChildren().add(sourceIndex, node);
                        widgetContainer.getChildren().add(targetIndex, sourceNode);
                    } else {
                        widgetContainer.getChildren().add(targetIndex, sourceNode);
                        widgetContainer.getChildren().add(sourceIndex, node);
                    }
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    // ===== NAVIGATION =====

    @FXML private void showDashboard() {
        setActiveBtn(btnDashboard); lblPageTitle.setText("Tổng quan");
        removeSubPane();
        paneDashboard.setVisible(true); paneDashboard.setManaged(true);
        paneComingSoon.setVisible(false); paneComingSoon.setManaged(false);
    }

    @FXML private void showAttendance() {
        setActiveBtn(btnAttendance); lblPageTitle.setText("Điểm danh");
        loadSubPane("/fxml/student/AttendanceGPS.fxml");
    }

    @FXML private void showScheduleDay()     { setActiveBtn(btnScheduleDay);     lblPageTitle.setText("TKB theo ngày");       loadSubPane("/fxml/student/ScheduleDay.fxml"); }
    @FXML private void showScheduleWeek()    { setActiveBtn(btnScheduleWeek);    lblPageTitle.setText("TKB theo tuần");       loadSubPane("/fxml/student/ScheduleWeek.fxml"); }
    @FXML private void showScheduleSubject() { setActiveBtn(btnScheduleSubject); lblPageTitle.setText("TKB theo môn");        loadSubPane("/fxml/student/ScheduleSubject.fxml"); }
    @FXML private void showHistory()         { setActiveBtn(btnHistory);         lblPageTitle.setText("Lịch sử điểm danh");  loadSubPane("/fxml/student/AttendanceHistory.fxml"); }
    @FXML private void showStats()           { setActiveBtn(btnStats);           lblPageTitle.setText("Thống kê chuyên cần"); loadSubPane("/fxml/student/AttendanceStats.fxml"); }
    @FXML private void showChat()            { setActiveBtn(btnChat);            lblPageTitle.setText("Chat lớp học"); loadSubPane("/fxml/shared/Chat.fxml"); }
    @FXML private void showNotification()    { setActiveBtn(btnNotification);    lblPageTitle.setText("Thông báo");           loadSubPane("/fxml/student/Notification.fxml"); }

    @FXML public void toggleAIChat() {
        if (aiChatWidget != null) {
            boolean isVisible = !aiChatWidget.isVisible();
            aiChatWidget.setVisible(isVisible);
            aiChatWidget.setManaged(isVisible);
        }
    }

    @FXML private void handleLogout() {
        loadScene("/fxml/auth/Login.fxml", "Dang nhap");
    }

    private void showComing() {
        removeSubPane();
        paneDashboard.setVisible(false);  paneDashboard.setManaged(false);
        paneComingSoon.setVisible(true);  paneComingSoon.setManaged(true);
    }

    private void loadSubPane(String fxmlPath) {
        try {
            removeSubPane();
            paneDashboard.setVisible(false);  paneDashboard.setManaged(false);
            paneComingSoon.setVisible(false);  paneComingSoon.setManaged(false);

            FXMLLoader loader = FxmlUtil.loader(fxmlPath);
            Node node = loader.load();
            contentArea.getChildren().add(node);
            currentSubPane = node;
        } catch (Exception e) {
            System.err.println("Loi load pane: " + e.getMessage());
            showComing();
        }
    }

    private void removeSubPane() {
        if (currentSubPane != null) {
            contentArea.getChildren().remove(currentSubPane);
            currentSubPane = null;
        }
    }

    private void setActiveBtn(Button active) {
        Button[] all = {btnDashboard, btnScheduleDay, btnScheduleWeek,
                        btnScheduleSubject, btnAttendance, btnHistory,
                        btnStats, btnChat, btnNotification};
        for (Button b : all) b.getStyleClass().remove("sidebar-btn-active");
        active.getStyleClass().add("sidebar-btn-active");
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = FxmlUtil.loader(fxmlPath);
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
        } catch (Exception e) {
            System.err.println("Loi navigate: " + e.getMessage());
        }
    }
}
