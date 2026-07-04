import pymongo
import os
import json
from bson import ObjectId

class JSONEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, ObjectId):
            return str(o)
        return json.JSONEncoder.default(self, o)

MONGO_URI = os.environ.get(
    "MONGO_URI",
    "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"
)

client = pymongo.MongoClient(MONGO_URI)
db = client["attendance_db"]

print("Users count:", db.users.count_documents({}))
print("Sample user:", json.dumps(db.users.find_one({"role": "STUDENT"}), cls=JSONEncoder))

print("Subjects count:", db.subjects.count_documents({}))
print("Sample subject:", json.dumps(db.subjects.find_one(), cls=JSONEncoder))

print("Schedules count:", db.schedules.count_documents({}))
print("Sample schedule:", json.dumps(db.schedules.find_one(), cls=JSONEncoder))

print("Enrollments count:", db.enrollments.count_documents({}))
print("Sample enrollment:", json.dumps(db.enrollments.find_one(), cls=JSONEncoder))
