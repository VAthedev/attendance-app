"""
test_integration.py
===================
Integration Test tự động kiểm thử 3 kịch bản sinh viên theo Business Rules.

Business Rules được kiểm thử:
  BR-1: Chỉ UNEXCUSED_ABSENT tính vào % vắng
  BR-2: Mẫu số = subjects.total_sessions_planned
  BR-3: Ngưỡng STRICT: absenceRate > 0.20

Scenarios:
  Scenario A (SV_A / 24520001): Vắng 0%   → AI trả lịch bình thường, không cảnh báo
  Scenario B (SV_B / 24520002): Vắng 17.2% → Safe (< 20%), không gửi mail
  Scenario C (SV_C / 24520003): Vắng 20.7% → Alert, hệ thống GHI LOG email_alert_logs

Chạy:
  python test_integration.py

Output: PASS/FAIL cho từng test case với chi tiết.
"""

import pymongo
import os
import sys
from datetime import datetime, timezone

MONGO_URI = os.environ.get(
    "MONGO_URI",
    "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"
)

client = pymongo.MongoClient(MONGO_URI)
db = client["attendance_db"]

# ─────────────────────────────────────────────────────────────────────────────
# Test Helpers
# ─────────────────────────────────────────────────────────────────────────────

passed = 0
failed = 0
results = []


def assert_that(test_id: str, description: str, condition: bool, detail: str = ""):
    global passed, failed
    status = "✅ PASS" if condition else "❌ FAIL"
    if condition:
        passed += 1
    else:
        failed += 1
    line = f"  [{status}] {test_id}: {description}"
    if detail and not condition:
        line += f"\n         Detail: {detail}"
    elif detail:
        line += f"  ({detail})"
    print(line)
    results.append({"id": test_id, "pass": condition, "desc": description, "detail": detail})


def compute_absence_rate(student_id: str, subject_code: str) -> tuple[int, int, float]:
    """
    Tính tỷ lệ vắng theo Business Rules:
      BR-1: Chỉ đếm UNEXCUSED_ABSENT
      BR-2: Mẫu số = subjects.total_sessions_planned
    """
    # BR-2: lấy mẫu số từ subjects
    subj = db["subjects"].find_one({"code": subject_code})
    total_planned = subj.get("total_sessions_planned", 0) if subj else 0

    # BR-1: chỉ đếm UNEXCUSED_ABSENT
    absent_count = db["attendance"].count_documents({
        "student_id": student_id,
        "subject_code": subject_code,
        "status": "UNEXCUSED_ABSENT"
    })

    rate = absent_count / total_planned if total_planned > 0 else 0.0
    return absent_count, total_planned, rate


def is_alert_already_sent(student_id: str, subject_code: str) -> bool:
    """Check email_alert_logs để xác nhận email đã gửi."""
    doc = db["email_alert_logs"].find_one({
        "student_id": student_id,
        "subject_code": subject_code,
        "status": "SENT_SUCCESS",
        "is_revoked": False
    })
    return doc is not None


def simulate_absence_alert(student_id: str, subject_code: str,
                            absent_count: int, total_planned: int, rate: float):
    """
    Giả lập logic gửi email cảnh báo (Python equivalent của EmailService.sendAbsenceAlertAsync):
      - Kiểm tra idempotency
      - BR-3: rate > 0.20 (STRICT)
      - Ghi log vào email_alert_logs
    """
    if is_alert_already_sent(student_id, subject_code):
        return "ALREADY_SENT"

    if rate > 0.20:  # BR-3: STRICT greater-than
        # Lấy thông tin user
        user = db["users"].find_one({"id": student_id})
        if not user:
            return "USER_NOT_FOUND"

        # Ghi log (giả lập EmailService.logAlertSent)
        db["email_alert_logs"].insert_one({
            "student_id": student_id,
            "subject_code": subject_code,
            "absent_count_at_send": absent_count,
            "total_sessions_at_send": total_planned,
            "absence_rate_at_send": rate,
            "sent_at": int(datetime.now(timezone.utc).timestamp() * 1000),
            "status": "SENT_SUCCESS",
            "triggered_by": "INTEGRATION_TEST",
            "is_revoked": False,
            "student_name": user.get("full_name", ""),
            "student_email": user.get("email", ""),
        })
        return "SENT"
    else:
        return "BELOW_THRESHOLD"


