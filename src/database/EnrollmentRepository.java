package database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;

public class EnrollmentRepository {

    private static EnrollmentRepository instance;
    private final MongoCollection<Document> enrollmentsCollection;

    private EnrollmentRepository() {
        this.enrollmentsCollection = DatabaseHelper.getInstance().getEnrollmentsCollection();
    }

    public static synchronized EnrollmentRepository getInstance() {
        if (instance == null) {
            instance = new EnrollmentRepository();
        }
        return instance;
    }

    public long countStudentsByClassCode(String classCode) {
        if (classCode == null || classCode.isEmpty()) return 0;
        return enrollmentsCollection.countDocuments(Filters.eq("class_code", classCode));
    }

    public java.util.List<Document> findStudentsByClassCode(String classCode) {
        java.util.List<Document> out = new java.util.ArrayList<>();
        if (classCode == null || classCode.isEmpty()) return out;
        
        java.util.List<org.bson.conversions.Bson> pipeline = java.util.Arrays.asList(
            com.mongodb.client.model.Aggregates.match(Filters.eq("class_code", classCode)),
            com.mongodb.client.model.Aggregates.lookup("users", "student_id", "code", "student_details"),
            com.mongodb.client.model.Aggregates.unwind("$student_details", new com.mongodb.client.model.UnwindOptions().preserveNullAndEmptyArrays(true))
        );
        
        com.mongodb.client.MongoCursor<Document> cursor = enrollmentsCollection.aggregate(pipeline).iterator();
        try {
            while (cursor.hasNext()) {
                out.add(cursor.next());
            }
        } finally {
            cursor.close();
        }
        return out;
    }
}
