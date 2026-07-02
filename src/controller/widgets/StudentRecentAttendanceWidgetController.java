package controller.widgets;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class StudentRecentAttendanceWidgetController {

    @FXML private VBox boxRecentAttendance;
    @FXML private Label lblNoAttendance;

    public void loadData(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;
        
        // Hiện tại tính năng điểm danh gần đây đang "pending" trong bản gốc
        // Để trống như code gốc, hoặc thêm giả lập
        Platform.runLater(() -> {
            boxRecentAttendance.getChildren().clear();
            lblNoAttendance.setVisible(true);
            lblNoAttendance.setManaged(true);
        });
    }

    private void addAttendanceItem(String subject, String date, String status) {
        HBox item = new HBox(12);
        item.setStyle("-fx-padding:8 0 8 0;");
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label s = new Label(subject); s.getStyleClass().add("schedule-subject");
        Label d = new Label(date);    d.getStyleClass().add("schedule-time");
        info.getChildren().addAll(s, d);
        String cls  = switch(status) { case "PRESENT"->"badge-present"; case "ABSENT"->"badge-absent"; default->"badge-late"; };
        String text = switch(status) { case "PRESENT"->"Có mặt"; case "ABSENT"->"Vắng"; default->"Trễ"; };
        Label badge = new Label(text); badge.getStyleClass().add(cls);
        item.getChildren().addAll(info, badge);
        boxRecentAttendance.getChildren().add(item);
    }
}
