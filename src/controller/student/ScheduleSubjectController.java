package controller.student;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ScheduleSubjectController implements Initializable {

    @FXML private ComboBox<String> cbSubjects;
    @FXML private Label lblSubjectLecturer, lblSubjectCredits, lblSubjectSessions;
    @FXML private Label lblTotalSessions, lblAttendedSessions, lblAbsentSessions, lblAttendanceRate;
    @FXML private VBox subjectStatsBox, subjectStatCardsBox, subjectScheduleBox, semesterProgressBox;
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
        // Mock data - replace with database
        subjectsData.put("Lập trình mạng", new SubjectInfo(
            "Lập trình mạng", "TS. Nguyễn Văn A", 3, 15,
            Arrays.asList(
                new SessionInfo("15/03/2026", "07:30 - 09:10", "P.201", "ATTENDED"),
                new SessionInfo("22/03/2026", "07:30 - 09:10", "P.201", "ATTENDED"),
                new SessionInfo("29/03/2026", "07:30 - 09:10", "P.201", "ATTENDED"),
                new SessionInfo("05/04/2026", "07:30 - 09:10", "P.201", "PENDING"),
                new SessionInfo("12/04/2026", "07:30 - 09:10", "P.201", "PENDING")
            )
        ));

        subjectsData.put("Cơ sở dữ liệu", new SubjectInfo(
            "Cơ sở dữ liệu", "TS. Trần Thị B", 3, 15,
            Arrays.asList(
                new SessionInfo("14/03/2026", "09:30 - 11:10", "P.305", "ATTENDED"),
                new SessionInfo("21/03/2026", "09:30 - 11:10", "P.305", "ABSENT"),
                new SessionInfo("28/03/2026", "09:30 - 11:10", "P.305", "ATTENDED")
            )
        ));

        subjectsData.put("Giải thuật", new SubjectInfo(
            "Giải thuật", "ThS. Phạm Văn C", 3, 15,
            Arrays.asList(
                new SessionInfo("16/03/2026", "13:00 - 14:40", "P.401", "ATTENDED"),
                new SessionInfo("23/03/2026", "13:00 - 14:40", "P.401", "ATTENDED"),
                new SessionInfo("30/03/2026", "13:00 - 14:40", "P.401", "PENDING")
            )
        ));

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
