package controller.student;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ScheduleSubjectController implements Initializable {

    @FXML private ComboBox<String> cbSubjects;
    @FXML private Label lblSubjectLecturer, lblSubjectCredits, lblSubjectSessions;
    @FXML private Label lblTotalSessions, lblAttendedSessions, lblAbsentSessions, lblAttendanceRate;
    @FXML private VBox subjectStatsBox, subjectScheduleBox, semesterProgressBox;
    @FXML private HBox subjectStatCardsBox;
    @FXML private VBox subjectSessionsList, emptySubjectBox;
    @FXML private ProgressBar attendanceProgress, remainingProgress;
    @FXML private Label lblAttendancePercent, lblRemainingPercent;

    private Map<String, SubjectInfo> subjectsData = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeSubjects();
        cbSubjects.setOnAction(e -> handleSubjectChanged());
    }

    private void initializeSubjects() {
        // Try load subjects from DB; fallback to mock
        // Populate from Aggregation Pipeline
        String sid = StudentDashboardController.currentStudentId;
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.List<java.util.Map<String,Object>> allSchedules = database.ScheduleRepository.getInstance()
                .findStudentSchedulesInRange(sid, today.minusWeeks(2), today.plusWeeks(10));
                
        for (java.util.Map<String,Object> s : allSchedules) {
            String name = (String) s.getOrDefault("subject", "Unnamed");
            if (!subjectsData.containsKey(name)) {
                String lecturer = (String) s.getOrDefault("lecturer", "Unknown");
                subjectsData.put(name, new SubjectInfo(name, lecturer, 3, 15, new ArrayList<>()));
            }
            
            // Format date for UI: "dd/MM/yyyy"
            String rawDate = (String) s.get("date");
            String displayDate = rawDate;
            try {
                java.time.LocalDate d = java.time.LocalDate.parse(rawDate);
                displayDate = d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch(Exception ignored) {}
            
            String time = s.get("startTime") + " - " + s.get("endTime");
            String room = (String) s.getOrDefault("room", "");
            String status = (String) s.getOrDefault("status", "PENDING");
            
            // Generate mock past/future status based on date for visual testing
            try {
                java.time.LocalDate d = java.time.LocalDate.parse(rawDate);
                if (d.isBefore(today)) status = "ATTENDED";
                else if (d.isAfter(today)) status = "PENDING";
            } catch(Exception ignored) {}

            subjectsData.get(name).sessions.add(new SessionInfo(displayDate, time, room, status));
        }
        
        // Populate combo box
        cbSubjects.getItems().addAll(subjectsData.keySet());
    }

    @FXML
    public void handleSubjectChanged() {
        String selectedSubject = cbSubjects.getValue();
        if (selectedSubject == null || selectedSubject.isEmpty()) {
            subjectStatsBox.setVisible(false);
            subjectStatCardsBox.setVisible(false);
            subjectScheduleBox.setVisible(true);
            emptySubjectBox.setVisible(true);
            return;
        }

        SubjectInfo subject = subjectsData.get(selectedSubject);
        
        // Update subject info
        lblSubjectLecturer.setText(subject.lecturer);
        lblSubjectCredits.setText(subject.credits + " tín chỉ");
        lblSubjectSessions.setText(subject.totalSessions + " tiết");
        subjectStatsBox.setVisible(true);

        // Calculate stats
        int attended = (int) subject.sessions.stream()
                .filter(s -> s.status.equals("ATTENDED")).count();
        int absent = (int) subject.sessions.stream()
                .filter(s -> s.status.equals("ABSENT")).count();
        int pending = (int) subject.sessions.stream()
                .filter(s -> s.status.equals("PENDING")).count();
        int total = subject.sessions.size();

        double attendanceRate = total > 0 ? (attended * 100.0 / total) : 0;

        lblTotalSessions.setText(String.valueOf(total));
        lblAttendedSessions.setText(String.valueOf(attended));
        lblAbsentSessions.setText(String.valueOf(absent));
        lblAttendanceRate.setText(String.format("%.1f%%", attendanceRate));

        // Update progress bars
        double attendedPercent = total > 0 ? (attended * 1.0 / total) : 0;
        double remainingPercent = total > attended ? ((total - attended) * 1.0 / total) : 0;
        
        attendanceProgress.setProgress(attendedPercent);
        remainingProgress.setProgress(remainingPercent);
        lblAttendancePercent.setText(String.format("%.0f%%", attendedPercent * 100));
        lblRemainingPercent.setText(String.format("%.0f%%", remainingPercent * 100));

        // Display sessions
        displaySessions(subject);

        subjectStatCardsBox.setVisible(true);
        semesterProgressBox.setVisible(true);
        emptySubjectBox.setVisible(false);
    }

    private void displaySessions(SubjectInfo subject) {
        subjectSessionsList.getChildren().clear();

        for (SessionInfo session : subject.sessions) {
            HBox sessionItem = new HBox(12);
            sessionItem.getStyleClass().add("subject-session-item");
            sessionItem.setPadding(new Insets(12));

            // Date
            Label dateLabel = new Label(session.date);
            dateLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb; -fx-min-width: 100px;");

            // Time
            Label timeLabel = new Label(session.time);
            timeLabel.setStyle("-fx-text-fill: #6b7a99; -fx-font-size: 12px;");

            // Room
            Label roomLabel = new Label("🏫  " + session.room);
            roomLabel.setStyle("-fx-text-fill: #6b7a99; -fx-font-size: 12px;");

            // Status badge
            Label statusLabel = new Label();
            switch (session.status) {
                case "ATTENDED":
                    statusLabel.setText("✅ Đã tham gia");
                    statusLabel.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-padding: 4 10; -fx-background-radius: 4; -fx-font-size: 11px;");
                    break;
                case "ABSENT":
                    statusLabel.setText("❌ Vắng mặt");
                    statusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-padding: 4 10; -fx-background-radius: 4; -fx-font-size: 11px;");
                    break;
                case "PENDING":
                    statusLabel.setText("⏳ Sắp tới");
                    statusLabel.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-padding: 4 10; -fx-background-radius: 4; -fx-font-size: 11px;");
                    break;
            }

            sessionItem.getChildren().addAll(dateLabel, timeLabel, roomLabel, new Region(), statusLabel);
            HBox.setHgrow(new Region(), Priority.ALWAYS);
            subjectSessionsList.getChildren().add(sessionItem);
        }
    }

    private static class SubjectInfo {
        String name, lecturer;
        int credits, totalSessions;
        List<SessionInfo> sessions;

        SubjectInfo(String name, String lecturer, int credits, int totalSessions, List<SessionInfo> sessions) {
            this.name = name;
            this.lecturer = lecturer;
            this.credits = credits;
            this.totalSessions = totalSessions;
            this.sessions = sessions;
        }
    }

    private static class SessionInfo {
        String date, time, room, status;

        SessionInfo(String date, String time, String room, String status) {
            this.date = date;
            this.time = time;
            this.room = room;
            this.status = status;
        }
    }
}
