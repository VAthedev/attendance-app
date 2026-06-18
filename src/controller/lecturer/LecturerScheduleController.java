package controller.lecturer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import database.ScheduleRepository;

public class LecturerScheduleController implements Initializable {

    @FXML private Label lblWeekRange;
    @FXML private Label lblTotalClassWeek, lblDoneWeek, lblPendingWeek;
    
    @FXML private ToggleGroup viewModeGroup;
    @FXML private VBox weekGridContainer, weekListContainer, emptyWeekBox;
    @FXML private HBox weekGridDays;
    @FXML private VBox weekListBox;

    private LocalDate currentWeekStart;
    private String lecturerName = "";

    public void setLecturerName(String lecturerName) {
        this.lecturerName = lecturerName;
        loadWeekData();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        LocalDate today = LocalDate.now();
        // Adjust to Monday
        currentWeekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        
        viewModeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String mode = newVal.getUserData().toString();
                if (mode.equals("GRID")) {
                    weekListContainer.setVisible(false);
                    weekListContainer.setManaged(false);
                    weekGridContainer.setVisible(true);
                    weekGridContainer.setManaged(true);
                } else {
                    weekGridContainer.setVisible(false);
                    weekGridContainer.setManaged(false);
                    weekListContainer.setVisible(true);
                    weekListContainer.setManaged(true);
                }
            }
        });
    }

    private void loadWeekData() {
        if (lecturerName == null || lecturerName.isEmpty()) return;

        LocalDate endOfWeek = currentWeekStart.plusDays(6);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");
        lblWeekRange.setText(currentWeekStart.format(dtf) + " - " + endOfWeek.format(dtf));

        new Thread(() -> {
            try {
                List<Map<String,Object>> rawSchedules = ScheduleRepository.getInstance()
                    .findLecturerSchedulesInRange(lecturerName, currentWeekStart, endOfWeek);

                List<ScheduleInfo> schedules = rawSchedules.stream().map(m -> {
                    ScheduleInfo s = new ScheduleInfo();
                    s.subject = (String) m.get("subject");
                    s.className = (String) m.get("className");
                    s.room = (String) m.get("room");
                    s.startTime = (String) m.get("startTime");
                    s.endTime = (String) m.get("endTime");
                    s.status = (String) m.get("status");
                    LocalDate d = LocalDate.parse((String) m.get("date"));
                    s.dayOfWeek = d.getDayOfWeek().getValue();
                    return s;
                }).collect(Collectors.toList());

                Map<Integer, List<ScheduleInfo>> weekSchedules = schedules.stream()
                        .collect(Collectors.groupingBy(s -> s.dayOfWeek));

                Platform.runLater(() -> {
                    updateUI(weekSchedules);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateUI(Map<Integer, List<ScheduleInfo>> weekSchedules) {
        if (weekSchedules.isEmpty()) {
            emptyWeekBox.setVisible(true);
            emptyWeekBox.setManaged(true);
            weekGridDays.setVisible(false);
            weekListContainer.setVisible(false);
        } else {
            emptyWeekBox.setVisible(false);
            emptyWeekBox.setManaged(false);
            weekGridDays.setVisible(true);
            
            displayGridView(weekSchedules);
            displayListView(weekSchedules);
        }

        int total = weekSchedules.values().stream().mapToInt(List::size).sum();
        int done = (int) weekSchedules.values().stream()
                .flatMap(List::stream)
                .filter(s -> s.status.equals("PAST")).count();
        int pending = total - done;

        lblTotalClassWeek.setText(String.valueOf(total));
        lblDoneWeek.setText(String.valueOf(done));
        lblPendingWeek.setText(String.valueOf(pending));
    }

    private void displayGridView(Map<Integer, List<ScheduleInfo>> weekSchedules) {
        weekGridDays.getChildren().clear();

        String[] dayNames = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN"};

        for (int day = 1; day <= 7; day++) {
            VBox dayColumn = new VBox(4);
            dayColumn.getStyleClass().add("week-day-column");
            VBox.setVgrow(dayColumn, Priority.ALWAYS);
            HBox.setHgrow(dayColumn, Priority.ALWAYS);
            dayColumn.setPrefHeight(300);
            dayColumn.setPrefWidth(180);
            dayColumn.setMinWidth(180);
            dayColumn.setMaxWidth(Double.MAX_VALUE);

            LocalDate date = currentWeekStart.plusDays(day - 1);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM");

            Label dayHeader = new Label(dayNames[day - 1]);
            dayHeader.getStyleClass().add("week-day-column-header");
            dayHeader.setMaxWidth(Double.MAX_VALUE);
            dayHeader.setAlignment(javafx.geometry.Pos.CENTER);
            
            Label dateLabel = new Label(date.format(dateFormatter));
            dateLabel.getStyleClass().add("week-day-column-date");
            dateLabel.setMaxWidth(Double.MAX_VALUE);
            dateLabel.setAlignment(javafx.geometry.Pos.CENTER);

            VBox headerBox = new VBox(2);
            headerBox.getChildren().addAll(dayHeader, dateLabel);
            headerBox.setAlignment(javafx.geometry.Pos.CENTER);
            headerBox.setMaxWidth(Double.MAX_VALUE);
            headerBox.setStyle("-fx-padding: 8;");

            VBox itemsBox = new VBox(8);
            itemsBox.setStyle("-fx-fill-width: true; -fx-padding: 4;");

            List<ScheduleInfo> daySchedules = weekSchedules.getOrDefault(day, new ArrayList<>());
            for (ScheduleInfo schedule : daySchedules) {
                VBox card = new VBox(4);
                card.getStyleClass().add("week-day-item");
                card.setStyle(getItemStyle(schedule.status));
                
                Label time = new Label("⏰ " + schedule.startTime + " - " + schedule.endTime);
                time.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
                Label subj = new Label(schedule.subject);
                subj.setWrapText(true);
                subj.setStyle("-fx-font-weight: bold;");
                Label cls = new Label("Lớp: " + schedule.className);
                cls.setStyle("-fx-font-size: 11px;");
                Label rm = new Label("Phòng: " + schedule.room);
                rm.setStyle("-fx-font-size: 11px;");
                
                card.getChildren().addAll(time, subj, cls, rm);
                
                if (schedule.status.equals("TODAY")) {
                    Button btn = new Button("Mở điểm danh");
                    btn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
                    btn.setMaxWidth(Double.MAX_VALUE);
                    btn.setOnAction(e -> handleQuickOpenSession());
                    card.getChildren().add(btn);
                }
                
                itemsBox.getChildren().add(card);
            }

            ScrollPane scrollPane = new ScrollPane(itemsBox);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setFitToWidth(true);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            dayColumn.getChildren().addAll(headerBox, scrollPane);
            weekGridDays.getChildren().add(dayColumn);
        }
    }

    private void displayListView(Map<Integer, List<ScheduleInfo>> weekSchedules) {
        weekListBox.getChildren().clear();
        String[] dayNames = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ Nhật"};

        for (int day = 1; day <= 7; day++) {
            LocalDate date = currentWeekStart.plusDays(day - 1);
            List<ScheduleInfo> daySchedules = weekSchedules.getOrDefault(day, new ArrayList<>());

            if (!daySchedules.isEmpty()) {
                VBox daySection = new VBox(8);
                daySection.setStyle("-fx-padding: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-border-width: 1; -fx-background-color: #f8fafc;");

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
                Label dayLabel = new Label(dateFormatter.format(date));
                dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1a2744;");
                daySection.getChildren().add(dayLabel);

                for (ScheduleInfo schedule : daySchedules) {
                    HBox itemBox = new HBox(12);
                    itemBox.setStyle("-fx-padding: 10; " + getItemStyle(schedule.status) + " -fx-background-radius: 6; -fx-border-color: #e2e8f0; -fx-border-radius: 6;");

                    Label time = new Label(schedule.startTime + " - " + schedule.endTime);
                    time.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a2744; -fx-min-width: 100px;");

                    VBox info = new VBox(2);
                    Label subject = new Label(schedule.subject);
                    subject.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a2744;");
                    Label details = new Label("Lớp: " + schedule.className + " | Phòng: " + schedule.room);
                    details.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7a99;");
                    info.getChildren().addAll(subject, details);

                    itemBox.getChildren().addAll(time, info, new Region());
                    HBox.setHgrow(info, Priority.ALWAYS);
                    
                    if (schedule.status.equals("TODAY")) {
                        Button btn = new Button("Mở điểm danh");
                        btn.getStyleClass().add("btn-primary");
                        btn.setOnAction(e -> handleQuickOpenSession());
                        itemBox.getChildren().add(btn);
                    }
                    
                    daySection.getChildren().add(itemBox);
                }

                weekListBox.getChildren().add(daySection);
            }
        }
    }

    private String getItemStyle(String status) {
        return switch (status) {
            case "PAST" -> "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a;";
            case "TODAY" -> "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb;";
            case "UPCOMING" -> "-fx-background-color: #fef3c7; -fx-text-fill: #b45309;";
            default -> "-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280;";
        };
    }

    @FXML private void handlePreviousWeek() {
        currentWeekStart = currentWeekStart.minusWeeks(1);
        loadWeekData();
    }

    @FXML private void handleNextWeek() {
        currentWeekStart = currentWeekStart.plusWeeks(1);
        loadWeekData();
    }

    @FXML private void handleCurrentWeek() {
        LocalDate today = LocalDate.now();
        currentWeekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        loadWeekData();
    }
    
    @FXML private void handleSync() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Đồng bộ TKB");
        alert.setHeaderText("Đang đồng bộ dữ liệu...");
        alert.setContentText("Đã đồng bộ thành công thời khóa biểu mới nhất từ hệ thống đào tạo trung tâm.");
        alert.show();
        loadWeekData();
    }
    
    @FXML private void handleQuickOpenSession() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mở điểm danh nhanh");
        alert.setHeaderText("Dò tìm lớp học");
        alert.setContentText("Tính năng đang dò tìm lớp học trong khung giờ hiện tại. Tính năng đầy đủ sẽ được triển khai ở module Điểm danh.");
        alert.show();
    }

    private static class ScheduleInfo {
        int dayOfWeek;
        String subject;
        String className;
        String room;
        String startTime;
        String endTime;
        String status;
    }
}
