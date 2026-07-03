import pymongo, json

c = pymongo.MongoClient('mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0')
db = c['attendance_db']

lecturers = {}
for s in db.schedules.find():
    lid = s.get('lecturer_id')
    lname = s.get('lecturer_name')
    class_code = s.get('class_code')
    subject_name = s.get('subject_name')
    if class_code and subject_name:
        subj = f"{class_code} - {subject_name}"
    else:
        subj = "Không xác định"
        
    if lid and lname:
        if lid not in lecturers:
            lecturers[lid] = {'name': lname, 'subjects': set()}
        if subj != "Không xác định":
            lecturers[lid]['subjects'].add(subj)

with open('C:\\Users\\ADMIN\\.gemini\\antigravity\\brain\\c405fd90-4b23-4948-995a-f801c635f29d\\lecturer_list.md', 'w', encoding='utf-8') as f:
    f.write("# Danh sách Giảng viên và Các môn phụ trách\n\n")
    f.write("| Mã Giảng Viên (Username) | Họ và Tên | Mật khẩu mặc định | Các môn giảng dạy |\n")
    f.write("|---|---|---|---|\n")
    for lid, data in lecturers.items():
        if len(data['subjects']) > 0:
            subs = '<br>'.join(sorted(data['subjects']))
        else:
            subs = "Chưa có"
        f.write(f"| {lid} | {data['name']} | {lid} | {subs} |\n")
