import pymongo

uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"
client = pymongo.MongoClient(uri)
db = client['attendance_db']
schedules = set(db['schedules'].distinct('class_code'))
enrollments = set(db['enrollments'].distinct('class_code'))
p_classes = [c for c in enrollments if '.P' in c]

for c in p_classes[:10]:
    q_class = c.replace('.P', '.Q')
    print(f'{c} -> {q_class}')
    # Check if exact q_class exists
    if q_class in schedules:
        print('  Exact match found!')
    else:
        # Check if practice variants exist
        variants = [s for s in schedules if s.startswith(q_class + '.')]
        if variants:
            print(f'  Practice variants found: {variants}')
        else:
            print(f'  No matches found in schedules for {q_class}. Available: {[s for s in schedules if s.startswith(c.split(".")[0])] }')
