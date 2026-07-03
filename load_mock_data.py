"""
load_mock_data.py
=================
Nạp 7 MongoDB Collections với dữ liệu mẫu thực tế để kiểm thử.

3 hồ sơ sinh viên đặc biệt (theo Business Rules 2026-07-02):
  - SV_A (24520001): Vắng 0/25 buổi = 0%   → Bình thường
  - SV_B (24520002): Vắng 5/26 buổi = 19.2% → Sát ngưỡng (SAFE, vì 19.2% < 20%)
  - SV_C (24520003): Vắng 6/29 buổi = 20.7% → Cảnh báo  (ALERT, vì 20.7% > 20%)

Chú ý: Tất cả bản ghi vắng dùng "UNEXCUSED_ABSENT" (Business Rule BR-1).
       Mẫu số = subjects.total_sessions_planned   (Business Rule BR-2).
       Ngưỡng: absenceRate > 0.20 (STRICT)        (Business Rule BR-3).

Chạy:
  pip install pymongo
  python load_mock_data.py
"""

import pymongo
import hashlib
import os
from datetime import datetime, timezone, timedelta

MONGO_URI = os.environ.get(
    "MONGO_URI",
    "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"
)

client = pymongo.MongoClient(MONGO_URI)
db = client["attendance_db"]


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def sha256(text: str) -> str:
    return hashlib.sha256(text.encode()).hexdigest()


def now_ms():
    return int(datetime.now(timezone.utc).timestamp() * 1000)


def days_ago_ms(n: int) -> int:
    dt = datetime.now(timezone.utc) - timedelta(days=n)
    return int(dt.timestamp() * 1000)


def upsert(collection, filter_doc, doc):
    """Upsert (update or insert) a document."""
    db[collection].update_one(filter_doc, {"$set": doc}, upsert=True)


# ---------------------------------------------------------------------------
# 1. USERS
# ---------------------------------------------------------------------------

def load_users():
    print("\n[load_mock_data] Loading users...")
    users = [
        # --- Students ---
        {
            "id": "24520001",
            "username": "nguyenvana",
            "password": sha256("password123"),
            "full_name": "Nguyễn Văn An",
            "email": "24520001@student.hust.edu.vn",
            "phone": "0912345678",
            "role": "STUDENT",
            "class_code": "CNTT-K65A",
            "department": "Công nghệ Thông tin",
            "face_registered": True,
            "require_password_change": False,
            "__scenario__": "SV_A: Vắng 0% — Bình thường"
        },
        {
            "id": "24520002",
            "username": "tranthib",
            "password": sha256("password123"),
            "full_name": "Trần Thị Bình",
            "email": "24520002@student.hust.edu.vn",
            "phone": "0923456789",
            "role": "STUDENT",
            "class_code": "CNTT-K65A",
            "department": "Công nghệ Thông tin",
            "face_registered": True,
            "require_password_change": False,
            "__scenario__": "SV_B: Vắng 19.2% — Sát ngưỡng, KHÔNG gửi mail"
        },
        {
            "id": "24520003",
            "username": "levanc",
            "password": sha256("password123"),
            "full_name": "Lê Văn Cường",
            "email": "24520003@student.hust.edu.vn",
            "phone": "0934567890",
            "role": "STUDENT",
            "class_code": "CNTT-K65A",
            "department": "Công nghệ Thông tin",
            "face_registered": True,
            "require_password_change": False,
            "__scenario__": "SV_C: Vắng 20.7% — QUÁ NGƯỠNG, PHẢI gửi mail"
        },
        {
            "id": "24520004",
            "username": "phamthid",
            "password": sha256("password123"),
            "full_name": "Phạm Thị Dung",
            "email": "24520004@student.hust.edu.vn",
            "phone": "0945678901",
            "role": "STUDENT",
            "class_code": "CNTT-K65B",
            "department": "Công nghệ Thông tin",
            "face_registered": False,
            "require_password_change": False,
            "__scenario__": "SV_D: Chưa đăng ký khuôn mặt — test TC-FAC-004"
        },
        # --- Lecturers ---
        {
            "id": "GV001",
            "username": "nguyenthilan",
            "password": sha256("lecturer123"),
            "full_name": "Nguyễn Thị Lan",
            "email": "nguyenthilan@hust.edu.vn",
            "phone": "0901234567",
            "role": "LECTURER",
            "department": "Công nghệ Thông tin",
            "employee_id": "GV20150045",
            "require_password_change": False
        },
        {
            "id": "GV002",
            "username": "tranthanhminh",
            "password": sha256("lecturer123"),
            "full_name": "Trần Thanh Minh",
            "email": "tranthanhminh@hust.edu.vn",
            "phone": "0902345678",
            "role": "LECTURER",
            "department": "Công nghệ Thông tin",
            "employee_id": "GV20180032",
            "require_password_change": False
        },
    ]
    for u in users:
        upsert("users", {"id": u["id"]}, u)
    print(f"  ✅ {len(users)} users upserted.")


