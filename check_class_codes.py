import pymongo
client = pymongo.MongoClient('mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0')
db = client['attendance_db']
print("Schedules with .P:", list(db['schedules'].find({'class_code': {'$regex': '\.P'}}).limit(5)))
print("Enrollments with .Q:", list(db['enrollments'].find({'class_code': {'$regex': '\.Q'}}).limit(5)))
