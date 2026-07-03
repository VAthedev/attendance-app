import pymongo
from pymongo import MongoClient
from collections import defaultdict

# MongoDB URI
uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"

def clean_duplicate_schedules():
    print("Connecting to MongoDB...")
    client = MongoClient(uri)
    db = client['attendance_db']
    schedules_col = db['schedules']

    print("Fetching all schedules...")
    schedules = list(schedules_col.find())
    
    # Group schedules by a unique key to identify duplicates
    # Key: (class_code, lecturer_id, subject_code, day_of_week, periods, room)
    grouped_schedules = defaultdict(list)
    for doc in schedules:
        key = (
            doc.get('class_code'),
            doc.get('lecturer_id'),
            doc.get('subject_code'),
            doc.get('day_of_week'),
            doc.get('periods'),
            doc.get('room')
        )
        grouped_schedules[key].append(doc)

    print(f"Total schedules found: {len(schedules)}")
    print(f"Unique schedule groups: {len(grouped_schedules)}")
    
    total_deleted = 0
    
    for key, docs in grouped_schedules.items():
        if len(docs) > 1:
            # Sort by created_at or _id to keep the oldest one (or newest)
            # Let's keep the first one and delete the rest
            docs_to_delete = docs[1:]
            for doc in docs_to_delete:
                schedules_col.delete_one({'_id': doc['_id']})
                total_deleted += 1
            print(f"Deleted {len(docs_to_delete)} duplicates for class {key[0]} - Lecturer {key[1]}")

    print(f"Cleanup finished. Total duplicate schedules deleted: {total_deleted}")

if __name__ == "__main__":
    clean_duplicate_schedules()