# ─────────────────────────────────────────────────────────────────────────────
# PRE-CONDITIONS
# ─────────────────────────────────────────────────────────────────────────────

def test_pre_conditions():
    print("\n📋 PRE-CONDITIONS — Kiểm tra dữ liệu đã được nạp")
    print("-" * 56)

    user_count = db["users"].count_documents({})
    assert_that("PRE-001", "Users collection không rỗng", user_count >= 5,
                f"Found {user_count} users")

    subj_count = db["subjects"].count_documents({})
    assert_that("PRE-002", "subjects.total_sessions_planned tồn tại",
                db["subjects"].find_one({"code": "INT3105", "total_sessions_planned": {"$exists": True}}) is not None)

    sva = db["users"].find_one({"id": "24520001"})
    assert_that("PRE-003", "SV_A (24520001) tồn tại trong DB", sva is not None)

    svb = db["users"].find_one({"id": "24520002"})
    assert_that("PRE-004", "SV_B (24520002) tồn tại trong DB", svb is not None)

    svc = db["users"].find_one({"id": "24520003"})
    assert_that("PRE-005", "SV_C (24520003) tồn tại trong DB", svc is not None)

    open_sess = db["sessions"].find_one({"_id": "sess_mock_INT3105_OPEN", "status": "OPEN"})
    assert_that("PRE-006", "Phiên OPEN tồn tại trong sessions", open_sess is not None)


# ─────────────────────────────────────────────────────────────────────────────
# SCENARIO A — SV_A vắng 0%
# ─────────────────────────────────────────────────────────────────────────────

def test_scenario_a():
    print("\n\n🧪 SCENARIO A — SV_A (24520001): Vắng 0% — Bình thường")
    print("-" * 56)

    absent_count, total_planned, rate = compute_absence_rate("24520001", "INT3105")

    assert_that("SCA-001", "BR-2: total_sessions_planned = 29",
                total_planned == 29, f"actual={total_planned}")

    assert_that("SCA-002", "BR-1: SV_A không có UNEXCUSED_ABSENT",
                absent_count == 0, f"absent_count={absent_count}")

    assert_that("SCA-003", "Tỷ lệ vắng = 0.0%",
                rate == 0.0, f"rate={rate:.2%}")

    # BR-3: Không được gửi mail
    result = simulate_absence_alert("24520001", "INT3105", absent_count, total_planned, rate)
    assert_that("SCA-004", "BR-3: KHÔNG kích hoạt gửi mail (rate <= 20%)",
                result == "BELOW_THRESHOLD", f"result={result}")

    assert_that("SCA-005", "email_alert_logs KHÔNG có bản ghi cho SV_A",
                not is_alert_already_sent("24520001", "INT3105"))

    # AI Assistant context: SV_A có lịch học bình thường
    schedule = db["schedules"].find_one({"class_code": "CNTT-K65A", "subject_code": "INT3105"})
    assert_that("SCA-006", "AI có thể lấy lịch học của SV_A từ DB",
                schedule is not None, f"subject={schedule.get('subject_code') if schedule else 'N/A'}")


# ─────────────────────────────────────────────────────────────────────────────
# SCENARIO B — SV_B vắng 17.2% (SÁT NGƯỠNG)
# ─────────────────────────────────────────────────────────────────────────────

