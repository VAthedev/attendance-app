package client;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class CheckData {
    public static void main(String[] args) {
        String uri = System.getenv("ATTENDANCE_MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0";
        }
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("attendance_db");
            
            System.out.println("--- Sessions ---");
            MongoCollection<Document> sessions = database.getCollection("sessions");
            for (Document doc : sessions.find()) {
                System.out.println(doc.toJson());
            }

            System.out.println("\n--- Enrollments for 24520001 ---");
            MongoCollection<Document> enrollments = database.getCollection("enrollments");
            for (Document doc : enrollments.find(new Document("student_id", "24520001"))) {
                System.out.println(doc.toJson());
            }

            System.out.println("\n--- Schedules ---");
            MongoCollection<Document> schedules = database.getCollection("schedules");
            for (Document doc : schedules.find()) {
                System.out.println(doc.toJson());
            }
        }
    }
}
