import pymongo

uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"

def analyze_mismatch():
    client = pymongo.MongoClient(uri)
    db = client['attendance_db']
    
    # Get all distinct class codes in schedules
    schedule_classes = set(db['schedules'].distinct("class_code"))
    print(f"Total unique classes in schedules: {len(schedule_classes)}")
    
    # Get all distinct class codes in enrollments
    enrollment_classes = set(db['enrollments'].distinct("class_code"))
    print(f"Total unique classes in enrollments: {len(enrollment_classes)}")
    
    mismatched = enrollment_classes - schedule_classes
    print(f"\nNumber of class_codes in enrollments NOT FOUND in schedules: {len(mismatched)}")
    
    print("\nSample mismatched classes (first 20):")
    for c in list(mismatched)[:20]:
        print(f"'{c}'")

if __name__ == "__main__":
    analyze_mismatch()
