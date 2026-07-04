package database;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

/**
 * ScheduleRepository - truy vấn lịch từ collection `sessions` hoặc `schedules`.
 * Thực hiện các truy vấn cơ bản và trả về danh sách maps để controller dễ dùng.
 */
public class ScheduleRepository {

    private static ScheduleRepository instance;

    private ScheduleRepository() {}

    public static synchronized ScheduleRepository getInstance() {
        if (instance == null) instance = new ScheduleRepository();
        return instance;
    }

    private MongoCollection<Document> sessions() {
        return DatabaseHelper.getInstance().getSessionsCollection();
    }

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Lấy sessions theo ngày (nếu có). Trả về list map với các key nhất quán dùng bởi UI.
     */
    public List<Map<String,Object>> findSessionsByDate(LocalDate date) {
        List<Map<String,Object>> out = new ArrayList<>();
        try {
            String dateIso = date.format(ISO);
            Document filter = new Document("date", dateIso);
            MongoCursor<Document> cursor = sessions().find(filter).iterator();
            try {
                while (cursor.hasNext()) {
                    Document d = cursor.next();
                    out.add(docToMap(d));
                }
            } finally { cursor.close(); }
        } catch (Exception e) {
            // ignore and return empty
        }
        return out;
    }

    /**
     * Lấy sessions trong khoảng ngày [start, end]
     */
    public List<Map<String,Object>> findSessionsInRange(LocalDate start, LocalDate end) {
        List<Map<String,Object>> out = new ArrayList<>();
        try {
            String s = start.format(ISO);
            String e = end.format(ISO);
            Document filter = new Document("date", new Document("$gte", s).append("$lte", e));
            MongoCursor<Document> cursor = sessions().find(filter).iterator();
            try {
                while (cursor.hasNext()) {
                    Document d = cursor.next();
                    out.add(docToMap(d));
                }
            } finally { cursor.close(); }
        } catch (Exception ex) {
            // ignore
        }
        return out;
    }