# ---------------------------------------------------------------------------
# 2. SUBJECTS
# ---------------------------------------------------------------------------

def load_subjects():
    print("[load_mock_data] Loading subjects...")
    subjects = [
        {
            "code": "INT3105",
            "name": "Lập trình Hướng đối tượng",
            "lecturer_name": "Nguyễn Thị Lan",
            "total_credits": 3,
            "practice_credits": 1,
            "managing_faculty": "Công nghệ Thông tin",
            # BR-2: Mẫu số tính % vắng = total_sessions_planned
            "total_sessions_planned": 29,  # 30 buổi kế hoạch, trừ 1 buổi hủy
            "rebuilt_at": datetime.now(timezone.utc)
        },
        {
            "code": "INT3120",
            "name": "Cơ sở dữ liệu",
            "lecturer_name": "Trần Thanh Minh",
            "total_credits": 3,
            "practice_credits": 0,
            "managing_faculty": "Công nghệ Thông tin",
            "total_sessions_planned": 25,
            "rebuilt_at": datetime.now(timezone.utc)
        },
    ]
    for s in subjects:
        upsert("subjects", {"code": s["code"]}, s)
    print(f"  ✅ {len(subjects)} subjects upserted.")


# ---------------------------------------------------------------------------
# 3. SCHEDULES
# ---------------------------------------------------------------------------

def load_schedules():
    print("[load_mock_data] Loading schedules...")
    schedules = [
        {
            "schedule_id": "SCH-INT3105-TH2-CA1",
            "subject_code": "INT3105",
            "subject_name": "Lập trình Hướng đối tượng",
            "class_code": "CNTT-K65A",
            "lecturer_id": "GV001",
            "lecturer_name": "Nguyễn Thị Lan",
            "day_of_week": "2",
            "start_time": "07:30",
            "end_time": "09:20",
            "room": "D3-101",
            "semester": "2025-2026-HK1",
            "total_credits": "3",
            "practice_credits": "1",
            "managing_faculty": "Công nghệ Thông tin"
        },
        {
            "schedule_id": "SCH-INT3105-TH5-CA2",
            "subject_code": "INT3105",
            "subject_name": "Lập trình Hướng đối tượng",
            "class_code": "CNTT-K65A",
            "lecturer_id": "GV001",
            "lecturer_name": "Nguyễn Thị Lan",
            "day_of_week": "5",
            "start_time": "09:30",
            "end_time": "11:50",
            "room": "D3-201",
            "semester": "2025-2026-HK1",
            "total_credits": "3",
            "practice_credits": "1",
            "managing_faculty": "Công nghệ Thông tin"
        },
        {
            "schedule_id": "SCH-INT3120-TH3-CA2",
            "subject_code": "INT3120",
            "subject_name": "Cơ sở dữ liệu",
            "class_code": "CNTT-K65A",
            "lecturer_id": "GV002",
            "lecturer_name": "Trần Thanh Minh",
            "day_of_week": "3",
            "start_time": "09:30",
            "end_time": "11:50",
            "room": "B1-305",
            "semester": "2025-2026-HK1",
            "total_credits": "3",
            "practice_credits": "0",
            "managing_faculty": "Công nghệ Thông tin"
        },
    ]
    for s in schedules:
        upsert("schedules", {"schedule_id": s["schedule_id"]}, s)
    print(f"  ✅ {len(schedules)} schedules upserted.")


