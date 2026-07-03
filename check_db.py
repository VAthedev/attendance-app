import os
from pymongo import MongoClient

uri = os.environ.get("ATTENDANCE_MONGODB_URI", "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0")
client = MongoClient(uri)
db = client.attendance_db

print("--- enrollments for 24520001 ---")
for e in db.enrollments.find({"student_id": "24520001"}):
    print(e)

print("\n--- schedules for 24520001 ---")
# find all subjects the student is enrolled in
enrolled_subjects = [e["subject_id"] for e in db.enrollments.find({"student_id": "24520001"})]
for s in db.schedules.find({"subject_id": {"$in": enrolled_subjects}}):
    print(s)

print("\n--- open sessions ---")
for s in db.sessions.find({"status": "OPEN"}):
    print(s)
