import pymongo

uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"
client = pymongo.MongoClient(uri)
db = client['attendance_db']

def check_integrity():
    print("--- CHECKING DATA INTEGRITY ---")
    
    # Get all distinct keys
    subjects_codes = set(db['subjects'].distinct('code'))
    subjects_ids = set([str(x) for x in db['subjects'].distinct('_id')])
    schedules_classes = set(db['schedules'].distinct('class_code'))
    schedules_subjects = set(db['schedules'].distinct('subject_code'))
    schedules_lecturers = set(db['schedules'].distinct('lecturer_id'))
    users_ids = set(db['users'].distinct('id'))
    
    # 1. Schedules -> Subjects (by subject_code)
    missing_subjects = schedules_subjects - subjects_codes
    if missing_subjects:
        print(f"[!] Schedules reference {len(missing_subjects)} subject_codes not found in 'subjects' collection.")
        # print(list(missing_subjects)[:10])
    else:
        print("[OK] All subject_codes in 'schedules' exist in 'subjects'.")

    # 2. Schedules -> Users (by lecturer_id)
    missing_lecturers = schedules_lecturers - users_ids
    if missing_lecturers:
        print(f"[!] Schedules reference {len(missing_lecturers)} lecturer_ids not found in 'users' collection.")
    else:
        print("[OK] All lecturer_ids in 'schedules' exist in 'users'.")
        
    # 3. Enrollments -> Schedules (by class_code)
    enrollments_classes = set(db['enrollments'].distinct('class_code'))
    missing_classes = enrollments_classes - schedules_classes
    if missing_classes:
        print(f"[!] Enrollments reference {len(missing_classes)} class_codes not found in 'schedules' collection.")
    else:
        print("[OK] All class_codes in 'enrollments' exist in 'schedules'.")
        
    # 4. Enrollments -> Subjects (by subject_id)
    enrollments_subject_ids = set(db['enrollments'].distinct('subject_id'))
    missing_enrollment_subjects = enrollments_subject_ids - subjects_ids
    if missing_enrollment_subjects:
        print(f"[!] Enrollments reference {len(missing_enrollment_subjects)} subject_ids not found in 'subjects' collection.")
    else:
        print("[OK] All subject_ids in 'enrollments' exist in 'subjects'.")

    # 5. Enrollments -> Users (by student_id)
    enrollments_students = set(db['enrollments'].distinct('student_id'))
    missing_students = enrollments_students - users_ids
    if missing_students:
        print(f"[!] Enrollments reference {len(missing_students)} student_ids not found in 'users' collection.")
    else:
        print("[OK] All student_ids in 'enrollments' exist in 'users'.")

if __name__ == "__main__":
    check_integrity()
