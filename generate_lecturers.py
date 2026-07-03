import pymongo
import hashlib
import os
import base64
from datetime import datetime, timezone
import random
from bson.objectid import ObjectId

uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"
client = pymongo.MongoClient(uri)
db = client['attendance_db']

print("Aggregating schedules...")
pipeline = [
    {
        "$group": {
            "_id": "$lecturer_id",
            "lecturer_name": {"$first": "$lecturer_name"}
        }
    }
]

unique_lecturers = list(db.schedules.aggregate(pipeline))

def generate_salt():
    return base64.b64encode(os.urandom(16)).decode('utf-8')

def hash_password(password, salt):
    # Java does: hash(salt + password)
    # where hash is SHA-256 encoded as hex
    data = (salt + password).encode('utf-8')
    return hashlib.sha256(data).hexdigest()

md_content = "# Danh sách Tài khoản Giảng viên\n\n"
md_content += "Tài khoản mặc định được tự động tạo từ dữ liệu lịch học để giảng viên có thể đăng nhập.\n\n"
md_content += "| Mã Giảng Viên (Username) | Họ và Tên | Mật khẩu mặc định |\n"
md_content += "|---------------------------|-----------|-------------------|\n"

print("Loading existing usernames...")
existing_users = set(db.users.distinct("username"))

new_users = []

for s in unique_lecturers:
    lid = s["_id"]
    lname = s.get("lecturer_name", "")
    
    if not lid or not lname: continue
    
    md_content += f"| {lid} | {lname} | {lid} |\n"
    
    # Check if exists
    if lid in existing_users:
        continue
        
    salt = generate_salt()
    pwd_hash = hash_password(lid, salt)
    
    doc_id = str(ObjectId())
    legacy_id = random.randint(1000000, 2147483647)
    
    user_doc = {
        "_id": doc_id,
        "id": legacy_id,
        "username": lid,
        "password_hash": pwd_hash,
        "salt": salt,
        "role": "LECTURER",
        "full_name": lname,
        "student_id": "",
        "email": f"{lid}@gm.uit.edu.vn",
        "device_id": None,
        "session_token": None,
        "created_at": datetime.now(timezone.utc),
        "require_password_change": False
    }
    
    new_users.append(user_doc)

if new_users:
    db.users.insert_many(new_users)
    print(f"Inserted {len(new_users)} new lecturer users.")
else:
    print("No new lecturers to insert.")

artifact_path = "C:\\Users\\ADMIN\\.gemini\\antigravity\\brain\\c405fd90-4b23-4948-995a-f801c635f29d\\lecturer_credentials.md"
with open(artifact_path, "w", encoding="utf-8") as f:
    f.write(md_content)

print(f"Artifact saved to {artifact_path}")