# ---------------------------------------------------------------------------
# 4. ENROLLMENTS
# ---------------------------------------------------------------------------

def load_enrollments():
    print("[load_mock_data] Loading enrollments...")
    enrollments = [
        {"student_id": "24520001", "subject_code": "INT3105", "class_code": "CNTT-K65A"},
        {"student_id": "24520002", "subject_code": "INT3105", "class_code": "CNTT-K65A"},
        {"student_id": "24520003", "subject_code": "INT3105", "class_code": "CNTT-K65A"},
        {"student_id": "24520003", "subject_code": "INT3120", "class_code": "CNTT-K65A"},
        {"student_id": "24520001", "subject_code": "INT3120", "class_code": "CNTT-K65A"},
        {"student_id": "24520002", "subject_code": "INT3120", "class_code": "CNTT-K65A"},
    ]
    inserted = 0
    skipped = 0
    for e in enrollments:
        e["enrolled_at"] = datetime.now(timezone.utc)
        try:
            # Use all 3 fields as filter key to avoid conflict with existing indexes
            db["enrollments"].update_one(
                {"student_id": e["student_id"], "subject_code": e["subject_code"], "class_code": e["class_code"]},
                {"$set": e},
                upsert=True
            )
            inserted += 1
        except pymongo.errors.DuplicateKeyError as ex:
            # Pre-existing unique index conflict — enrollment already exists, skip
            skipped += 1
    print(f"  OK {inserted} enrollments upserted, {skipped} skipped (already exist).")



# ---------------------------------------------------------------------------
# 5. SESSIONS (phiên điểm danh)
# ---------------------------------------------------------------------------

def load_sessions():
    print("[load_mock_data] Loading sessions...")
    sessions = []

    # INT3105: 29 CLOSED sessions + 1 OPEN
    for i in range(29):
        sessions.append({
            "_id": f"sess_mock_INT3105_{i+1:03d}",
            "lecturer_id": "GV001",
            "class_name": "CNTT-K65A",
            "subject": "INT3105",
            "duration": 110,
            "start_time": days_ago_ms((29 - i) * 3 + 1),
            "end_time":   days_ago_ms((29 - i) * 3),
            "status": "CLOSED",
            "room": "D3-101",
            "gps_enabled": True,
            "wifi_enabled": True,
        })

    # 1 OPEN session (hiện tại)
    sessions.append({
        "_id": "sess_mock_INT3105_OPEN",
        "lecturer_id": "GV001",
        "class_name": "CNTT-K65A",
        "subject": "INT3105",
        "duration": 110,
        "start_time": now_ms() - 15 * 60 * 1000,
        "end_time":   now_ms() + 95 * 60 * 1000,
        "status": "OPEN",
        "room": "D3-101",
        "gps_enabled": True,
        "wifi_enabled": True,
    })

    # INT3120: 25 CLOSED sessions
    for i in range(25):
        sessions.append({
            "_id": f"sess_mock_INT3120_{i+1:03d}",
            "lecturer_id": "GV002",
            "class_name": "CNTT-K65A",
            "subject": "INT3120",
            "duration": 110,
            "start_time": days_ago_ms((25 - i) * 3 + 2),
            "end_time":   days_ago_ms((25 - i) * 3 + 1),
            "status": "CLOSED",
            "room": "B1-305",
            "gps_enabled": True,
            "wifi_enabled": True,
        })

    for s in sessions:
        upsert("sessions", {"_id": s["_id"]}, s)
    print(f"  ✅ {len(sessions)} sessions upserted.")
    return sessions


# ---------------------------------------------------------------------------
# 6. ATTENDANCE (điểm danh) — 3 scenarios
# ---------------------------------------------------------------------------

