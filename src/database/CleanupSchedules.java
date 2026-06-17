package database;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CleanupSchedules {

    public static void main(String[] args) {
        System.out.println("Connecting to MongoDB...");
        MongoCollection<Document> schedulesCol = DatabaseHelper.getInstance().getSchedulesCollection();
        
        System.out.println("Fetching schedules...");
        List<Document> schedules = schedulesCol.find().into(new ArrayList<>());
        
        Map<String, List<Document>> grouped = new HashMap<>();
        
        for (Document doc : schedules) {
            String classCode = doc.getString("class_code");
            String lecturerId = doc.getString("lecturer_id");
            String subjectCode = doc.getString("subject_code");
            String dayOfWeek = doc.getString("day_of_week");
            String periods = doc.getString("periods");
            String room = doc.getString("room");
            
            String key = classCode + "_" + lecturerId + "_" + subjectCode + "_" + dayOfWeek + "_" + periods + "_" + room;
            
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(doc);
        }
        
        System.out.println("Total schedules: " + schedules.size());
        System.out.println("Unique groups: " + grouped.size());
        
        int deleted = 0;
        
        for (Map.Entry<String, List<Document>> entry : grouped.entrySet()) {
            List<Document> docs = entry.getValue();
            if (docs.size() > 1) {
                // Delete duplicates
                for (int i = 1; i < docs.size(); i++) {
                    Document doc = docs.get(i);
                    // Handle both ObjectId and String _id depending on how it was inserted
                    Object id = doc.get("_id");
                    if (id instanceof org.bson.types.ObjectId) {
                        schedulesCol.deleteOne(new Document("_id", (org.bson.types.ObjectId) id));
                    } else {
                        schedulesCol.deleteOne(new Document("_id", id.toString()));
                    }
                    deleted++;
                }
                System.out.println("Deleted " + (docs.size() - 1) + " duplicates for group " + entry.getKey());
            }
        }
        
        System.out.println("Cleanup complete. Total deleted: " + deleted);
        System.exit(0);
    }
}
