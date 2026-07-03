import pymongo
import hashlib

uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"

def get_sha256(text):
    return hashlib.sha256(text.encode('utf-8')).hexdigest()

def fill_missing_lecturers():
    client = pymongo.MongoClient(uri)
    db = client['attendance_db']
    schedules_col = db['schedules']
    users_col = db['users']

    # Delete any invalid users that have email=null or username=null (from previous runs)
    print("Cleaning up invalid users...")
    users_col.delete_many({'email': None})
    users_col.delete_many({'username': None})
    users_col.delete_many({'email': {'$exists': False}})
    users_col.delete_many({'username': {'$exists': False}})

    # Find missing lecturers
    query = {'$or': [{'lecturer_id': None}, {'lecturer_id': ''}, {'lecturer_name': None}, {'lecturer_name': ''}]}
    schedules = list(schedules_col.find(query))
    
    # Group by class_code
    missing_by_class = {}
    for s in schedules:
        c = s.get('class_code')
        if c not in missing_by_class:
            missing_by_class[c] = []
        missing_by_class[c].append(s)

    print(f"Total missing schedules: {len(schedules)}")
    print(f"Total unique classes missing lecturer: {len(missing_by_class)}")

    current_id = 90000
    added_users = 0
    updated_schedules = 0

    for class_code, docs in missing_by_class.items():
        # Ensure ID uniqueness in DB
        while True:
            lid = str(current_id)
            if users_col.find_one({'user_id': lid}) is None and users_col.find_one({'username': lid}) is None:
                break
            current_id += 1
            
        lname = f"GV {class_code}"
        
        user_doc = {
            'username': lid,
            'user_id': lid,
            'password': get_sha256(lid),
            'role': 'LECTURER',
            'full_name': lname,
            'email': f"gv{lid}@uit.edu.vn",
            'require_password_change': True
        }
        users_col.insert_one(user_doc)
        added_users += 1

        # Update schedules
        for doc in docs:
            schedules_col.update_one(
                {'_id': doc['_id']},
                {'$set': {
                    'lecturer_id': lid,
                    'lecturer_name': lname
                }}
            )
            updated_schedules += 1
            
        current_id += 1

    print(f"Added {added_users} new users.")
    print(f"Updated {updated_schedules} schedules.")

if __name__ == "__main__":
    fill_missing_lecturers()
