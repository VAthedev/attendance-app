package controller.student;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ScheduleDayController implements Initializable {

    @FXML private DatePicker dpSelectedDate;
    @FXML private Label lblTotalClassToday, lblAttendedToday, lblPendingToday, lblPendingAttendance;
    @FXML private VBox scheduleTimelineBox, emptyScheduleBox;

    private LocalDate currentSelectedDate;
    private ToggleGroup filterGroup;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentSelectedDate = LocalDate.now();
        dpSelectedDate.setValue(currentSelectedDate);
        filterGroup = new ToggleGroup();
        loadScheduleData();
    }

    @FXML
    public void handleDateChanged() {
        currentSelectedDate = dpSelectedDate.getValue();
        loadScheduleData();
    }

    @FXML
    public void handleTodayClick() {
        currentSelectedDate = LocalDate.now();
        dpSelectedDate.setValue(currentSelectedDate);
        loadScheduleData();
    }

    @FXML
    public void handleTomorrowClick() {
        currentSelectedDate = LocalDate.now().plusDays(1);
        dpSelectedDate.setValue(currentSelectedDate);
        loadScheduleData();
    }

    private void loadScheduleData() {
        scheduleTimelineBox.getChildren().clear();
        
        // Lấy dữ liệu lịch học cho ngày được chọn
        List<ScheduleInfo> schedules = getSchedulesForDate(currentSelectedDate);

        if (schedules.isEmpty()) {
            emptyScheduleBox.setVisible(true);
            emptyScheduleBox.setManaged(true);
            updateStats(0, 0, 0, 0);
            return;
        }

        emptyScheduleBox.setVisible(false);
        emptyScheduleBox.setManaged(false);

        // Tính toán thống kê
        int total = schedules.size();
        int attended = (int) schedules.stream()
                .filter(s -> s.status.equals("ATTENDED")).count();
        int pending = (int) schedules.stream()
                .filter(s -> s.status.equals("PENDING")).count();
        int notAttended = total - attended;

        updateStats(total, attended, pending, notAttended);

        // Vẽ lịch trình
        for (ScheduleInfo schedule : schedules) {
            VBox scheduleItem = createScheduleItem(schedule);
            scheduleTimelineBox.getChildren().add(scheduleItem);
        }
    }

    private VBox createScheduleItem(ScheduleInfo schedule) {
        VBox mainBox = new VBox();
        mainBox.getStyleClass().add("schedule-day-item");
        mainBox.setStyle("-fx-border-width: 1; -fx-border-color: #e2e8f0;");

        // Status bar header with color
        HBox statusBar = new HBox();
        statusBar.setPrefHeight(4);
        statusBar.setStyle(getStatusBarColor(schedule.status));

        // Main content
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        // Header: Time + Subject + Status
        HBox header = new HBox(12);
        header.setStyle("-fx-alignment: CENTER_LEFT;");

        // Time (left side)
        Label timeLabel = new Label(schedule.startTime + " - " + schedule.endTime);
        timeLabel.getStyleClass().add("schedule-time-large");
        timeLabel.setStyle("-fx-min-width: 100px;");

        // Subject info (middle)
        VBox subjectInfo = new VBox(4);
        subjectInfo.setStyle("-fx-hgrow: ALWAYS;");
        Label subject = new Label(schedule.subject);
        subject.getStyleClass().add("schedule-subject-large");
        Label lecturer = new Label("👨‍🏫  " + schedule.lecturer);
        lecturer.getStyleClass().add("schedule-lecturer");
        subjectInfo.getChildren().addAll(subject, lecturer);

        // Status badge (right side)
        Label statusBadge = createStatusBadge(schedule.status);

        header.getChildren().addAll(timeLabel, subjectInfo, statusBadge);
        HBox.setHgrow(subjectInfo, Priority.ALWAYS);

        // Detail row: Room, Class
        HBox detailRow = new HBox(24);
        detailRow.setStyle("-fx-padding: 0 0 0 100px;");

        Label room = new Label("🏫  Phòng: " + schedule.room);
        room.getStyleClass().add("schedule-location");
        Label classInfo = new Label("👥  Lớp: " + schedule.className);
        classInfo.getStyleClass().add("schedule-location");

        detailRow.getChildren().addAll(room, classInfo);

        // Footer: Action buttons
        HBox footer = new HBox(12);
        footer.setStyle("-fx-padding: 12 0 0 100px; -fx-alignment: CENTER_LEFT;");

        if (schedule.status.equals("PENDING")) {
            Button checkInBtn = new Button("✓  Điểm danh ngay");
            checkInBtn.getStyleClass().add("btn-attendance");
            checkInBtn.setOnAction(e -> handleCheckIn(schedule));

            Button checkDetailsBtn = new Button("Chi tiết");
            checkDetailsBtn.getStyleClass().add("btn-secondary");

            footer.getChildren().addAll(checkInBtn, checkDetailsBtn);
        } else if (schedule.status.equals("UPCOMING")) {
            Label upcomingLabel = new Label("⏳ Sắp bắt đầu trong " + schedule.minutesUntilStart + " phút");
            upcomingLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 12px;");
            footer.getChildren().add(upcomingLabel);
        } else if (schedule.status.equals("ATTENDED")) {
            Label attendedLabel = new Label("✓ Đã điểm danh lúc " + schedule.attendanceTime);
            attendedLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12px;");
            footer.getChildren().add(attendedLabel);
        }

        content.getChildren().addAll(header, detailRow);
        if (!footer.getChildren().isEmpty()) {
            content.getChildren().add(footer);
        }

        mainBox.getChildren().addAll(statusBar, content);
        return mainBox;
    }

    private Label createStatusBadge(String status) {
        Label badge = new Label();
        switch (status) {
            case "ATTENDED":
                badge.setText("✅ Đã điểm danh");
                badge.getStyleClass().add("status-attended");
                break;
            case "PENDING":
                badge.setText("⭕ Chưa điểm danh");
                badge.getStyleClass().add("status-pending");
                break;
            case "UPCOMING":
                badge.setText("⏰ Sắp tới");
                badge.getStyleClass().add("status-upcoming");
                break;
            case "PAST":
                badge.setText("❌ Đã kết thúc");
                badge.getStyleClass().add("status-past");
                break;
        }
        return badge;
    }

    private String getStatusBarColor(String status) {
        return switch (status) {
            case "ATTENDED" -> "-fx-background-color: #22c55e;";
            case "PENDING" -> "-fx-background-color: #ef4444;";
            case "UPCOMING" -> "-fx-background-color: #f59e0b;";
            case "PAST" -> "-fx-background-color: #d1d5db;";
            default -> "-fx-background-color: #e2e8f0;";
        };
    }

    private void updateStats(int total, int attended, int pending, int notAttended) {
        lblTotalClassToday.setText(String.valueOf(total));
        lblAttendedToday.setText(String.valueOf(attended));
        lblPendingToday.setText(String.valueOf(pending));
        lblPendingAttendance.setText(String.valueOf(notAttended));
    }

    private void handleCheckIn(ScheduleInfo schedule) {
        // Show confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận điểm danh");
        confirm.setHeaderText("Điểm danh cho lớp: " + schedule.subject);
        confirm.setContentText("Bạn có chắc chắn muốn điểm danh cho buổi học lúc " + 
                              schedule.startTime + " không?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // TODO: Call attendance service to check in
            schedule.status = "ATTENDED";
            schedule.attendanceTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            
            // Show success message
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Thành công");
            success.setHeaderText("Điểm danh thành công!");
            success.setContentText("Điểm danh cho lớp " + schedule.subject + " lúc " + schedule.attendanceTime);
            success.showAndWait();
            
            // Reload data
            loadScheduleData();
        }
    }

    private List<ScheduleInfo> getSchedulesForDate(LocalDate date) {
        List<ScheduleInfo> schedules = new ArrayList<>();

        // Mock data - trong thực tế sẽ lấy từ database
        if (date.equals(LocalDate.now())) {
            schedules.add(new ScheduleInfo(
                    "Lập trình mạng",
                    "07:30",
                    "09:10",
                    "TS. Nguyễn Văn A",
                    "P.201",
                    "CTK43A",
                    "UPCOMING",
                    30
            ));
            schedules.add(new ScheduleInfo(
                    "Cơ sở dữ liệu",
                    "09:30",
                    "11:10",
                    "TS. Trần Thị B",
                    "P.305",
                    "CTK43A",
                    "PENDING",
                    0
            ));
            schedules.add(new ScheduleInfo(
                    "Giải thuật",
                    "13:00",
                    "14:40",
                    "ThS. Phạm Văn C",
                    "P.401",
                    "CTK43A",
                    "ATTENDED",
                    "13:02"
            ));
        } else if (date.equals(LocalDate.now().plusDays(1))) {
            schedules.add(new ScheduleInfo(
                    "Hệ điều hành",
                    "07:30",
                    "09:10",
                    "TS. Lê Minh D",
                    "P.202",
                    "CTK43A",
                    "PENDING",
                    0
            ));
            schedules.add(new ScheduleInfo(
                    "Web Development",
                    "09:30",
                    "11:10",
                    "ThS. Hoàng Thị E",
                    "P.306",
                    "CTK43A",
                    "PENDING",
                    0
            ));
        }

        return schedules;
    }

    // Inner class to hold schedule information
    private static class ScheduleInfo {
        String subject;
        String startTime;
        String endTime;
        String lecturer;
        String room;
        String className;
        String status;  // ATTENDED, PENDING, UPCOMING, PAST
        int minutesUntilStart;
        String attendanceTime;

        ScheduleInfo(String subject, String startTime, String endTime,
                    String lecturer, String room, String className,
                    String status, int minutesUntilStart) {
            this.subject = subject;
            this.startTime = startTime;
            this.endTime = endTime;
            this.lecturer = lecturer;
            this.room = room;
            this.className = className;
            this.status = status;
            this.minutesUntilStart = minutesUntilStart;
        }

        ScheduleInfo(String subject, String startTime, String endTime,
                    String lecturer, String room, String className,
                    String status, String attendanceTime) {
            this.subject = subject;
            this.startTime = startTime;
            this.endTime = endTime;
            this.lecturer = lecturer;
            this.room = room;
            this.className = className;
            this.status = status;
            this.attendanceTime = attendanceTime;
        }
    }
}
