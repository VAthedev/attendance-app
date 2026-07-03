import pymongo
import json

client = pymongo.MongoClient('mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0')
db = client['attendance_db']

schedules = list(db['schedules'].find({'$or': [{'lecturer_id': None}, {'lecturer_id': ''}, {'lecturer_name': None}, {'lecturer_name': ''}]}))

output = []
for s in schedules:
    output.append({
        'class_code': s.get('class_code'),
        'subject_name': s.get('subject_name'),
        'lecturer_id': s.get('lecturer_id'),
        'lecturer_name': s.get('lecturer_name')
    })

with open('missing_lecturers.json', 'w', encoding='utf-8') as f:
    json.dump(output, f, ensure_ascii=False, indent=2)