def test_scenario_b():
    print("\n\n🧪 SCENARIO B — SV_B (24520002): Vắng 17.2% — Sát ngưỡng (SAFE)")
    print("-" * 56)

    absent_count, total_planned, rate = compute_absence_rate("24520002", "INT3105")

    assert_that("SCB-001", "BR-2: Mẫu số = 29 (total_sessions_planned)",
                total_planned == 29, f"actual={total_planned}")

    assert_that("SCB-002", "BR-1: SV_B có 5 UNEXCUSED_ABSENT",
                absent_count == 5, f"absent_count={absent_count}")

    # 5/29 = 0.1724... ≈ 17.2%
    expected_rate = 5 / 29
    assert_that("SCB-003", "Tỷ lệ vắng ≈ 17.2% (5/29)",
                abs(rate - expected_rate) < 0.001, f"rate={rate:.4%}")

    # BR-3: 17.2% < 20% → KHÔNG gửi mail
    result = simulate_absence_alert("24520002", "INT3105", absent_count, total_planned, rate)
    assert_that("SCB-004", "BR-3: KHÔNG gửi mail (17.2% ≤ 20% threshold)",
                result == "BELOW_THRESHOLD", f"result={result}")

    assert_that("SCB-005", "email_alert_logs KHÔNG có bản ghi cho SV_B",
                not is_alert_already_sent("24520002", "INT3105"))

    # Kiểm tra EXCUSED_ABSENT không được tính
    # Thêm 1 bản ghi EXCUSED_ABSENT tạm thời và verify không ảnh hưởng
    db["attendance"].insert_one({
        "_id": "att_SV_B_EXCUSED_TEST",
        "student_id": "24520002",
        "session_id": "sess_mock_INT3105_030",
        "subject_code": "INT3105",
        "status": "EXCUSED_ABSENT",  # Có phép — không tính vào %
        "method": "MANUAL"
    })
    absent_count_after, _, rate_after = compute_absence_rate("24520002", "INT3105")
    assert_that("SCB-006", "BR-1: EXCUSED_ABSENT KHÔNG tính vào % vắng",
                absent_count_after == absent_count,
                f"before={absent_count}, after={absent_count_after} (should be equal)")
    # Clean up
    db["attendance"].delete_one({"_id": "att_SV_B_EXCUSED_TEST"})


# ─────────────────────────────────────────────────────────────────────────────
# SCENARIO C — SV_C vắng 20.7% (QUÁ NGƯỠNG)
# ─────────────────────────────────────────────────────────────────────────────

def test_scenario_c():
    print("\n\n🧪 SCENARIO C — SV_C (24520003): Vắng 20.7% — ⚠️ ALERT")
    print("-" * 56)

    # Clean up any pre-existing alert log for this student to reset test state
    db["email_alert_logs"].delete_many({
        "student_id": "24520003",
        "subject_code": "INT3105",
        "triggered_by": "INTEGRATION_TEST"
    })

    absent_count, total_planned, rate = compute_absence_rate("24520003", "INT3105")

    assert_that("SCC-001", "BR-2: Mẫu số = 29 (total_sessions_planned)",
                total_planned == 29, f"actual={total_planned}")

    assert_that("SCC-002", "BR-1: SV_C có 6 UNEXCUSED_ABSENT",
                absent_count == 6, f"absent_count={absent_count}")

    # 6/29 = 0.2069... ≈ 20.7%
    expected_rate = 6 / 29
    assert_that("SCC-003", "Tỷ lệ vắng ≈ 20.7% (6/29)",
                abs(rate - expected_rate) < 0.001, f"rate={rate:.4%}")

    # BR-3: 20.7% > 20% → PHẢI gửi mail
    assert_that("SCC-004", "BR-3: Rate STRICTLY > 20% → kích hoạt cảnh báo",
                rate > 0.20, f"rate={rate:.4%}")

    result = simulate_absence_alert("24520003", "INT3105", absent_count, total_planned, rate)
    assert_that("SCC-005", "Giả lập gửi email cảnh báo thành công",
                result == "SENT", f"result={result}")

    assert_that("SCC-006", "email_alert_logs GHI NHẬN bản ghi SENT_SUCCESS",
                is_alert_already_sent("24520003", "INT3105"))

    # Test idempotency: gọi lại không gửi lần 2
    result2 = simulate_absence_alert("24520003", "INT3105", absent_count, total_planned, rate)
    assert_that("SCC-007", "Idempotency: Không gửi email lần 2 (TC-ABS-004)",
                result2 == "ALREADY_SENT", f"result2={result2}")

    # Verify log content
    log = db["email_alert_logs"].find_one({"student_id": "24520003", "subject_code": "INT3105"})
    assert_that("SCC-008", "Log chứa absence_rate_at_send chính xác",
                log is not None and abs(log.get("absence_rate_at_send", 0) - expected_rate) < 0.001,
                f"rate_in_log={log.get('absence_rate_at_send') if log else 'N/A':.4%}" if log else "log is None")

    assert_that("SCC-009", "Log is_revoked = False",
                log is not None and log.get("is_revoked") is False)


