import pymongo
import os
import random
import time
from datetime import datetime, timezone, timedelta
import uuid

MONGO_URI = os.environ.get(
    "MONGO_URI",
    "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"
)

client = pymongo.MongoClient(MONGO_URI)
db = client["attendance_db"]

def now_ms():
    return int(datetime.now(timezone.utc).timestamp() * 1000)

print("Starting mock data generation based on real data...")

# 1. Clear existing sessions, attendance, notifications
print("Clearing old sessions, attendance, and notifications...")
db.sessions.delete_many({})
db.attendance.delete_many({})
db.notifications.delete_many({})

# 2. Get all schedules
schedules = list(db.schedules.find({}))
print(f"Found {len(schedules)} schedules.")

if not schedules:
    print("No schedules found! Cannot generate sessions.")
    exit(1)

# 3. Get all enrollments
enrollments = list(db.enrollments.find({}))
print(f"Found {len(enrollments)} enrollments.")

# Group enrollments by class_code
class_enrollments = {}
for enr in enrollments:
    cc = enr.get("class_code")
    if not cc: continue
    if cc not in class_enrollments:
        class_enrollments[cc] = []
    class_enrollments[cc].append(enr["student_id"])

total_sessions = 0
total_attendances = 0

methods = ["QR", "GPS", "WiFi"]
statuses = ["PRESENT", "ABSENT", "LATE"]
weights = [0.8, 0.1, 0.1]  # 80% present, 10% absent, 10% late

base_time = now_ms()
day_ms = 24 * 60 * 60 * 1000

print("Generating sessions and attendance...")
# Generate 5 past sessions for each schedule
for schedule in schedules:
    class_code = schedule.get("class_code")
    subject_code = schedule.get("subject_code")
    lecturer_id = schedule.get("lecturer_id")
    
    if not class_code or not subject_code:
        continue
        
    students_in_class = class_enrollments.get(class_code, [])
    if not students_in_class:
        continue
        
    for i in range(5):
        # Session 1 to 5 days ago
        session_time = base_time - ((5 - i) * day_ms)
        schedule_id_str = str(schedule.get("_id", "unknown"))
        session_id = f"sess_{class_code}_{subject_code}_{schedule_id_str}_{i}"
        
        session = {
            "_id": session_id,
            "schedule_id": str(schedule.get("_id", "unknown")),
            "class_name": class_code,
            "subject": subject_code,
            "start_time": session_time,
            "end_time": session_time + (2 * 60 * 60 * 1000), # 2 hours later
            "status": "CLOSED",
            "method": random.choice(methods),
            "room": f"Room-{random.randint(100, 400)}",
            "lecturer_id": lecturer_id
        }
        db.sessions.insert_one(session)
        total_sessions += 1
        
        # Generate attendance for each student in this class
        attendances_to_insert = []
        for student_id in students_in_class:
            status = random.choices(statuses, weights)[0]
            
            att = {
                "session_id": session_id,
                "student_id": student_id,
                "subject_code": subject_code,
                "class_code": class_code,
                "method": "SYSTEM" if status == "ABSENT" else random.choice(methods),
                "status": "UNEXCUSED_ABSENT" if status == "ABSENT" else status,
                "timestamp": session_time + random.randint(0, 15 * 60 * 1000), # randomly up to 15 mins late
                "location": session["room"] if status != "ABSENT" else "",
                "device_id": f"device_{student_id}"
            }
            attendances_to_insert.append(att)
            
        if attendances_to_insert:
            db.attendance.insert_many(attendances_to_insert)
            total_attendances += len(attendances_to_insert)

print(f"Generated {total_sessions} sessions and {total_attendances} attendance records.")

# 4. Generate some notifications
print("Generating notifications...")
users = list(db.users.find({}, {"id": 1, "role": 1}))
student_users = [u["id"] for u in users if u.get("role") == "STUDENT"]

notifications = []
for student_id in student_users[:50]: # just generate for first 50 students to save time
    notifications.append({
        "_id": str(uuid.uuid4()),
        "user_id": student_id,
        "title": "Nhắc nhở điểm danh",
        "message": "Bạn có một lịch học sắp diễn ra, vui lòng chuẩn bị.",
        "type": "SYSTEM",
        "is_read": False,
        "created_at": base_time - random.randint(1000, 100000)
    })

if notifications:
    db.notifications.insert_many(notifications)
    print(f"Generated {len(notifications)} notifications.")

print("Done generating mock data based on real data.")
