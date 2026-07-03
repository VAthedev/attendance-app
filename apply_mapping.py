import pymongo
import random

uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"

def apply_mapping():
    client = pymongo.MongoClient(uri)
    db = client['attendance_db']
    schedules = set(db['schedules'].distinct('class_code'))
    enrollments_col = db['enrollments']
    
    # Get distinct P classes from enrollments
    enrollment_classes = set(enrollments_col.distinct('class_code'))
    p_classes = [c for c in enrollment_classes if '.P' in c]

    updated_count = 0
    skipped_classes = []

    for c in p_classes:
        subject = c.split('.')[0]
        subject_schedules = [s for s in schedules if s.startswith(subject)]
        
        if not subject_schedules:
            skipped_classes.append(c)
            continue
            
        # Check if there are practice classes (.1, .2, etc)
        # Note: We consider it a practice class if it has a period after the Q part (e.g. Q21.1)
        practice_classes = [s for s in subject_schedules if '.' in s.split('Q')[1]]
        
        if practice_classes:
            chosen = random.choice(practice_classes)
        else:
            chosen = random.choice(subject_schedules)
            
        # Update enrollments
        res = enrollments_col.update_many(
            {'class_code': c},
            {'$set': {'class_code': chosen}}
        )
        updated_count += res.modified_count
        print(f"Mapped {c} -> {chosen} ({res.modified_count} enrollments)")

    print(f"\nSuccessfully updated {updated_count} enrollment records.")
    if skipped_classes:
        print(f"Skipped {len(skipped_classes)} classes because they have NO schedules at all:")
        print(skipped_classes)

if __name__ == "__main__":
    apply_mapping()
