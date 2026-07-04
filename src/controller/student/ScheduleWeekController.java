package controller.student;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ScheduleWeekController implements Initializable {

    @FXML private Label lblWeekRange;
    @FXML private Label lblTotalClassWeek, lblAttendedWeek, lblPendingWeek, lblMissedWeek;
    @FXML private HBox weekGridDays;
    @FXML private VBox weekListBox, weekGridContainer, weekListContainer, emptyWeekBox;
    @FXML private ToggleGroup viewModeGroup;

    private LocalDate currentWeekStart;
    private ToggleGroup filterGroup;
    private Map<Integer, List<ScheduleInfo>> currentWeekSchedules = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentWeekStart = LocalDate.now().with(ChronoField.DAY_OF_WEEK, 1);
        filterGroup = new ToggleGroup();
        
        if (viewModeGroup != null) {
            viewModeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    viewModeGroup.selectToggle(oldVal);
                    return;
                }
                renderSchedules();
            });
        }
        
        updateWeekDisplay();
    }

    @FXML
    public void handlePreviousWeek() {
        currentWeekStart = currentWeekStart.minusWeeks(1);
        updateWeekDisplay();
    }

    @FXML
    public void handleNextWeek() {
        currentWeekStart = currentWeekStart.plusWeeks(1);
        updateWeekDisplay();
    }

    @FXML
    public void handleCurrentWeek() {
        currentWeekStart = LocalDate.now().with(ChronoField.DAY_OF_WEEK, 1);
        updateWeekDisplay();
    }

    private void updateWeekDisplay() {
        LocalDate weekEnd = currentWeekStart.plusDays(6);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        
        int weekNum = currentWeekStart.get(java.time.temporal.WeekFields.ISO.weekOfYear());
        lblWeekRange.setText(String.format("Tuần %d (%s - %s)", 
            weekNum, 
            currentWeekStart.format(formatter),
            weekEnd.format(formatter)));

        String sid = StudentDashboardController.currentStudentId;
        protocol.Request req = new protocol.Request(protocol.RequestType.GET_SCHEDULE_BY_WEEK);
        req.putPayload("studentId", sid);
        req.putPayload("startDate", currentWeekStart.toString());
        req.putPayload("endDate", weekEnd.toString());

        client.network.SocketClient.getInstance().sendAsync(req, res -> {
            if (!res.isOk()) {
                javafx.application.Platform.runLater(() -> System.err.println("Lỗi: " + res.getMessage()));
                return;
            }
            try {
                Map<Integer, List<ScheduleInfo>> weekSchedules = new HashMap<>();
                String schedulesStr = res.getDataValue("schedules");
                org.json.JSONArray arr = new org.json.JSONArray(schedulesStr);
                
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject r = arr.getJSONObject(i);
                    String dateStr = r.optString("date", "");
                    java.time.LocalDate date = null;
                    try { date = java.time.LocalDate.parse(dateStr); } catch (Exception ex) { continue; }
                    
                    int day = date.get(java.time.temporal.ChronoField.DAY_OF_WEEK);
                    if (day == 7) day = 7; 
                    int idx = (day == 7) ? 7 : day; 

                    String subj = r.optString("subject", "");
                    String status = r.optString("status", "PENDING");
                    if ("ABSENT".equals(status)) status = "MISSED";

                    ScheduleInfo si = new ScheduleInfo(
                            subj,
                            r.optString("startTime", ""),
                            r.optString("endTime", ""),
                            r.optString("lecturer", ""),
                            r.optString("room", ""),
                            r.optString("className", ""),
                            status
                    );
                    weekSchedules.computeIfAbsent(idx, k -> new ArrayList<>()).add(si);
                }
                
                javafx.application.Platform.runLater(() -> {
                    currentWeekSchedules = weekSchedules;
                    renderSchedules();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void renderSchedules() {
        if (currentWeekSchedules.isEmpty() || currentWeekSchedules.values().stream().allMatch(List::isEmpty)) {
            weekGridContainer.setVisible(true);
            weekListContainer.setVisible(false);
            emptyWeekBox.setVisible(true);
            updateStats(0, 0, 0, 0);
            return;
        }

        emptyWeekBox.setVisible(false);
        
        ToggleButton selectedMode = (ToggleButton) viewModeGroup.getSelectedToggle();
        String viewMode = selectedMode != null ? (String) selectedMode.getUserData() : "GRID";
        
        if ("GRID".equals(viewMode)) {
            displayGridView(currentWeekSchedules);
            weekGridContainer.setVisible(true);
            weekListContainer.setVisible(false);
        } else {
            displayListView(currentWeekSchedules);
            weekGridContainer.setVisible(false);
            weekListContainer.setVisible(true);
        }

        int total = currentWeekSchedules.values().stream().mapToInt(List::size).sum();
        int attended = (int) currentWeekSchedules.values().stream()
                .flatMap(List::stream)
                .filter(s -> s.status.equals("ATTENDED")).count();
        int pending = (int) currentWeekSchedules.values().stream()
                .flatMap(List::stream)
                .filter(s -> s.status.equals("PENDING")).count();
        int missed = total - attended - pending;

        updateStats(total, attended, pending, missed);
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

            VBox itemsBox = new VBox(4);
            itemsBox.setStyle("-fx-fill-width: true; -fx-padding: 4;");

            List<ScheduleInfo> daySchedules = weekSchedules.getOrDefault(day, new ArrayList<>());
            for (ScheduleInfo schedule : daySchedules) {
                Label item = new Label(schedule.startTime + " " + schedule.subject);
                item.getStyleClass().add("week-day-item");
                item.setStyle(getItemStyle(schedule.status));
                item.setWrapText(true);
                item.setMaxWidth(Double.MAX_VALUE);
                itemsBox.getChildren().add(item);
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
                // Day header
                VBox daySection = new VBox(8);
                daySection.setStyle("-fx-padding: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-border-width: 1; -fx-background-color: #f8fafc;");

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
                Label dayLabel = new Label(dateFormatter.format(date));
                dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1a2744;");
                daySection.getChildren().add(dayLabel);

                // Schedules for this day
                for (ScheduleInfo schedule : daySchedules) {
                    HBox itemBox = new HBox(12);
                    itemBox.setStyle("-fx-padding: 10; -fx-background-color: " + getStatusColor(schedule.status) + "; -fx-background-radius: 6; -fx-border-color: #e2e8f0; -fx-border-radius: 6;");

                    Label time = new Label(schedule.startTime + " - " + schedule.endTime);
                    time.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a2744; -fx-min-width: 100px;");

                    VBox info = new VBox(2);
                    Label subject = new Label(schedule.subject);
                    subject.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a2744;");
                    Label lecturer = new Label("👤  " + schedule.lecturer);
                    lecturer.setStyle("-fx-font-size:10px; -fx-text-fill:#6b7a99;");
                    info.getChildren().addAll(subject, lecturer);

                    Label status = new Label(schedule.status.equals("ATTENDED") ? "✅ Đã điểm danh" : 
                                            schedule.status.equals("PENDING") ? "⭕ Chưa điểm danh" : "⏰ Sắp tới");
                    status.setStyle("-fx-font-size: 11px; -fx-text-fill: #2563eb;");

                    itemBox.getChildren().addAll(time, info, new Region(), status);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    daySection.getChildren().add(itemBox);
                }

                weekListBox.getChildren().add(daySection);
            }
        }
    }

    private String getItemStyle(String status) {
        return switch (status) {
            case "ATTENDED" -> "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a;";
            case "PENDING" -> "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;";
            case "UPCOMING" -> "-fx-background-color: #fef3c7; -fx-text-fill: #b45309;";
            case "PAST", "MISSED", "ABSENT", "LATE" -> "-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280;";
            default -> "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb;";
        };
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "ATTENDED" -> "#dcfce7";
            case "PENDING" -> "#fee2e2";
            case "UPCOMING" -> "#fef3c7";
            case "PAST", "MISSED", "ABSENT", "LATE" -> "#f3f4f6";
            default -> "#eff6ff";
        };
    }

    private void updateStats(int total, int attended, int pending, int missed) {
        lblTotalClassWeek.setText(String.valueOf(total));
        lblAttendedWeek.setText(String.valueOf(attended));
        lblPendingWeek.setText(String.valueOf(pending));
        lblMissedWeek.setText(String.valueOf(missed));
    }

    private Map<Integer, List<ScheduleInfo>> getSchedulesForWeek(LocalDate startDate, LocalDate endDate) {
        Map<Integer, List<ScheduleInfo>> schedules = new HashMap<>();

        // Mock data - replace with database calls
        LocalDate today = LocalDate.now();
        
        if (startDate.isBefore(today.plusDays(7)) && endDate.isAfter(today.minusDays(7))) {
            // Thứ 2
            List<ScheduleInfo> mon = new ArrayList<>();
            mon.add(new ScheduleInfo("Lập trình mạng", "07:30", "09:10", "TS. Nguyễn Văn A", "P.201", "CTK43A", "ATTENDED"));
            mon.add(new ScheduleInfo("Cơ sở dữ liệu", "09:30", "11:10", "TS. Trần Thị B", "P.305", "CTK43A", "PENDING"));
            schedules.put(1, mon);

            // Thứ 3
            List<ScheduleInfo> tue = new ArrayList<>();
            tue.add(new ScheduleInfo("Hệ điều hành", "07:30", "09:10", "TS. Lê Minh D", "P.202", "CTK43A", "PENDING"));
            schedules.put(2, tue);

            // Thứ 5
            List<ScheduleInfo> thu = new ArrayList<>();
            thu.add(new ScheduleInfo("Giải thuật", "13:00", "14:40", "ThS. Phạm Văn C", "P.401", "CTK43A", "ATTENDED"));
            thu.add(new ScheduleInfo("Web Development", "15:00", "16:40", "ThS. Hoàng Thị E", "P.306", "CTK43A", "UPCOMING"));
            schedules.put(4, thu);
        }

        return schedules;
    }

    private static class ScheduleInfo {
        String subject, startTime, endTime, lecturer, room, className, status;

        ScheduleInfo(String subject, String startTime, String endTime,
                    String lecturer, String room, String className, String status) {
            this.subject = subject;
            this.startTime = startTime;
            this.endTime = endTime;
            this.lecturer = lecturer;
            this.room = room;
            this.className = className;
            this.status = status;
        }
    }
}
