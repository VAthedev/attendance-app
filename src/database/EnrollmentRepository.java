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
}
