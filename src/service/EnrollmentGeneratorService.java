package service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;

import database.MongoDBConnection;

/**
 * EnrollmentGeneratorService - Tạo dữ liệu enrollment ngẫu nhiên cho test
 */
public class EnrollmentGeneratorService {

    private final MongoDBConnection mongoConnection;

    public EnrollmentGeneratorService() {
        this.mongoConnection = MongoDBConnection.getInstance();
    }

    /**
     * Tạo 200 enrollment random
     */
    public void generateRandomEnrollments() {

        MongoCollection<Document> enrollmentsCollection =
                mongoConnection.getDatabase()
                        .getCollection("enrollments");

        String[] subjects = {
                "NT208", "NT106", "CS105",
                "MA003", "SE104", "NT209",
                "CS114", "IT001", "PH001",
                "EN001"
        };

        String[] classes = {
                "P11", "P12", "P13", "P14"
        };

        Random random = new Random();

        List<Document> enrollments = new ArrayList<>();

        for (int i = 1; i <= 200; i++) {

            // Random khóa 23 24 25
            int courseYear;

            int randYear = random.nextInt(3);

            if (randYear == 0) {
                courseYear = 23;
            } else if (randYear == 1) {
                courseYear = 24;
            } else {
                courseYear = 25;
            }

            String studentId =
                    courseYear + "520" +
                    String.format("%04d", i);

            String subject =
                    subjects[random.nextInt(subjects.length)];

            String classCode =
                    subject + "." +
                    classes[random.nextInt(classes.length)];

            Document enrollment =
                    new Document("_id",
                            new ObjectId().toHexString())

                    .append("student_id", studentId)

                    .append("subject_code", subject)

                    .append("class_code", classCode)

                    .append("enrolled_at", new Date());

            enrollments.add(enrollment);
        }

        enrollmentsCollection.insertMany(enrollments);

        System.out.println(
                "✅ Inserted "
                + enrollments.size()
                + " enrollments!"
        );
    }

    /**
     * Xóa tất cả enrollments
     */
    public void clearEnrollments() {
        MongoCollection<Document> enrollmentsCollection =
                mongoConnection.getDatabase()
                        .getCollection("enrollments");

        enrollmentsCollection.deleteMany(new Document());

        System.out.println("✅ Cleared all enrollments");
    }

    /**
     * Lấy số lượng enrollments
     */
    public long countEnrollments() {
        MongoCollection<Document> enrollmentsCollection =
                mongoConnection.getDatabase()
                        .getCollection("enrollments");

        return enrollmentsCollection.countDocuments();
    }
}
