import pymongo
import random
from datetime import datetime, timedelta
import time

uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"
client = pymongo.MongoClient(uri)
db = client['attendance_db']

def generate():
    print("Deleting old sessions and attendance...")
    db.sessions.delete_many({})
    db.attendance.delete_many({})
    
    schedules = list(db.schedules.find({}))
    if not schedules:
        print("No schedules found")
        return

    # To optimize, let's load all enrollments and group by class_code
    enrollments = list(db.enrollments.find({}))
    enrollment_map = {} # class_code -> list of student_ids
    for e in enrollments:
        c_code = e.get("class_code")
        if c_code not in enrollment_map:
            enrollment_map[c_code] = []
        enrollment_map[c_code].append(e.get("student_id"))
        
    print(f"Found {len(schedules)} schedules, {len(enrollments)} enrollments.")

    # We want to generate sessions for the last 10 weeks
    now = datetime.now()
    
    total_sessions = 0
    total_attendance = 0
    
    for schedule in schedules:
        subject = schedule.get("subject_code")
        class_name = schedule.get("class_code")
        lecturer_id = schedule.get("lecturer_id", "GV01")
        day_of_week = schedule.get("day_of_week") # "Thứ 2" -> 2
        start_time_str = schedule.get("start_time", "07:30") # e.g. "13:00"
        
        # map day_of_week string to weekday (0=Mon, 6=Sun)
        day_map = {"2": 0, "3": 1, "4": 2, "5": 3, "6": 4, "7": 5, "8": 6, "CN": 6, "1": 6}
        if day_of_week not in day_map:
            # try parsing as int
            try:
                d = int(day_of_week)
                if d == 1 or d == 8: target_weekday = 6
                else: target_weekday = d - 2
            except:
                continue
        else:
            target_weekday = day_map[day_of_week]
        
        students_in_class = enrollment_map.get(class_name, [])
        if not students_in_class:
            continue
            
        # Parse time
        try:
            h, m = map(int, start_time_str.split(':'))
        except:
            h, m = 7, 30
            
        # Generate for last 10 weeks
        for week in range(10):
            days_ago = (7 * week) + 1 # offset to avoid future
            base_date = now - timedelta(days=days_ago)
            
            # find the exact date for this weekday in that week
            # base_date.weekday() gives current weekday. We want target_weekday
            diff = target_weekday - base_date.weekday()
            session_date = base_date + timedelta(days=diff)
            
            # create session doc
            session_start_dt = session_date.replace(hour=h, minute=m, second=0, microsecond=0)
            if session_start_dt > now:
                continue
                
            session_id = f"sess_{int(session_start_dt.timestamp())}_{class_name}"
            
            session_doc = {
                "_id": session_id,
                "lecturer_id": lecturer_id,
                "class_name": class_name,
                "subject": subject,
                "duration": 90,
                "start_time": int(session_start_dt.timestamp() * 1000),
                "end_time": int((session_start_dt + timedelta(minutes=90)).timestamp() * 1000),
                "status": "CLOSED",
                "room": schedule.get("room", "P.Unknown"),
                "gps_enabled": True,
                "wifi_enabled": True
            }
            
            try:
                db.sessions.insert_one(session_doc)
                total_sessions += 1
            except Exception as e:
                pass # ignore duplicates if any
            
            # generate attendance for each student
            attendance_docs = []
            for sid in students_in_class:
                # probabilities: 70% present, 15% absent, 15% late
                rand = random.random()
                if rand < 0.7:
                    status = "PRESENT"
                elif rand < 0.85:
                    status = "ABSENT"
                else:
                    status = "LATE"
                    
                method = random.choice(["GPS", "WiFi", "QR"])
                location = session_doc["room"] if method == "WiFi" else "10.8231, 106.6297"
                
                attendance_docs.append({
                    "student_id": sid,
                    "session_id": session_id,
                    "status": status,
                    "method": method,
                    "location": location,
                    "notes": ""
                })
                
            if attendance_docs:
                try:
                    db.attendance.insert_many(attendance_docs, ordered=False)
                    total_attendance += len(attendance_docs)
                except Exception as e:
                    pass

    print(f"Generated {total_sessions} sessions and {total_attendance} attendance records.")

if __name__ == "__main__":
    generate()
