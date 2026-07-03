import pymongo
import csv
import os

uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"
client = pymongo.MongoClient(uri)
db = client['attendance_db']

enrollments = list(db.enrollments.find({}))
artifact_path = "C:\\Users\\ADMIN\\.gemini\\antigravity\\brain\\c405fd90-4b23-4948-995a-f801c635f29d\\enrollments_export.csv"

if enrollments:
    keys = ["student_id", "subject_code", "subject_id", "class_code", "enrolled_at"]
    with open(artifact_path, "w", newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=keys, extrasaction='ignore')
        writer.writeheader()
        for e in enrollments:
            writer.writerow(e)
    print(f"Exported {len(enrollments)} enrollments to {artifact_path}")
else:
    print("No enrollments found.")
