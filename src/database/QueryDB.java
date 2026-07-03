package database;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import java.util.ArrayList;

public class QueryDB {
    public static void main(String[] args) {
        MongoDatabase db = DatabaseHelper.getInstance().getDatabase();
        System.out.println("--- SUBJECTS ---");
        for (Document d : db.getCollection("subjects").find().into(new ArrayList<>())) {
            System.out.println(d.toJson());
        }
        System.out.println("--- SCHEDULES ---");
        for (Document d : db.getCollection("schedules").find().into(new ArrayList<>())) {
            System.out.println(d.toJson());
        }
        System.exit(0);
    }
}
