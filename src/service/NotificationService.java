package service;

import database.NotificationRepository;
import model.Notification;
import org.bson.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private final NotificationRepository repository;

    public NotificationService() {
        this.repository = NotificationRepository.getInstance();
    }

    public List<Notification> getNotificationsForStudent(String studentId) {
        
        List<Document> docs = repository.findByStudentId(studentId);
        List<Notification> notifications = new ArrayList<>();
        
        for (Document doc : docs) {
            Notification notif = new Notification();
            Object idObj = doc.get("_id");
            notif.setId(idObj != null ? idObj.toString() : null);
            notif.setStudentId(doc.getString("studentId"));
            notif.setTitle(doc.getString("title"));
            notif.setMessage(doc.getString("message"));
            notif.setType(doc.getString("type"));
            
            Boolean isRead = doc.getBoolean("isRead");
            notif.setRead(isRead != null ? isRead : false);
            
            Long createdAtMs = doc.getLong("createdAt");
            if (createdAtMs != null) {
                LocalDateTime date = Instant.ofEpochMilli(createdAtMs)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                notif.setCreatedAt(date);
            }
            
            notifications.add(notif);
        }
        
        return notifications;
    }

    public void markAsRead(String notificationId) {
        repository.markAsRead(notificationId);
    }

    public void markAllAsRead(String studentId) {
        repository.markAllAsRead(studentId);
    }
}