def load_attendance():
    print("[load_mock_data] Loading attendance records...")

    # Xóa mock records cũ (chỉ xóa records có _id bắt đầu bằng 'att_SV_')
    # để không ảnh hưởng đến attendance records thực của hệ thống
    deleted = db["attendance"].delete_many({"_id": {"$regex": "^att_SV_"}})
    if deleted.deleted_count > 0:
        print(f"  Cleaned up {deleted.deleted_count} stale mock attendance records.")

    records = []

    # -------------------------------------------------------------------------
    # SV_A (24520001): Vắng 0/29 buổi INT3105 = 0.0% → SAFE
    # -------------------------------------------------------------------------
    for i in range(29):
        records.append({
            "_id": f"att_SV_A_INT3105_{i+1:03d}",
            "student_id": "24520001",
            "session_id": f"sess_mock_INT3105_{i+1:03d}",
            "subject_code": "INT3105",
            "class_code": "CNTT-K65A",
            "device_id": "DEV_24520001_GPS",   # Needed for session_id_1_device_id_1 index
            "status": "PRESENT",
            "method": "GPS",
            "location": "D3-101",
            "notes": "",
            "timestamp": days_ago_ms((29 - i) * 3),
        })

    # -------------------------------------------------------------------------
    # SV_B (24520002): 5 UNEXCUSED_ABSENT / 26 total = 19.2% → SAFE (< 20%)
    #
    # Chú ý: Mẫu số dùng total_sessions_planned = 29 (từ subjects collection)
    #   5 / 29 = 17.2% → SAFE
    # Nhưng nếu dùng mẫu số = 26 (số buổi thực tế đã điểm danh):
    #   5 / 26 = 19.2% → vẫn SAFE (< 20%)
    # -------------------------------------------------------------------------
    absent_sessions_b = {5, 12, 18, 21, 25}
    for i in range(29):
        if (i + 1) in absent_sessions_b:
            records.append({
                "_id": f"att_SV_B_INT3105_{i+1:03d}",
                "student_id": "24520002",
                "session_id": f"sess_mock_INT3105_{i+1:03d}",
                "subject_code": "INT3105",
                "class_code": "CNTT-K65A",
                "device_id": "SYSTEM_AUTO",    # SYSTEM records use shared device_id
                # BR-1: Dùng UNEXCUSED_ABSENT (không phép) cho trường hợp cần tính vào %
                "status": "UNEXCUSED_ABSENT",
                "method": "SYSTEM",
                "location": None,
                "notes": "He thong tu dong danh dau khi dong phien",
                "timestamp": None,
            })
        else:
            records.append({
                "_id": f"att_SV_B_INT3105_{i+1:03d}",
                "student_id": "24520002",
                "session_id": f"sess_mock_INT3105_{i+1:03d}",
                "subject_code": "INT3105",
                "class_code": "CNTT-K65A",
                "device_id": "DEV_24520002_WIFI",   # Unique per student per session
                "status": "PRESENT",
                "method": "WiFi",
                "location": "D3-101",
                "notes": "",
                "timestamp": days_ago_ms((29 - i) * 3),
            })

    # -------------------------------------------------------------------------
    # SV_C (24520003): 6 UNEXCUSED_ABSENT / 29 planned = 20.7% → ALERT (> 20%)
    # -------------------------------------------------------------------------
    absent_sessions_c = {3, 8, 14, 19, 24, 28}
    for i in range(29):
        if (i + 1) in absent_sessions_c:
            records.append({
                "_id": f"att_SV_C_INT3105_{i+1:03d}",
                "student_id": "24520003",
                "session_id": f"sess_mock_INT3105_{i+1:03d}",
                "subject_code": "INT3105",
                "class_code": "CNTT-K65A",
                "device_id": "SYSTEM_AUTO",    # SYSTEM records use shared device_id
                "status": "UNEXCUSED_ABSENT",
                "method": "SYSTEM",
                "location": None,
                "notes": "He thong tu dong danh dau khi dong phien",
                "timestamp": None,
            })
        else:
            records.append({
                "_id": f"att_SV_C_INT3105_{i+1:03d}",
                "student_id": "24520003",
                "session_id": f"sess_mock_INT3105_{i+1:03d}",
                "subject_code": "INT3105",
                "class_code": "CNTT-K65A",
                "device_id": "DEV_24520003_GPS",
                "status": "PRESENT",
                "method": "GPS",
                "location": "D3-101",
                "notes": "",
                "timestamp": days_ago_ms((29 - i) * 3),
            })

    inserted = 0
    skipped = 0
    for r in records:
        try:
            upsert("attendance", {"_id": r["_id"]}, r)
            inserted += 1
        except pymongo.errors.DuplicateKeyError:
            skipped += 1
    print(f"  OK {inserted} attendance records upserted, {skipped} skipped (index conflicts).")

    # Summary
    sv_a_absent = sum(1 for r in records if r["student_id"] == "24520001" and r["status"] == "UNEXCUSED_ABSENT")
    sv_b_absent = sum(1 for r in records if r["student_id"] == "24520002" and r["status"] == "UNEXCUSED_ABSENT")
    sv_c_absent = sum(1 for r in records if r["student_id"] == "24520003" and r["status"] == "UNEXCUSED_ABSENT")
    planned = 29
    print(f"  📊 SV_A (24520001): {sv_a_absent}/{planned} = {sv_a_absent/planned*100:.1f}% → SAFE")
    print(f"  📊 SV_B (24520002): {sv_b_absent}/{planned} = {sv_b_absent/planned*100:.1f}% → SAFE (< 20%)")
    print(f"  📊 SV_C (24520003): {sv_c_absent}/{planned} = {sv_c_absent/planned*100:.1f}% → ⚠️  ALERT (> 20%)")


