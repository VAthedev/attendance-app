import pymongo
import random

uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"
client = pymongo.MongoClient(uri)
db = client['attendance_db']
schedules = set(db['schedules'].distinct('class_code'))
enrollments = set(db['enrollments'].distinct('class_code'))
p_classes = [c for c in enrollments if '.P' in c]

mappings = {}
for c in p_classes:
    subject = c.split('.')[0]
    subject_schedules = [s for s in schedules if s.startswith(subject)]
    
    if not subject_schedules:
        print(f"{c} -> NO SCHEDULES FOR {subject}")
        continue
        
    # Check if there are practice classes (.1, .2, etc)
    practice_classes = [s for s in subject_schedules if '.' in s.split('Q')[1]]
    
    if practice_classes:
        chosen = random.choice(practice_classes)
        print(f"{c} -> {chosen} (Random practice class)")
    else:
        chosen = random.choice(subject_schedules)
        print(f"{c} -> {chosen} (Random regular class)")
