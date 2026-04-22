package database;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import model.User;
import org.bson.Document;
import org.bson.types.ObjectId;
import security.SHA256Util;

import java.sql.SQLException;
import java.util.Date;

public class UserRepository {

    private final MongoCollection<Document> usersCollection;

    public UserRepository() throws SQLException {
        try {
            this.usersCollection = DatabaseHelper.getInstance().getUsersCollection();
        } catch (MongoException e) {
            throw new SQLException("Khong the ket noi MongoDB: " + e.getMessage(), e);
        }
    }

    // Dang ky user moi - nhan password thô, tu hash ben trong
    public boolean register(String username, String password, String role,
                            String fullName, String studentId, String email) throws SQLException {
        String salt = SHA256Util.generateSalt();
        String hash = SHA256Util.hashWithSalt(password, salt);
        String objectId = new ObjectId().toHexString();

        Document userDoc = new Document("_id", objectId)
            .append("id", toLegacyIntId(objectId))
            .append("username", username)
            .append("password_hash", hash)
            .append("salt", salt)
            .append("role", role)
            .append("full_name", fullName)
            .append("student_id", studentId)
            .append("email", email)
            .append("device_id", null)
            .append("session_token", null)
            .append("created_at", new Date());

        try {
            usersCollection.insertOne(userDoc);
            return true;
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new SQLException("Ten dang nhap hoac email da ton tai.");
            }
            throw new SQLException("Loi ghi du lieu: " + e.getMessage(), e);
        } catch (MongoException e) {
            throw new SQLException("Loi MongoDB: " + e.getMessage(), e);
        }
    }

    // Dang nhap - nhan password thô, lay salt tu DB roi verify
    public User login(String username, String password) throws SQLException {
        try {
            Document userDoc = usersCollection.find(Filters.eq("username", username)).first();
            if (userDoc != null) {
                String salt = userDoc.getString("salt");
                String storedHash = userDoc.getString("password_hash");

                if (SHA256Util.verify(password, salt, storedHash)) {
                    return mapUser(userDoc);
                }
            }
            return null;
        } catch (MongoException e) {
            throw new SQLException("Loi MongoDB: " + e.getMessage(), e);
        }
    }

    public void saveSessionToken(int userId, String token) throws SQLException {
        try {
            usersCollection.updateOne(
                Filters.eq("id", userId),
                new Document("$set", new Document("session_token", token))
            );
        } catch (MongoException e) {
            throw new SQLException("Loi MongoDB: " + e.getMessage(), e);
        }
    }

    public void saveDeviceId(int userId, String deviceId) throws SQLException {
        try {
            usersCollection.updateOne(
                Filters.eq("id", userId),
                new Document("$set", new Document("device_id", deviceId))
            );
        } catch (MongoException e) {
            throw new SQLException("Loi MongoDB: " + e.getMessage(), e);
        }
    }

    public boolean updatePassword(String email, String newPassword) throws SQLException {
        String salt = SHA256Util.generateSalt();
        String hash = SHA256Util.hashWithSalt(newPassword, salt);
        try {
            return usersCollection.updateOne(
                Filters.eq("email", email),
                new Document("$set", new Document("password_hash", hash).append("salt", salt))
            ).getModifiedCount() > 0;
        } catch (MongoException e) {
            throw new SQLException("Loi MongoDB: " + e.getMessage(), e);
        }
    }

    public User findByEmail(String email) throws SQLException {
        try {
            Document userDoc = usersCollection.find(Filters.eq("email", email)).first();
            return userDoc != null ? mapUser(userDoc) : null;
        } catch (MongoException e) {
            throw new SQLException("Loi MongoDB: " + e.getMessage(), e);
        }
    }

    public User findByToken(String token) throws SQLException {
        try {
            Document userDoc = usersCollection.find(Filters.eq("session_token", token)).first();
            return userDoc != null ? mapUser(userDoc) : null;
        } catch (MongoException e) {
            throw new SQLException("Loi MongoDB: " + e.getMessage(), e);
        }
    }

    private User mapUser(Document doc) {
        User u = new User();
        u.setId(extractLegacyIntId(doc));
        u.setUsername(doc.getString("username"));
        u.setRole(doc.getString("role"));
        u.setFullName(doc.getString("full_name"));
        u.setEmail(doc.getString("email"));
        u.setStudentId(doc.getString("student_id"));
        u.setDeviceId(doc.getString("device_id"));
        u.setSessionToken(doc.getString("session_token"));
        return u;
    }

    private int extractLegacyIntId(Document doc) {
        Integer id = doc.getInteger("id");
        if (id != null) {
            return id;
        }

        Object mongoId = doc.get("_id");
        String rawId = mongoId != null ? mongoId.toString() : "0";
        return toLegacyIntId(rawId);
    }

    private int toLegacyIntId(String rawId) {
        return Math.abs(rawId.hashCode());
    }
}
