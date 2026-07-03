package database;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import com.mongodb.client.model.Filters;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class DeleteMockData {
    public static void main(String[] args) {
        MongoDatabase db = DatabaseHelper.getInstance().getDatabase();
        MongoCollection<Document> schedules = db.getCollection("schedules");
        MongoCollection<Document> enrollments = db.getCollection("enrollments");
        MongoCollection<Document> subjects = db.getCollection("subjects");

        // 1. Find all mock schedules (no periods field)
        Set<String> mockClassCodes = new HashSet<>();
        Set<String> mockSubjectCodes = new HashSet<>();
        for (Document d : schedules.find(Filters.exists("periods", false)).into(new ArrayList<>())) {
            if (d.getString("class_code") != null) mockClassCodes.add(d.getString("class_code"));
            if (d.getString("subject_code") != null) mockSubjectCodes.add(d.getString("subject_code"));
        }

        // 2. Delete the mock schedules
        System.out.println("Deleting mock schedules (no periods field)...");
        long delSchedules = schedules.deleteMany(Filters.exists("periods", false)).getDeletedCount();
        System.out.println("Deleted " + delSchedules + " mock schedules.");

        // 3. Delete enrollments for mock classes
        long delEnrollments = 0;
        for (String classCode : mockClassCodes) {
            delEnrollments += enrollments.deleteMany(Filters.eq("class_code", classCode)).getDeletedCount();
        }
        System.out.println("Deleted " + delEnrollments + " enrollments related to mock classes.");

        // 4. Delete mock subjects
        long delSubjects = 0;
        for (String subjCode : mockSubjectCodes) {
            delSubjects += subjects.deleteMany(Filters.eq("code", subjCode)).getDeletedCount();
        }
        System.out.println("Deleted " + delSubjects + " mock subjects.");

        System.exit(0);
    }
}
