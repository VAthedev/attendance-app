package database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class SessionRepository {

    private static SessionRepository instance;
    private final MongoCollection<Document> sessionsCollection;

    private SessionRepository() {
        this.sessionsCollection = DatabaseHelper.getInstance().getSessionsCollection();
    }

    public static synchronized SessionRepository getInstance() {
        if (instance == null) {
            instance = new SessionRepository();
        }
        return instance;
    }

    public String openSession(Document sessionDoc) {
        String id = new ObjectId().toHexString();
        sessionDoc.put("_id", id);
        sessionDoc.put("status", "OPEN");
        sessionsCollection.insertOne(sessionDoc);
        return id;
    }

    public boolean closeSession(String sessionId) {
        Document update = new Document("$set", new Document("status", "CLOSED"));
        return sessionsCollection.updateOne(Filters.eq("_id", sessionId), update).getModifiedCount() > 0;
    }

    public List<Document> findActiveSessions(String lecturerId) {
        long now = System.currentTimeMillis();
        return sessionsCollection.find(
            Filters.and(
                Filters.eq("lecturer_id", lecturerId),
                Filters.eq("status", "OPEN"),
                Filters.gt("end_time", now)
            )
        ).into(new ArrayList<>());
    }
    
    public Document findById(String sessionId) {
        return sessionsCollection.find(Filters.eq("_id", sessionId)).first();
    }
}
