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
        Document doc = sessionsCollection.find(Filters.eq("_id", sessionId)).first();
        if (doc == null && ObjectId.isValid(sessionId)) {
            doc = sessionsCollection.find(Filters.eq("_id", new ObjectId(sessionId))).first();
        }
        return doc;
    }

    public List<Document> findAllSessionsByLecturerId(String lecturerId) {
        return sessionsCollection.find(Filters.eq("lecturer_id", lecturerId))
            .sort(new Document("date", -1).append("start_time", -1))
            .into(new ArrayList<>());
    }

    public List<Document> findSessionsByClassCode(String classCode) {
        return sessionsCollection.find(Filters.eq("class_name", classCode))
            .sort(new Document("date", 1).append("start_time", 1))
            .into(new ArrayList<>());
    }
}
