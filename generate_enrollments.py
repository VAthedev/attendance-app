import pandas as pd
import pymongo
import random
from datetime import datetime, timezone
from bson.objectid import ObjectId

def main():
    print("Reading Excel file...")
    df = pd.read_excel('f:\\attendance-app\\TKB.xlsx', header=7)
    df = df.dropna(subset=['MÃ LỚP', 'MÃ MH'])
    classes = []
    subjects_info = {} # code -> name
    for idx, row in df.iterrows():
        subject_code = str(row['MÃ MH']).strip()
        class_code = str(row['MÃ LỚP']).strip()
        subject_name = str(row['TÊN MÔN HỌC']).strip() if 'TÊN MÔN HỌC' in row else subject_code
        if subject_code and class_code and subject_code != 'nan' and class_code != 'nan':
            classes.append((subject_code, class_code))
            subjects_info[subject_code] = subject_name

    classes = list(set(classes))
    num_classes = len(classes)
    print(f"Found {num_classes} unique classes in TKB.")

    print("Connecting to MongoDB...")
    client = pymongo.MongoClient('mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0')
    db = client['attendance_db']
    
    col_names = db.list_collection_names()
    col_name = 'enrollment' if 'enrollment' in col_names else 'enrollments'
    if 'enrollment' not in col_names and 'enrollments' not in col_names:
        col_name = 'enrollments'
        
    enrollment_col = db[col_name]
    subject_col = db['subjects']

    # Upsert subjects to get subject_id
    print("Synchronizing subjects...")
    subject_id_map = {} # subject_code -> ObjectId
    for code, name in subjects_info.items():
        doc = subject_col.find_one_and_update(
            {'code': code},
            {'$set': {'name': name}},
            upsert=True,
            return_document=pymongo.ReturnDocument.AFTER
        )
        subject_id_map[code] = doc['_id']

    print(f"Fetching existing enrollments from {col_name}...")
    existing_enrollments = list(enrollment_col.find())
    print(f"Found {len(existing_enrollments)} existing enrollments.")
    
    students = list(set([str(e['student_id']) for e in existing_enrollments if 'student_id' in e]))
    print(f"Found {len(students)} distinct students.")

    if len(students) == 0:
        students = [f"2452{str(i).zfill(4)}" for i in range(1, 201)]

    class_enrollments = {c: [] for c in classes}
    student_enrollments = {s: [] for s in students}

    # Add existing
    existing_set = set()
    for e in existing_enrollments:
        if 'student_id' in e and 'class_code' in e and 'subject_code' in e:
            s = str(e['student_id'])
            c = (str(e['subject_code']), str(e['class_code']))
            existing_set.add((s, c[0], c[1]))
            if c in classes:
                if s not in student_enrollments:
                    student_enrollments[s] = []
                student_enrollments[s].append(c)
                class_enrollments[c].append(s)
            elif s in student_enrollments:
                student_enrollments[s].append(c)
                
            # If the existing document is missing subject_id, we could update it.
            # But the duplicate error is only if we insert MULTIPLE missing subject_ids for the same student.
            # Since we will provide subject_id for new ones, we won't get conflicts with each other.
            # We might conflict with the existing one if we insert the same subject_id, but the index is student_id + subject_id.

    def can_take(student, clss):
        subj_code = clss[0]
        # Student cannot take the same subject twice in different classes
        student_subjects = [c[0] for c in student_enrollments[student]]
        return subj_code not in student_subjects

    print("Step 1: Filling all classes to have at least 20 students...")
    for clss in classes:
        while len(class_enrollments[clss]) < 20:
            candidates = [s for s in students if can_take(s, clss)]
            if not candidates:
                break
            
            candidates.sort(key=lambda s: len(student_enrollments[s]))
            top_candidates = [c for c in candidates if len(student_enrollments[c]) == len(student_enrollments[candidates[0]])]
            chosen = random.choice(top_candidates)
            
            student_enrollments[chosen].append(clss)
            class_enrollments[clss].append(chosen)

    print("Step 2: Ensuring every student has at least 6 classes...")
    for s in students:
        while len(student_enrollments[s]) < 6:
            candidates = [c for c in classes if can_take(s, c)]
            if not candidates:
                break
            chosen = random.choice(candidates)
            student_enrollments[s].append(chosen)
            class_enrollments[chosen].append(s)

    print("Preparing documents for insertion...")
    new_docs = []
    for s, clsses in student_enrollments.items():
        for (subj, cls) in clsses:
            if (s, subj, cls) not in existing_set:
                subj_id = subject_id_map.get(subj)
                new_docs.append({
                    "student_id": s,
                    "subject_code": subj,
                    "subject_id": subj_id,
                    "class_code": cls,
                    "enrolled_at": datetime.now(timezone.utc)
                })

    if new_docs:
        print(f"Inserting {len(new_docs)} new enrollments...")
        try:
            enrollment_col.insert_many(new_docs, ordered=False)
            print("Insertion complete!")
        except pymongo.errors.BulkWriteError as e:
            print("Completed with some duplicate key errors. Number of successes:", e.details['nInserted'])
    else:
        print("No new enrollments to insert.")

if __name__ == "__main__":
    main()