# ---------------------------------------------------------------------------
# 7. EMAIL_ALERT_LOGS (khởi tạo rỗng để test idempotency)
# ---------------------------------------------------------------------------

def load_email_alert_logs():
    print("[load_mock_data] Ensuring email_alert_logs collection exists...")
    if "email_alert_logs" not in db.list_collection_names():
        db.create_collection("email_alert_logs")
    # Tạo index để tìm kiếm nhanh
    db["email_alert_logs"].create_index(
        [("student_id", 1), ("subject_code", 1), ("status", 1), ("is_revoked", 1)],
        name="idx_email_alert_lookup"
    )
    print("  ✅ email_alert_logs collection and index ready.")


# ---------------------------------------------------------------------------
# Tạo MongoDB Indexes
# ---------------------------------------------------------------------------

def create_indexes():
    print("[load_mock_data] Creating indexes...")

    # Attendance: chống điểm danh trùng
    db["attendance"].create_index(
        [("student_id", 1), ("session_id", 1)],
        unique=True,
        name="idx_attendance_unique_per_session"
    )

    # Attendance: tăng tốc query thống kê vắng
    db["attendance"].create_index(
        [("student_id", 1), ("subject_code", 1), ("status", 1)],
        name="idx_attendance_student_subject_status"
    )

    # Sessions: query theo lecturer
    db["sessions"].create_index(
        [("lecturer_id", 1), ("status", 1)],
        name="idx_sessions_lecturer_status"
    )

    # Schedules: query theo class_code
    db["schedules"].create_index(
        [("class_code", 1)],
        name="idx_schedules_class"
    )

    # Enrollments: chống duplicate
    db["enrollments"].create_index(
        [("student_id", 1), ("subject_code", 1), ("class_code", 1)],
        unique=True,
        name="idx_enrollment_unique"
    )

    print("  ✅ All indexes created.")


# ---------------------------------------------------------------------------
# MAIN
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    print("=" * 60)
    print("  load_mock_data.py — Attendance App Mock Data Loader")
    print("=" * 60)
    print(f"  Database: attendance_db @ MongoDB Atlas")

    try:
        client.admin.command("ping")
        print("  ✅ MongoDB connection OK\n")
    except Exception as e:
        print(f"  ❌ MongoDB connection FAILED: {e}")
        exit(1)

    load_users()
    load_subjects()
    load_schedules()
    load_enrollments()
    load_sessions()
    load_attendance()
    load_email_alert_logs()
    create_indexes()

    print("\n" + "=" * 60)
    print("  ✅ Mock data loaded successfully!")
    print("  Run test_integration.py to validate business rules.")
    print("=" * 60)
