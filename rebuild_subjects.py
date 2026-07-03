import pymongo
from datetime import datetime, timezone

uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0"
client = pymongo.MongoClient(uri)
db = client['attendance_db']

# ---------------------------------------------------------------------------
# FIX TC-SYNC-001: Zero-Downtime Atomic Rebuild
# ---------------------------------------------------------------------------
# TRƯỚC (nguy hiểm): delete_many({}) tạo khoảng trắng → Java App crash
#   db.subjects.delete_many({})      # <-- Collection trống tạm thời!
#   db.subjects.insert_many(...)
#
# SAU (an toàn): ghi vào collection tạm, rename atomic → không bao giờ trống
#   1. Insert vào  subjects_temp
#   2. db.subjects_temp.rename("subjects", dropTarget=True)  ← atomic swap
# ---------------------------------------------------------------------------

TEMP_COLLECTION = "subjects_temp"

def rebuild_subjects():
    """
    Rebuild the 'subjects' collection from 'schedules' data using an atomic
    rename strategy to guarantee zero-downtime for concurrent Java readers.
    """
    print("[rebuild_subjects] Extracting unique subjects from 'schedules'...")

    pipeline = [
        {
            "$group": {
                "_id": "$subject_code",
                "name":             {"$first": "$subject_name"},
                "lecturer_name":    {"$first": "$lecturer_name"},
                "total_credits":    {"$first": "$total_credits"},
                "practice_credits": {"$first": "$practice_credits"},
                "managing_faculty": {"$first": "$managing_faculty"},
            }
        }
    ]

    unique_subjects = list(db.schedules.aggregate(pipeline))

    if not unique_subjects:
        print("[rebuild_subjects] WARNING: No subjects found in 'schedules'. Aborting to prevent data loss.")
        return

    # --- Step 1: Build documents ---
    new_subjects = []
    for s in unique_subjects:
        try:
            total_creds = int(s.get("total_credits", 0) or 0)
        except (ValueError, TypeError):
            total_creds = 0

        try:
            practice_creds = int(s.get("practice_credits", 0) or 0)
        except (ValueError, TypeError):
            practice_creds = 0

        new_subjects.append({
            "_id":              s["_id"],          # subject_code as _id
            "code":             s["_id"],
            "name":             s.get("name", ""),
            "lecturer_name":    s.get("lecturer_name", ""),
            "total_credits":    total_creds,
            "practice_credits": practice_creds,
            "managing_faculty": s.get("managing_faculty", ""),
            "rebuilt_at":       datetime.now(timezone.utc),
        })

    # --- Step 2: Write to temporary collection ---
    print(f"[rebuild_subjects] Writing {len(new_subjects)} subjects to temp collection '{TEMP_COLLECTION}'...")
    
    # Drop the temp collection if it already exists from a previous failed run
    if TEMP_COLLECTION in db.list_collection_names():
        db[TEMP_COLLECTION].drop()
        print(f"[rebuild_subjects] Dropped stale temp collection '{TEMP_COLLECTION}'.")

    db[TEMP_COLLECTION].insert_many(new_subjects)
    print(f"[rebuild_subjects] Inserted {len(new_subjects)} documents into '{TEMP_COLLECTION}'.")

    # --- Step 3: Atomic rename → swap (dropTarget=True drops the old 'subjects') ---
    # Java App readers see either the old 'subjects' or the new one — never an empty state.
    print("[rebuild_subjects] Performing atomic rename: subjects_temp → subjects ...")
    db[TEMP_COLLECTION].rename("subjects", dropTarget=True)

    print(f"[rebuild_subjects] ✅ Successfully rebuilt 'subjects' with {len(new_subjects)} entries. Zero downtime guaranteed.")


if __name__ == "__main__":
    rebuild_subjects()
