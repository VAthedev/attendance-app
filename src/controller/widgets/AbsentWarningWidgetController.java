package controller.widgets;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AbsentWarningWidgetController {

    @FXML private VBox boxAbsentWarning;
    @FXML private Label lblNoWarning;

    public void loadData(String lecturerId) {
        if (lecturerId == null || lecturerId.isEmpty()) return;

        new Thread(() -> {
            try {
                // Pending implementation from original
                // For now, it just shows no warning.
                Platform.runLater(() -> {
                    boxAbsentWarning.getChildren().clear();
                    lblNoWarning.setVisible(true); lblNoWarning.setManaged(true);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addAbsentWarningRow(String name, String mssv, String subject,
                                      int absent, int total) {
        HBox row = new HBox(12);
        row.setStyle("-fx-padding:10 4 10 4;-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:1;");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label n = new Label(name); n.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1a2744;");
        Label d = new Label(mssv + "  •  " + subject); d.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7a99;");
        info.getChildren().addAll(n, d);
        Label lblAbsent = new Label(absent + "/" + total + " buổi vắng");
        lblAbsent.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;" +
                           "-fx-background-radius:4;-fx-padding:3 8 3 8;-fx-font-size:12px;");
        Button btnEmail = new Button("📧 Gửi cảnh báo");
        btnEmail.setStyle("-fx-background-color:#fff7ed;-fx-text-fill:#c2410c;" +
                          "-fx-background-radius:6;-fx-font-size:11px;-fx-cursor:hand;");
        btnEmail.setOnAction(e -> { btnEmail.setText("✓ Đã gửi"); btnEmail.setDisable(true); });
        row.getChildren().addAll(info, lblAbsent, btnEmail);
        boxAbsentWarning.getChildren().add(row);
        lblNoWarning.setVisible(false); lblNoWarning.setManaged(false);
    }
    
    @FXML
    private void handleShowAll() {
        // Có thể emit event hoặc gọi callback
    }
}
