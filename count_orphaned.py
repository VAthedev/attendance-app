import pymongo
client = pymongo.MongoClient('mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0')
db = client['attendance_db']
schedule_classes = set(db['schedules'].distinct('class_code'))
enrollment_classes = set(db['enrollments'].distinct('class_code'))
mismatched = list(enrollment_classes - schedule_classes)
orphaned_docs = db['enrollments'].count_documents({'class_code': {'$in': mismatched}})
print(f'Total orphaned enrollment records: {orphaned_docs}')
