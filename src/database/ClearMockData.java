package database;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class ClearMockData {
    public static void main(String[] args) {
        System.out.println("Connecting to database...");
        MongoDatabase db = DatabaseHelper.getInstance().getDatabase();
        
        System.out.println("Deleting all attendances...");
        db.getCollection("attendances").deleteMany(new Document());
        
        System.out.println("Deleting all notifications...");
        db.getCollection("notifications").deleteMany(new Document());
        
        System.out.println("Mock data cleared successfully.");
        System.exit(0);
    }
}