    /**
     * Lấy subjects (mô tả môn) - truy vấn collection subjects
     */
    public List<Map<String,Object>> findAllSubjects() {
        List<Map<String,Object>> out = new ArrayList<>();
        try {
            MongoCollection<Document> coll = DatabaseHelper.getInstance().getSubjectsCollection();
            MongoCursor<Document> cursor = coll.find().iterator();
            try {
                while (cursor.hasNext()) out.add(docToMap(cursor.next()));
            } finally { cursor.close(); }
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    /**
     * API Endpoint Logic: Lấy lịch học thực tế từ Aggregation Pipeline.
     * JOIN: enrollments -> schedules -> subjects
     */
    public List<Map<String,Object>> findStudentSchedulesByDate(String studentId, LocalDate date) {
        List<Map<String,Object>> out = new ArrayList<>();
        if (studentId == null || studentId.isEmpty()) return out;
        try {
            int dayOfWeek = date.getDayOfWeek().getValue();
            // Map LocalDate dayOfWeek (1=Mon..7=Sun) to String matching DB ("2"=Mon.."8"=Sun)
            String targetDayOfWeek = String.valueOf(dayOfWeek == 7 ? 8 : dayOfWeek + 1);

            MongoCollection<Document> enrollments = DatabaseHelper.getInstance().getEnrollmentsCollection();

            List<org.bson.conversions.Bson> pipeline = java.util.Arrays.asList(
                com.mongodb.client.model.Aggregates.match(com.mongodb.client.model.Filters.eq("student_id", studentId)),
                com.mongodb.client.model.Aggregates.lookup("schedules", "class_code", "class_code", "schedule_details"),
                com.mongodb.client.model.Aggregates.unwind("$schedule_details", new com.mongodb.client.model.UnwindOptions().preserveNullAndEmptyArrays(false)),
                com.mongodb.client.model.Aggregates.lookup("subjects", "subject_code", "code", "subject_details"),
                com.mongodb.client.model.Aggregates.unwind("$subject_details", new com.mongodb.client.model.UnwindOptions().preserveNullAndEmptyArrays(true)),
                com.mongodb.client.model.Aggregates.match(com.mongodb.client.model.Filters.eq("schedule_details.day_of_week", targetDayOfWeek)),
                com.mongodb.client.model.Aggregates.project(com.mongodb.client.model.Projections.fields(
                    com.mongodb.client.model.Projections.excludeId(),
                    com.mongodb.client.model.Projections.computed("subject", "$subject_details.name"),
                    com.mongodb.client.model.Projections.computed("subject_name", "$schedule_details.subject_name"),
                    com.mongodb.client.model.Projections.computed("subject_code", "$schedule_details.subject_code"),
                    com.mongodb.client.model.Projections.computed("lecturer", "$schedule_details.lecturer_name"),
                    com.mongodb.client.model.Projections.computed("room", "$schedule_details.room"),
                    com.mongodb.client.model.Projections.computed("className", "$class_code"),
                    com.mongodb.client.model.Projections.computed("periods", "$schedule_details.periods"),
                    com.mongodb.client.model.Projections.computed("day_of_week", "$schedule_details.day_of_week")
                ))
            );

            MongoCursor<Document> cursor = enrollments.aggregate(pipeline).iterator();
            try {
                while (cursor.hasNext()) {
                    Document d = cursor.next();
                    Map<String,Object> m = new HashMap<>();
                    String subject = d.getString("subject");
                    if (subject == null) subject = d.getString("subject_name");
                    if (subject == null) subject = d.getString("subject_code");
                    m.put("subject", subject);
                    m.put("lecturer", d.getString("lecturer"));
                    m.put("room", d.getString("room"));
                    m.put("className", d.getString("className"));
                    
                    String periods = d.getString("periods");
                    m.put("periods", periods);
                    String[] times = parsePeriods(periods);
                    m.put("startTime", times[0]);
                    m.put("endTime", times[1]);
                    
                    m.put("status", "PENDING"); // Default status for generated schedules
                    m.put("date", date.format(ISO));
                    out.add(m);
                }
            } finally { cursor.close(); }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public List<String> findUniqueClassesByLecturerName(String lecturerName) {
        List<String> classes = new ArrayList<>();
        if (lecturerName == null || lecturerName.isEmpty()) return classes;
        try {
            MongoCollection<Document> schedules = DatabaseHelper.getInstance().getSchedulesCollection();
            List<org.bson.conversions.Bson> pipeline = java.util.Arrays.asList(
                com.mongodb.client.model.Aggregates.match(com.mongodb.client.model.Filters.eq("lecturer_name", lecturerName)),
                com.mongodb.client.model.Aggregates.group(
                    new Document("class_code", "$class_code").append("subject_name", "$subject_name"),
                    com.mongodb.client.model.Accumulators.first("class_code", "$class_code"),
                    com.mongodb.client.model.Accumulators.first("subject_name", "$subject_name")
                ),
                com.mongodb.client.model.Aggregates.sort(com.mongodb.client.model.Sorts.ascending("class_code", "subject_name"))
            );
            com.mongodb.client.MongoCursor<Document> cursor = schedules.aggregate(pipeline).iterator();
            try {
                while (cursor.hasNext()) {
                    Document d = cursor.next();
                    String className = d.getString("class_code");
                    String subjectName = d.getString("subject_name");
                    classes.add(className + " - " + subjectName);
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classes;
    }

    public List<Map<String,Object>> findStudentSchedulesInRange(String studentId, LocalDate start, LocalDate end) {
        List<Map<String,Object>> out = new ArrayList<>();
        if (studentId == null || studentId.isEmpty()) return out;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            out.addAll(findStudentSchedulesByDate(studentId, d));
        }
        return out;
    }

    public List<Map<String,Object>> findLecturerSchedulesByDate(String lecturerName, LocalDate date) {
        List<Map<String,Object>> out = new ArrayList<>();
        if (lecturerName == null || lecturerName.isEmpty()) return out;
        try {
            int dayOfWeek = date.getDayOfWeek().getValue();
            String targetDayOfWeek = String.valueOf(dayOfWeek == 7 ? 8 : dayOfWeek + 1);

            MongoCollection<Document> schedules = DatabaseHelper.getInstance().getSchedulesCollection();

            List<org.bson.conversions.Bson> pipeline = java.util.Arrays.asList(
                com.mongodb.client.model.Aggregates.match(com.mongodb.client.model.Filters.and(
                        com.mongodb.client.model.Filters.eq("lecturer_name", lecturerName),
                        com.mongodb.client.model.Filters.eq("day_of_week", targetDayOfWeek)
                )),
                com.mongodb.client.model.Aggregates.lookup("subjects", "subject_code", "code", "subject_details"),
                com.mongodb.client.model.Aggregates.unwind("$subject_details", new com.mongodb.client.model.UnwindOptions().preserveNullAndEmptyArrays(true)),
                com.mongodb.client.model.Aggregates.project(com.mongodb.client.model.Projections.fields(
                    com.mongodb.client.model.Projections.excludeId(),
                    com.mongodb.client.model.Projections.computed("subject", "$subject_details.name"),
                    com.mongodb.client.model.Projections.computed("subject_name", "$subject_name"),
                    com.mongodb.client.model.Projections.computed("subject_code", "$subject_code"),
                    com.mongodb.client.model.Projections.computed("room", "$room"),
                    com.mongodb.client.model.Projections.computed("className", "$class_code"),
                    com.mongodb.client.model.Projections.computed("periods", "$periods"),
                    com.mongodb.client.model.Projections.computed("day_of_week", "$day_of_week")
                ))
            );

            MongoCursor<Document> cursor = schedules.aggregate(pipeline).iterator();
            try {
                while (cursor.hasNext()) {
                    Document d = cursor.next();
                    Map<String,Object> m = new HashMap<>();
                    String subject = d.getString("subject");
                    if (subject == null) subject = d.getString("subject_name");
                    if (subject == null) subject = d.getString("subject_code");
                    m.put("subject", subject);
                    
                    m.put("lecturer", lecturerName);
                    m.put("room", d.getString("room"));
                    
                    String className = d.getString("className");
                    if (className == null) className = d.getString("class");
                    if (className == null) className = d.getString("class_code");
                    m.put("className", className);
                    
                    String periods = d.getString("periods");
                    m.put("periods", periods);
                    String[] times = parsePeriods(periods);
                    m.put("startTime", times[0]);
                    m.put("endTime", times[1]);
                    
                    // For lecturer view, "status" can just indicate UPCOMING, or TODAY
                    m.put("status", date.isBefore(LocalDate.now()) ? "PAST" : (date.isEqual(LocalDate.now()) ? "TODAY" : "UPCOMING")); 
                    m.put("date", date.format(ISO));
                    out.add(m);
                }
            } finally { cursor.close(); }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public List<Map<String,Object>> findLecturerSchedulesInRange(String lecturerName, LocalDate start, LocalDate end) {
        List<Map<String,Object>> out = new ArrayList<>();
        if (lecturerName == null || lecturerName.isEmpty()) return out;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            out.addAll(findLecturerSchedulesByDate(lecturerName, d));
        }
        return out;
    }

    private String[] parsePeriods(String periods) {
        if (periods == null || periods.isEmpty()) return new String[]{"00:00", "00:00"};
        if (periods.contains("1") || periods.contains("2") || periods.contains("3")) {
            return new String[]{"07:30", "09:10"};
        } else if (periods.contains("4") || periods.contains("5")) {
            return new String[]{"09:30", "11:10"};
        } else if (periods.contains("6") || periods.contains("7") || periods.contains("8")) {
            return new String[]{"13:00", "14:40"};
        } else if (periods.contains("9") || periods.contains("10")) {
            return new String[]{"15:00", "16:40"};
        }
        return new String[]{"07:30", "09:10"};
    }

    private Map<String,Object> docToMap(Document d) {
        Map<String,Object> m = new HashMap<>();
        if (d == null) return m;
        // map known fields with fallbacks
        String subject = d.getString("subject");
        if (subject == null) subject = d.getString("subject_name");
        if (subject == null) subject = d.getString("subject_code");
        m.put("subject", subject);
        
        m.put("startTime", d.getString("start_time") != null ? d.getString("start_time") : d.getString("startTime"));
        m.put("endTime", d.getString("end_time") != null ? d.getString("end_time") : d.getString("endTime"));
        
        String lecturer = d.getString("lecturer");
        if (lecturer == null) lecturer = d.getString("lecturer_name");
        m.put("lecturer", lecturer);
        
        m.put("room", d.getString("room"));
        
        String className = d.getString("class");
        if (className == null) className = d.getString("className");
        if (className == null) className = d.getString("class_code");
        m.put("className", className);
        m.put("status", d.getString("status") != null ? d.getString("status") : "PENDING");
        m.put("date", d.getString("date"));
        m.put("attendanceTime", d.getString("attendance_time") != null ? d.getString("attendance_time") : d.getString("attendanceTime"));
        m.put("id", d.getObjectId("_id") != null ? d.getObjectId("_id").toHexString() : null);
        return m;
    }
}
