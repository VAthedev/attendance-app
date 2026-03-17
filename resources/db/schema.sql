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
