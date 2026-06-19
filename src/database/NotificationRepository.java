package database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {
    private static NotificationRepository instance;
    private final MongoCollection<Document> notificationsCollection;

    private NotificationRepository() {
        this.notificationsCollection = DatabaseHelper.getInstance().getDatabase().getCollection("notifications");
    }

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    public List<Document> findByStudentId(String studentId) {
        return notificationsCollection.find(Filters.eq("studentId", studentId))
                .sort(Sorts.descending("createdAt"))
                .into(new ArrayList<>());
    }

    public void markAsRead(String notificationId) {
        try {
            notificationsCollection.updateOne(
                    Filters.eq("_id", new ObjectId(notificationId)),
                    Updates.set("isRead", true)
            );
        } catch (IllegalArgumentException e) {
            // In case notificationId is not a valid ObjectId but just a string
            notificationsCollection.updateOne(
                    Filters.eq("_id", notificationId),
                    Updates.set("isRead", true)
            );
        }
    }

    public void markAllAsRead(String studentId) {
        notificationsCollection.updateMany(
                Filters.and(Filters.eq("studentId", studentId), Filters.eq("isRead", false)),
                Updates.set("isRead", true)
            );
    }

    private Document createDoc(String studentId, String title, String message, String type, LocalDateTime time) {
        return new Document("_id", new ObjectId().toHexString())
                .append("studentId", studentId)
                .append("title", title)
                .append("message", message)
                .append("type", type)
                .append("isRead", false)
                .append("createdAt", time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
