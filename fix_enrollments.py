import pymongo

def main():
    print("Connecting to MongoDB...")
    client = pymongo.MongoClient('mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0')
    db = client['attendance_db']
    
    col_names = db.list_collection_names()
    col_name = 'enrollment' if 'enrollment' in col_names else 'enrollments'
    if 'enrollment' not in col_names and 'enrollments' not in col_names:
        col_name = 'enrollments'
        
    enrollment_col = db[col_name]

    print("Fetching existing enrollments...")
    all_enrollments = list(enrollment_col.find())
    
    student_records = {}
    for e in all_enrollments:
        s_id = str(e.get('student_id'))
        if s_id not in student_records:
            student_records[s_id] = []
        student_records[s_id].append(e)

    to_delete_ids = []
    
    for s_id, records in student_records.items():
        if len(records) > 6:
            # Sort by enrolled_at so we keep the oldest 6 (original + 5 new ones)
            # Sort by enrolled_at if it exists, else by _id string value
            records.sort(key=lambda x: str(x.get('enrolled_at', str(x['_id']))))
            # The ones to delete are from index 6 onwards
            excess = records[6:]
            for e in excess:
                to_delete_ids.append(e['_id'])

    if to_delete_ids:
        print(f"Found {len(to_delete_ids)} excess enrollments to delete. Deleting now...")
        enrollment_col.delete_many({'_id': {'$in': to_delete_ids}})
        print("Delete completed. Every student now has a maximum of 6 classes.")
    else:
        print("No students have more than 6 classes.")

if __name__ == "__main__":
    main()
