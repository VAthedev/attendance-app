package controller.lecturer;

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
import controller.widgets.StatsWidgetController;
import controller.widgets.TodayScheduleWidgetController;
import controller.widgets.ActiveSessionsWidgetController;
import controller.widgets.AbsentWarningWidgetController;

public class LecturerDashboardController implements Initializable {

    @FXML private Button btnDashboard, btnSchedule, btnOpenSession;
    @FXML private Button btnAttendanceList, btnStatistics, btnExport;
    @FXML private Button btnChat, btnNotification;

    @FXML private Label  lblPageTitle, lblPageDate;
    @FXML private Button btnQuickOpen;

    @FXML private Label lblUserName, lblUserCode;

    @FXML private ScrollPane paneDashboard;
    @FXML private Label      paneComingSoon;
    @FXML private StackPane  contentArea;
    @FXML private StackPane  aiChatWidget;
    @FXML private FlowPane   widgetContainer;

    public static String currentLecturerId = "";
    public static String currentLecturerName = "";

    private Node currentSubPane = null;
    private String sessionToken = "";

    private static final String[] WIDGET_FXMLS = {
        "/fxml/widgets/StatsWidget.fxml",
        "/fxml/widgets/TodayScheduleWidget.fxml",
        "/fxml/widgets/ActiveSessionsWidget.fxml",
        "/fxml/widgets/AbsentWarningWidget.fxml"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String today = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy",
            new java.util.Locale("vi", "VN")));
        lblPageDate.setText(today);
    }

    public void setUserInfo(String fullName, String userCode, String token) {
        this.sessionToken = token;
        lblUserName.setText(fullName.isEmpty() ? "Giang vien" : fullName);
        lblUserCode.setText("Ma GV: " + userCode.replace("}", ""));
        currentLecturerId = lblUserCode.getText().replace("Ma GV: ", "").trim();
        currentLecturerName = lblUserName.getText();

        client.network.SocketClient.getInstance().addPushListener(res -> {
            if ("ANNOUNCEMENT".equals(res.getMessage()) && "SCHEDULE_UPDATED".equals(res.getDataValue("message"))) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Thông báo");
                alert.setHeaderText("Đồng bộ Thời khóa biểu");
                alert.setContentText("Thời khóa biểu đã có sự thay đổi. Vui lòng làm mới trang.");
                alert.show();
                loadDashboardData();
            }
        });
        
        javafx.application.Platform.runLater(this::loadDashboardData);
    }

    @FXML
    private void handleSyncTKB(javafx.event.ActionEvent event) {
        protocol.Request req = new protocol.Request(protocol.RequestType.SYNC_SCHEDULE);
        client.network.SocketClient.getInstance().sendAsync(req, res -> {
            if (res.isOk()) {
                System.out.println("Gửi yêu cầu đồng bộ thành công.");
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Lỗi");
                alert.setContentText(res.getMessage());
                alert.show();
            }
        });
    }

    private void loadDashboardData() {
        if (currentLecturerName == null || currentLecturerName.isEmpty() || "Giang vien".equals(currentLecturerName)) {
            return;
        }

        widgetContainer.getChildren().clear();
        for (int i = 0; i < WIDGET_FXMLS.length; i++) {
            loadWidget(WIDGET_FXMLS[i], i);
        }
    }

    private void loadWidget(String fxmlPath, int index) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node widgetNode = loader.load();
            
            // Cho phép widget linh hoạt Drag & Drop
            setupDragAndDrop(widgetNode, index);
            
            // Chuyển dữ liệu vào các Controller con
            Object controller = loader.getController();
            if (controller instanceof StatsWidgetController) {
                ((StatsWidgetController) controller).loadData(currentLecturerName);
            } else if (controller instanceof TodayScheduleWidgetController) {
                ((TodayScheduleWidgetController) controller).loadData(currentLecturerName);
            } else if (controller instanceof ActiveSessionsWidgetController) {
                ((ActiveSessionsWidgetController) controller).loadData(currentLecturerId);
            } else if (controller instanceof AbsentWarningWidgetController) {
                ((AbsentWarningWidgetController) controller).loadData(currentLecturerId);
            }

            widgetContainer.getChildren().add(widgetNode);
        } catch (Exception e) {
            System.err.println("Lỗi load widget " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupDragAndDrop(Node node, int index) {
        node.setId("widget_" + index);
        node.setStyle(node.getStyle() + "; -fx-cursor: hand;"); // Đổi con trỏ chuột khi chỉ vào

        node.setOnDragDetected((MouseEvent event) -> {
            Dragboard db = node.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(node.getId()); // Lưu ID của Widget đang bị kéo
            db.setContent(content);
            event.consume();
        });

        node.setOnDragOver((DragEvent event) -> {
            // Chấp nhận thả nếu không phải tự thả vào chính nó
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
                    
                    // Hoán đổi vị trí trong FlowPane
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
                    // TODO: Tại đây có thể lưu lại thứ tự mới vào file cấu hình / database
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
        paneDashboard.setVisible(true);  paneDashboard.setManaged(true);
        paneComingSoon.setVisible(false); paneComingSoon.setManaged(false);
    }

    @FXML private void showOpenSession() {
        setActiveBtn(btnOpenSession); lblPageTitle.setText("Mở phiên điểm danh");
        loadSubPane("/fxml/lecturer/OpenSession.fxml");
    }

    @FXML private void showSchedule()       { setActiveBtn(btnSchedule);       lblPageTitle.setText("Quản lý TKB");          loadSubPane("/fxml/lecturer/LecturerSchedule.fxml"); }
    @FXML private void showAttendanceList() { setActiveBtn(btnAttendanceList); lblPageTitle.setText("Danh sách điểm danh");  loadSubPane("/fxml/lecturer/LecturerAttendanceList.fxml"); }
    @FXML private void showStatistics()     { setActiveBtn(btnStatistics);     lblPageTitle.setText("Thống kê chuyên cần");  loadSubPane("/fxml/lecturer/LecturerStatistics.fxml"); }
    @FXML private void showExport()         { setActiveBtn(btnExport);         lblPageTitle.setText("Xuất danh sách");       loadSubPane("/fxml/lecturer/LecturerExport.fxml"); }
    @FXML private void showChat()           { setActiveBtn(btnChat);           lblPageTitle.setText("Chat nội bộ lớp học");  loadSubPane("/fxml/shared/Chat.fxml"); }
    @FXML private void showNotification()   { setActiveBtn(btnNotification);   lblPageTitle.setText("Thông báo");            loadSubPane("/fxml/student/Notification.fxml"); }

    @FXML private void handleLogout() { loadScene("/fxml/auth/Login.fxml", "Dang nhap"); }

    @FXML
    private void toggleAIChat() {
        boolean isVisible = aiChatWidget.isVisible();
        aiChatWidget.setVisible(!isVisible);
        aiChatWidget.setManaged(!isVisible);
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
            
            Object controller = loader.getController();
            if (controller instanceof LecturerScheduleController) {
                ((LecturerScheduleController) controller).setLecturerName(lblUserName.getText());
            } else if (controller instanceof LecturerAttendanceListController) {
                String uCode = lblUserCode.getText().replace("Ma GV: ", "").trim();
                ((LecturerAttendanceListController) controller).setLecturerId(uCode);
            } else if (controller instanceof OpenSessionController) {
                ((OpenSessionController) controller).setLecturerName(lblUserName.getText());
            } else if (controller instanceof LecturerStatisticsController) {
                ((LecturerStatisticsController) controller).setLecturerName(lblUserName.getText());
            } else if (controller instanceof LecturerExportController) {
                ((LecturerExportController) controller).setLecturerName(lblUserName.getText());
            }
            
            contentArea.getChildren().add(node);
            currentSubPane = node;
        } catch (Exception e) {
            System.err.println("Lỗi load pane: " + e.getMessage());
            e.printStackTrace();
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
        Button[] all = {btnDashboard, btnSchedule, btnOpenSession,
                        btnAttendanceList, btnStatistics, btnExport,
                        btnChat, btnNotification};
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
            System.err.println("Lỗi navigate: " + e.getMessage());
        }
    }
}
