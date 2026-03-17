$dirs = @(
  ".vscode","lib","lib/javafx-sdk-21","certs",
  "resources/fxml/auth","resources/fxml/student","resources/fxml/lecturer","resources/fxml/shared",
  "resources/css","resources/images","resources/db",
  "src/server","src/client/network",
  "src/controller/auth","src/controller/student","src/controller/lecturer","src/controller/shared",
  "src/model","src/service","src/database","src/security","src/protocol","src/util",
  "test/unit","test/integration","test/stress","docs"
)
foreach ($d in $dirs) { New-Item -ItemType Directory -Path $d -Force | Out-Null }

$files = @(
  "lib/sqlite-jdbc-3.x.jar","certs/server.keystore","certs/server.truststore",
  "resources/fxml/auth/Login.fxml","resources/fxml/auth/Register.fxml","resources/fxml/auth/ForgotPassword.fxml",
  "resources/fxml/student/StudentDashboard.fxml","resources/fxml/student/ScheduleDay.fxml",
  "resources/fxml/student/ScheduleWeek.fxml","resources/fxml/student/ScheduleSubject.fxml",
  "resources/fxml/student/AttendanceGPS.fxml","resources/fxml/student/AttendanceHistory.fxml",
  "resources/fxml/student/AttendanceStats.fxml",
  "resources/fxml/lecturer/LecturerDashboard.fxml","resources/fxml/lecturer/OpenSession.fxml",
  "resources/fxml/lecturer/SessionTimer.fxml","resources/fxml/lecturer/AttendanceList.fxml",
  "resources/fxml/lecturer/ExportData.fxml","resources/fxml/lecturer/Statistics.fxml",
  "resources/fxml/shared/Chat.fxml","resources/fxml/shared/Notification.fxml",
  "resources/css/main.css","resources/css/theme-dark.css","resources/db/seed_data.sql",
  "src/server/Server.java","src/server/ClientHandler.java",
  "src/server/SessionManager.java","src/server/BroadcastManager.java",
  "src/client/Main.java","src/client/network/SocketClient.java",
  "src/client/network/TLSSocketClient.java","src/client/network/RequestBuilder.java",
  "src/client/network/ResponseHandler.java",
  "src/controller/auth/LoginController.java","src/controller/auth/RegisterController.java",
  "src/controller/auth/ForgotPasswordController.java",
  "src/controller/student/StudentDashboardController.java","src/controller/student/ScheduleDayController.java",
  "src/controller/student/ScheduleWeekController.java","src/controller/student/ScheduleSubjectController.java",
  "src/controller/student/AttendanceGPSController.java","src/controller/student/AttendanceHistoryController.java",
  "src/controller/lecturer/LecturerDashboardController.java","src/controller/lecturer/OpenSessionController.java",
  "src/controller/lecturer/SessionTimerController.java","src/controller/lecturer/ExportDataController.java",
  "src/controller/lecturer/StatisticsController.java",
  "src/controller/shared/ChatController.java","src/controller/shared/NotificationController.java",
  "src/model/User.java","src/model/Subject.java","src/model/Schedule.java",
  "src/model/Session.java","src/model/Attendance.java","src/model/ChatMessage.java","src/model/Notification.java",
  "src/service/AuthService.java","src/service/ScheduleService.java","src/service/AttendanceService.java",
  "src/service/GPSVerifyService.java","src/service/WiFiVerifyService.java",
  "src/service/DeviceFingerprintService.java","src/service/EmailService.java",
  "src/service/ExportService.java","src/service/ChatService.java","src/service/StatisticsService.java",
  "src/database/DatabaseHelper.java","src/database/UserRepository.java",
  "src/database/ScheduleRepository.java","src/database/AttendanceRepository.java","src/database/ChatRepository.java",
  "src/security/SHA256Util.java","src/security/AESUtil.java",
  "src/security/NonceManager.java","src/security/TokenManager.java",
  "src/protocol/Request.java","src/protocol/Response.java","src/protocol/RequestType.java",
  "src/util/DateTimeUtil.java","src/util/LocationUtil.java","src/util/JsonUtil.java",
  "test/unit/AuthServiceTest.java","test/unit/AttendanceServiceTest.java","test/unit/GPSVerifyTest.java",
  "test/integration/ClientServerFlowTest.java","test/integration/AttendanceFlowTest.java",
  "test/stress/MultiClientStressTest.java",
  "docs/usecase-diagram.drawio","docs/er-diagram.drawio","attendance.db"
)
foreach ($f in $files) { New-Item -ItemType File -Path $f -Force | Out-Null }

# ===== launch.json =====
Set-Content -Path ".vscode/launch.json" -Encoding UTF8 -Value '{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Launch Client (JavaFX)",
      "request": "launch",
      "mainClass": "client.Main",
      "projectName": "attendance-app",
      "vmArgs": "--module-path lib/javafx-sdk-21/lib --add-modules javafx.controls,javafx.fxml"
    },
    {
      "type": "java",
      "name": "Launch Server",
      "request": "launch",
      "mainClass": "server.Server",
      "projectName": "attendance-app"
    }
  ]
}'

# ===== settings.json =====
Set-Content -Path ".vscode/settings.json" -Encoding UTF8 -Value '{
  "java.project.sourcePaths": ["src"],
  "java.project.outputPath": "bin",
  "java.project.referencedLibraries": [
    "lib/**/*.jar",
    "lib/javafx-sdk-21/lib/*.jar"
  ],
  "editor.tabSize": 4,
  "editor.formatOnSave": true,
  "files.encoding": "utf8"
}'

