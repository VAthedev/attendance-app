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

    private Map<String,Object> docToMap(Document d) {
        Map<String,Object> m = new HashMap<>();
        if (d == null) return m;
        // map known fields with fallbacks
        m.put("subject", d.getString("subject"));
        m.put("startTime", d.getString("start_time") != null ? d.getString("start_time") : d.getString("startTime"));
        m.put("endTime", d.getString("end_time") != null ? d.getString("end_time") : d.getString("endTime"));
        m.put("lecturer", d.getString("lecturer"));
        m.put("room", d.getString("room"));
        m.put("className", d.getString("class") != null ? d.getString("class") : d.getString("className"));
        m.put("status", d.getString("status") != null ? d.getString("status") : "PENDING");
        m.put("date", d.getString("date"));
        m.put("attendanceTime", d.getString("attendance_time") != null ? d.getString("attendance_time") : d.getString("attendanceTime"));
        m.put("id", d.getObjectId("_id") != null ? d.getObjectId("_id").toHexString() : null);
        return m;
    }
}
