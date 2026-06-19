package database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GenerateAttendancesAndNotifications {

    public static void main(String[] args) {
        try {
            System.out.println("Connecting to database...");
            MongoDatabase db = DatabaseHelper.getInstance().getDatabase();
            MongoCollection<Document> attendances = db.getCollection("attendances");
            MongoCollection<Document> notifications = db.getCollection("notifications");
            MongoCollection<Document> enrollments = db.getCollection("enrollments");
            MongoCollection<Document> schedules = db.getCollection("schedules");

            System.out.println("Deleting all old attendances and notifications...");
            attendances.deleteMany(new Document());
            notifications.deleteMany(new Document());

            System.out.println("Loading data...");
            List<Document> allEnrollments = new ArrayList<>();
            enrollments.find().into(allEnrollments);
            
            List<Document> allSchedules = new ArrayList<>();
            schedules.find().into(allSchedules);
            
            Map<String, Document> schedulesByCode = new HashMap<>();
            for (Document s : allSchedules) {
                String subjCode = s.getString("subject_code");
                String classCode = s.getString("class_code");
                if (subjCode != null && classCode != null) {
                    schedulesByCode.put(subjCode + "_" + classCode, s);
                }
            }

            Random rand = new Random();
            List<Document> attendanceDocs = new ArrayList<>();
            List<Document> notificationDocs = new ArrayList<>();
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            System.out.println("Generating attendances and notifications...");
            
            for (Document enr : allEnrollments) {
                String studentId = enr.getString("student_id");
                String subjectCode = enr.getString("subject_code");
                String classCode = enr.getString("class_code");
                
                Document scheduleDef = schedulesByCode.get(subjectCode + "_" + classCode);
                if (scheduleDef == null) continue;
                
                String scheduleId;
                Object idObj = scheduleDef.get("_id");
                if (idObj instanceof ObjectId) {
                    scheduleId = ((ObjectId) idObj).toHexString();
                } else {
                    scheduleId = idObj.toString();
                }
                String dayOfWeekStr = scheduleDef.getString("day_of_week");
                if ("*".equals(dayOfWeekStr) || dayOfWeekStr == null || dayOfWeekStr.isEmpty()) continue;
                
                int dow = 1;
                try {
                    dow = Integer.parseInt(dayOfWeekStr);
                    if (dow == 8) dow = 7;
                    else dow -= 1;
                } catch (Exception e) {}
                
                String startStr = scheduleDef.getString("start_date");
                String endStr = scheduleDef.getString("end_date");
                if (startStr == null || endStr == null) continue;
                
                LocalDate startDate;
                LocalDate endDate;
                try {
                    startDate = LocalDate.parse(startStr, formatter);
                    endDate = LocalDate.parse(endStr, formatter);
                } catch (Exception e) {
                    continue;
                }
                
                // Truncate to past 4 weeks maximum
                LocalDate simStart = today.minusWeeks(4);
                if (startDate.isBefore(simStart)) startDate = simStart;
                if (endDate.isAfter(today.minusDays(1))) endDate = today.minusDays(1);
                
                int absentCount = 0;
                
                for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                    if (d.getDayOfWeek().getValue() == dow) {
                        boolean attended = rand.nextDouble() < 0.85;
                        if (attended) {
                            Document att = new Document("_id", new ObjectId())
                                    .append("schedule_id", scheduleId)
                                    .append("student_id", studentId)
                                    .append("subject_code", subjectCode)
                                    .append("class_code", classCode)
                                    .append("device_id", UUID.randomUUID().toString())
                                    .append("timestamp", d.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                                    .append("status", "PRESENT");
                            attendanceDocs.add(att);
                        } else {
                            absentCount++;
                        }
                    }
                }
                
                if (absentCount >= 2) {
                    Document notif = new Document("_id", new ObjectId().toHexString())
                            .append("studentId", studentId)
                            .append("title", "Cảnh báo vắng mặt")
                            .append("message", "Bạn đã vắng mặt " + absentCount + " buổi môn học " + subjectCode + ". Hãy chú ý đi học đầy đủ.")
                            .append("type", "ALERT")
                            .append("isRead", false)
                            .append("createdAt", LocalDateTime.now().minusDays(rand.nextInt(3)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                    notificationDocs.add(notif);
                }
            }

            if (!attendanceDocs.isEmpty()) attendances.insertMany(attendanceDocs);
            if (!notificationDocs.isEmpty()) notifications.insertMany(notificationDocs);

            System.out.println("Generated " + attendanceDocs.size() + " attendances.");
            System.out.println("Generated " + notificationDocs.size() + " notifications.");
            System.out.println("Done!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
