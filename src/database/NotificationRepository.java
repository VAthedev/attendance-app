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

    public void insertMockDataIfEmpty(String studentId) {
        long count = notificationsCollection.countDocuments(Filters.eq("studentId", studentId));
        if (count == 0) {
            List<Document> mocks = new ArrayList<>();
            mocks.add(createDoc(studentId, "Cảnh báo điểm danh", "Bạn đã vắng mặt 2 buổi môn Cấu trúc dữ liệu. Hãy chú ý đi học đầy đủ để không bị cấm thi.", "ALERT", LocalDateTime.now().minusHours(2)));
            mocks.add(createDoc(studentId, "Thay đổi phòng học", "Môn Lập trình mạng ngày mai sẽ chuyển sang phòng P.305. Vui lòng cập nhật lịch.", "INFO", LocalDateTime.now().minusDays(1)));
            mocks.add(createDoc(studentId, "Tuyệt vời!", "Bạn đã đạt tỷ lệ chuyên cần 100% trong tháng này. Hãy tiếp tục phát huy nhé!", "SUCCESS", LocalDateTime.now().minusDays(3)));
            mocks.add(createDoc(studentId, "Lịch thi giữa kỳ", "Lịch thi giữa kỳ các môn đã được cập nhật trên hệ thống đào tạo.", "INFO", LocalDateTime.now().minusDays(5)));
            mocks.add(createDoc(studentId, "Bảo trì hệ thống", "Hệ thống điểm danh sẽ được bảo trì từ 22:00 đến 24:00 tối nay. Mong bạn thông cảm.", "INFO", LocalDateTime.now().minusDays(10)));
            
            notificationsCollection.insertMany(mocks);
        }
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