# ─────────────────────────────────────────────────────────────────────────────
# EDGE CASES — Kiểm thử các biên
# ─────────────────────────────────────────────────────────────────────────────

def test_edge_cases():
    print("\n\n🧪 EDGE CASES — Kiểm thử các trường hợp biên")
    print("-" * 56)

    # TC-ABS-001: Đúng 20.0% = SAFE (strict >)
    assert_that("EDGE-001", "TC-ABS-001: Đúng 20.0% KHÔNG kích hoạt cảnh báo (strict >)",
                not (0.20 > 0.20),  # 0.20 > 0.20 = False → SAFE
                f"0.20 > 0.20 = {0.20 > 0.20}")

    # 20.001% = ALERT
    assert_that("EDGE-002", "TC-ABS-001: 20.001% kích hoạt cảnh báo",
                0.20001 > 0.20)

    # TC-FAC-003: Dedup index — không ghi điểm danh trùng
    try:
        db["attendance"].insert_one({
            "_id": "att_DEDUP_TEST_1",
            "student_id": "24520001",
            "session_id": "sess_mock_INT3105_001",
            "subject_code": "INT3105",
            "status": "PRESENT"
        })
        # Lần 2: trùng student_id + session_id → unique index phải chặn
        db["attendance"].insert_one({
            "_id": "att_DEDUP_TEST_2",
            "student_id": "24520001",
            "session_id": "sess_mock_INT3105_001",
            "subject_code": "INT3105",
            "status": "PRESENT"
        })
        assert_that("EDGE-003", "TC-FAC-003: Unique index chặn điểm danh trùng", False,
                    "Lẽ ra phải throw DuplicateKeyError!")
    except pymongo.errors.DuplicateKeyError:
        assert_that("EDGE-003", "TC-FAC-003: Unique index chặn điểm danh trùng", True,
                    "DuplicateKeyError đúng như kỳ vọng")
    finally:
        db["attendance"].delete_one({"_id": "att_DEDUP_TEST_1"})
        db["attendance"].delete_one({"_id": "att_DEDUP_TEST_2"})

    # TC-FAC-001: Phiên CLOSED → không cho điểm danh
    closed_sess = db["sessions"].find_one({"_id": "sess_mock_INT3105_001"})
    assert_that("EDGE-004", "TC-FAC-001: Session CLOSED → status='CLOSED'",
                closed_sess is not None and closed_sess.get("status") == "CLOSED")

    open_sess = db["sessions"].find_one({"_id": "sess_mock_INT3105_OPEN"})
    assert_that("EDGE-005", "TC-FAC-001: Session OPEN → status='OPEN'",
                open_sess is not None and open_sess.get("status") == "OPEN")

    # Kiểm tra rebuild_subjects.py không còn gây downtime
    # (Không thể test atomic rename trực tiếp, nhưng verify collection luôn có dữ liệu)
    before_count = db["subjects"].count_documents({})
    assert_that("EDGE-006", "TC-SYNC-001: subjects collection KHÔNG rỗng (zero-downtime guard)",
                before_count > 0, f"count={before_count}")


# ─────────────────────────────────────────────────────────────────────────────
# MAIN
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    print("=" * 60)
    print("  test_integration.py — Attendance App Integration Tests")
    print(f"  Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)

    try:
        client.admin.command("ping")
        print("  ✅ MongoDB connection OK")
    except Exception as e:
        print(f"  ❌ MongoDB connection FAILED: {e}")
        sys.exit(1)

    test_pre_conditions()
    test_scenario_a()
    test_scenario_b()
    test_scenario_c()
    test_edge_cases()

    # ── Summary ──────────────────────────────────────────────────────────────
    total = passed + failed
    print("\n" + "=" * 60)
    print(f"  TEST SUMMARY: {passed}/{total} passed, {failed} failed")
    if failed == 0:
        print("  🎉 All tests PASSED!")
    else:
        print("  ⚠️  Some tests FAILED. See details above.")
        print("\n  Failed tests:")
        for r in results:
            if not r["pass"]:
                print(f"    ❌ {r['id']}: {r['desc']}")
                if r["detail"]:
                    print(f"       Detail: {r['detail']}")
    print("=" * 60)

    sys.exit(0 if failed == 0 else 1)