# ===== schema.sql =====
$schema = @"
-- =============================================
-- DATABASE SCHEMA - Attendance App
-- =============================================

CREATE TABLE IF NOT EXISTS users (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username        TEXT    UNIQUE NOT NULL,
    password_hash   TEXT    NOT NULL,
    role            TEXT    NOT NULL CHECK(role IN ('STUDENT','LECTURER')),
    full_name       TEXT,
    email           TEXT    UNIQUE,
    student_id      TEXT    UNIQUE,
    device_id       TEXT,
    session_token   TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subjects (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL,
    code            TEXT    UNIQUE NOT NULL,
    credits         INTEGER DEFAULT 3,
    lecturer_id     INTEGER REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS enrollments (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id      INTEGER REFERENCES users(id) ON DELETE CASCADE,
    subject_id      INTEGER REFERENCES subjects(id) ON DELETE CASCADE,
    UNIQUE(student_id, subject_id)
);

CREATE TABLE IF NOT EXISTS schedules (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_id      INTEGER REFERENCES subjects(id) ON DELETE CASCADE,
    day_of_week     INTEGER NOT NULL CHECK(day_of_week BETWEEN 2 AND 8),
    start_time      TEXT    NOT NULL,
    end_time        TEXT    NOT NULL,
    room            TEXT,
    semester        TEXT,
    wifi_bssid      TEXT,
    gps_lat         REAL,
    gps_lng         REAL,
    gps_radius      INTEGER DEFAULT 100
);

CREATE TABLE IF NOT EXISTS sessions (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    schedule_id     INTEGER REFERENCES schedules(id) ON DELETE CASCADE,
    open_time       DATETIME,
    close_time      DATETIME,
    duration_minutes INTEGER DEFAULT 15,
    status          TEXT    DEFAULT 'OPEN' CHECK(status IN ('OPEN','CLOSED')),
    created_by      INTEGER REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS attendance (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id      INTEGER REFERENCES sessions(id) ON DELETE CASCADE,
    student_id      INTEGER REFERENCES users(id) ON DELETE CASCADE,
    check_in_time   DATETIME,
    method          TEXT    CHECK(method IN ('GPS','WIFI')),
    status          TEXT    NOT NULL CHECK(status IN ('PRESENT','LATE','ABSENT')),
    device_id       TEXT,
    gps_lat         REAL,
    gps_lng         REAL,
    nonce           TEXT,
    UNIQUE(session_id, student_id),
    UNIQUE(session_id, device_id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_id      INTEGER REFERENCES subjects(id) ON DELETE CASCADE,
    sender_id       INTEGER REFERENCES users(id) ON DELETE CASCADE,
    content         TEXT    NOT NULL,
    sent_at         DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER REFERENCES users(id) ON DELETE CASCADE,
    title           TEXT    NOT NULL,
    message         TEXT    NOT NULL,
    type            TEXT    CHECK(type IN ('ABSENCE','SCHEDULE_CHANGE','SYSTEM')),
    is_read         INTEGER DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attendance_session  ON attendance(session_id);
CREATE INDEX IF NOT EXISTS idx_attendance_student  ON attendance(student_id);
CREATE INDEX IF NOT EXISTS idx_sessions_schedule   ON sessions(schedule_id);
CREATE INDEX IF NOT EXISTS idx_schedules_subject   ON schedules(subject_id);
CREATE INDEX IF NOT EXISTS idx_chat_subject        ON chat_messages(subject_id);
CREATE INDEX IF NOT EXISTS idx_notif_user          ON notifications(user_id);
"@
Set-Content -Path "resources/db/schema.sql" -Encoding UTF8 -Value $schema

# ===== README.md =====
$readme = @"
# Attendance App - He thong TKB va Diem danh

## Cong nghe su dung
| Tang     | Cong nghe                        |
|----------|----------------------------------|
| UI       | JavaFX 21 + FXML                 |
| Mang     | TCP Socket + TLS/SSL             |
| CSDL     | SQLite 3 (sqlite-jdbc)           |
| Bao mat  | SHA-256, AES-128, Nonce          |
| Email    | JavaMail (SMTP)                  |
| Xuat file| Apache POI (Excel), OpenCSV      |

## Chay ung dung
1. Giai nen JavaFX SDK 21 vao: lib/javafx-sdk-21/
2. Tai sqlite-jdbc.jar vao: lib/
3. Chay Server: Launch Server (F5)
4. Chay Client: Launch Client JavaFX (F5)

## Yeu cau
- Java 21+
- JavaFX SDK 21
- VSCode + Extension Pack for Java

## Thanh vien nhom
| Ten | MSSV | Phan cong |
|-----|------|-----------|
|     |      |           |
"@
Set-Content -Path "README.md" -Encoding UTF8 -Value $readme

Write-Host ""
Write-Host "=== HOAN THANH ===" -ForegroundColor Green
Write-Host "File .java : $((Get-ChildItem src -Recurse -Filter *.java).Count)" -ForegroundColor Cyan
Write-Host "File .fxml : $((Get-ChildItem resources/fxml -Recurse -Filter *.fxml).Count)" -ForegroundColor Cyan
Write-Host "Thu muc    : $((Get-ChildItem . -Recurse -Directory).Count)" -ForegroundColor Cyan
Write-Host "Tong file  : $((Get-ChildItem . -Recurse -File).Count)" -ForegroundColor Cyan
Write-Host ""
Write-Host "Nhan Ctrl+Shift+E de xem cay thu muc!" -ForegroundColor Yellow
