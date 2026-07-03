import pymongo
import hashlib
import base64
import os

# Connect to MongoDB
client = pymongo.MongoClient('mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0')
db = client['attendance_db']
users_collection = db['users']

def generate_salt():
    # 16 bytes random salt, base64 encoded
    return base64.b64encode(os.urandom(16)).decode('utf-8')

def hash_with_salt(password, salt):
    # Java SHA256Util: hash(salt + password)
    # returns hex string
    text = salt + password
    m = hashlib.sha256()
    m.update(text.encode('utf-8'))
    return m.hexdigest()

def main():
    # Find all lecturers
    lecturers = users_collection.find({"role": "LECTURER"})
    count = 0
    updated = 0
    
    for lecturer in lecturers:
        count += 1
        student_id = lecturer.get("student_id", "")
        if not student_id:
            # If no student_id, we can't set it as password. Skip or use username?
            # User request: "mật khẩu mặc định cho tất cả các document có role: "LECTURER" là lecturer_id luôn"
            # In this system, student_id is used as lecturer_id.
            student_id = lecturer.get("username", "") # fallback to username just in case
            
        if not student_id:
            print(f"Skipping {lecturer.get('username')}, no ID found.")
            continue
            
        new_salt = generate_salt()
        new_hash = hash_with_salt(student_id, new_salt)
        
        users_collection.update_one(
            {"_id": lecturer["_id"]},
            {"$set": {
                "password_hash": new_hash,
                "salt": new_salt,
                "require_password_change": True
            }}
        )
        updated += 1

    print(f"Found {count} lecturers.")
    print(f"Successfully reset passwords for {updated} lecturers and set require_password_change = True.")

if __name__ == '__main__':
    main()
