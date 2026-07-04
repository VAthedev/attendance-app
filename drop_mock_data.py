import pymongo
import os

MONGO_URI = os.environ.get(
    "MONGO_URI",
    "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"
)

client = pymongo.MongoClient(MONGO_URI)
db = client["attendance_db"]

r1 = db.users.delete_many({"id": {"$in": ["24520001", "24520002", "24520003", "lecturer_mock_1"]}})
print(f"Deleted {r1.deleted_count} mock users.")

r2 = db.subjects.delete_many({"code": {"$in": ["ENG01", "INT3105", "MM214"]}})
print(f"Deleted {r2.deleted_count} mock subjects.")

r3 = db.schedules.delete_many({"class_code": "CNTT-K65A"})
print(f"Deleted {r3.deleted_count} mock schedules.")

r4 = db.enrollments.delete_many({"class_code": "CNTT-K65A"})
print(f"Deleted {r4.deleted_count} mock enrollments.")

r5 = db.sessions.delete_many({"_id": {"$regex": "^sess_mock_"}})
print(f"Deleted {r5.deleted_count} mock sessions.")

r6 = db.attendance.delete_many({"session_id": {"$regex": "^sess_mock_"}})
r7 = db.attendance.delete_many({"student_id": {"$in": ["24520001", "24520002", "24520003"]}})
print(f"Deleted {r6.deleted_count + r7.deleted_count} mock attendance records.")

print("Successfully deleted all mock data.")
