import pymongo
import re

uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"

def fix_practice_classes():
    client = pymongo.MongoClient(uri)
    db = client['attendance_db']
    schedules_col = db['schedules']
    enrollments_col = db['enrollments']

    pattern = re.compile(r'\.Q(\d{2})(\d)(.*)')

    # Fix in schedules
    schedule_classes = set(schedules_col.distinct('class_code'))
    updated_schedules = 0
    for c in schedule_classes:
        match = pattern.search(c)
        if match:
            # Reconstruct the string
            # e.g. IT002.Q211 -> IT002.Q21.1
            new_c = re.sub(r'\.Q(\d{2})(\d)(.*)', r'.Q\1.\2\3', c)
            res = schedules_col.update_many(
                {'class_code': c},
                {'$set': {'class_code': new_c}}
            )
            updated_schedules += res.modified_count
            print(f"Schedule: {c} -> {new_c} ({res.modified_count} docs)")

    # Fix in enrollments
    enrollment_classes = set(enrollments_col.distinct('class_code'))
    updated_enrollments = 0
    for c in enrollment_classes:
        match = pattern.search(c)
        if match:
            new_c = re.sub(r'\.Q(\d{2})(\d)(.*)', r'.Q\1.\2\3', c)
            res = enrollments_col.update_many(
                {'class_code': c},
                {'$set': {'class_code': new_c}}
            )
            updated_enrollments += res.modified_count
            print(f"Enrollment: {c} -> {new_c} ({res.modified_count} docs)")

    print(f"\nDone! Updated {updated_schedules} schedule docs and {updated_enrollments} enrollment docs.")

if __name__ == "__main__":
    fix_practice_classes()
