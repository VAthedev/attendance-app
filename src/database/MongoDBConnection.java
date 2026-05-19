package database;

import com.mongodb.MongoException;
import com.mongodb.MongoClientSettings;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * MongoDBConnection - Quản lý kết nối MongoDB Atlas
 * Đọc cấu hình từ src/config/config.properties
 */
public class MongoDBConnection {

    private static final String CONFIG_FILE = "/config/config.properties";
    private static MongoDBConnection instance;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private Properties properties;

    private MongoDBConnection() {
        loadProperties();
    }

    /**
     * Singleton Pattern - Lấy instance duy nhất
     */
    public static MongoDBConnection getInstance() {
        if (instance == null) {
            synchronized (MongoDBConnection.class) {
                if (instance == null) {
                    instance = new MongoDBConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Load cấu hình từ config.properties
     */
    private void loadProperties() {
        properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("Không tìm thấy file: " + CONFIG_FILE);
            }
            properties.load(input);
            System.out.println("[MongoDB] Config loaded từ: " + CONFIG_FILE);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc config.properties: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy MongoDB Database
     */
    public synchronized MongoDatabase getDatabase() throws MongoException {
        ensureInitialized();
        return database;
    }

    /**
     * Lấy Collection theo tên
     */
    public synchronized MongoCollection<Document> getCollection(String name) {
        ensureInitialized();
        return database.getCollection(name);
    }

    // ===== Collections =====
    public MongoCollection<Document> getUsersCollection()         { return getCollection("users"); }
    public MongoCollection<Document> getSubjectsCollection()      { return getCollection("subjects"); }
    public MongoCollection<Document> getEnrollmentsCollection()   { return getCollection("enrollments"); }
    public MongoCollection<Document> getSchedulesCollection()     { return getCollection("schedules"); }
    public MongoCollection<Document> getSessionsCollection()      { return getCollection("sessions"); }
    public MongoCollection<Document> getAttendanceCollection()    { return getCollection("attendance"); }
    public MongoCollection<Document> getChatMessagesCollection()  { return getCollection("chat_messages"); }
    public MongoCollection<Document> getNotificationsCollection() { return getCollection("notifications"); }
    public MongoCollection<Document> getImportHistoryCollection() { return getCollection("import_history"); }

    /**
     * Khởi tạo kết nối MongoDB nếu chưa có
     */
    private void ensureInitialized() {
        if (mongoClient != null) {
            return;
        }

        try {
            String mongoUri = properties.getProperty("mongodb.uri");
            String dbName = properties.getProperty("mongodb.database.name", "attendance_db");

            if (mongoUri == null || mongoUri.isBlank()) {
                throw new RuntimeException("mongodb.uri không được cấu hình trong config.properties");
            }

            int maxSize = Integer.parseInt(properties.getProperty("mongodb.connection.max.size", "50"));
            int minSize = Integer.parseInt(properties.getProperty("mongodb.connection.min.size", "5"));
            int maxWait = Integer.parseInt(properties.getProperty("mongodb.connection.max.wait.time", "10000"));
            int idleTime = Integer.parseInt(properties.getProperty("mongodb.connection.idle.time", "60000"));

            ConnectionString connectionString = new ConnectionString(mongoUri);
            MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder -> builder
                    .maxSize(maxSize)
                    .minSize(minSize)
                    .maxWaitTime(maxWait, TimeUnit.MILLISECONDS)
                    .maxConnectionIdleTime(idleTime, TimeUnit.MILLISECONDS)
                )
                .build();

            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase(dbName);
            initCollectionsAndIndexes();
            System.out.println("[MongoDB] ✓ Connected to: " + dbName);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi kết nối MongoDB: " + e.getMessage(), e);
        }
    }

    /**
     * Khởi tạo Collections và Indexes
     */
    private void initCollectionsAndIndexes() {
        try {
            // Users collection
            MongoCollection<Document> users = getUsersCollection();
            users.createIndex(new Document("username", 1), new IndexOptions().unique(true));
            users.createIndex(new Document("email", 1), new IndexOptions().unique(true));
            users.createIndex(new Document("session_token", 1));

            // Subjects collection
            getSubjectsCollection().createIndex(new Document("code", 1), new IndexOptions().unique(true));

            // Enrollments collection
            getEnrollmentsCollection().createIndex(
                new Document("student_id", 1).append("subject_id", 1),
                new IndexOptions().unique(true)
            );

            // Schedules collection
            getSchedulesCollection().createIndex(new Document("subject_id", 1));

            // Sessions collection
            getSessionsCollection().createIndex(new Document("schedule_id", 1));

            // Attendance collection
            MongoCollection<Document> attendance = getAttendanceCollection();
            attendance.createIndex(
                new Document("session_id", 1).append("student_id", 1),
                new IndexOptions().unique(true)
            );
            attendance.createIndex(new Document("session_id", 1));
            attendance.createIndex(new Document("student_id", 1));

            // Chat Messages collection
            getChatMessagesCollection().createIndex(new Document("session_id", 1));
            getChatMessagesCollection().createIndex(new Document("created_at", 1));

            // Notifications collection
            getNotificationsCollection().createIndex(new Document("user_id", 1));
            getNotificationsCollection().createIndex(new Document("created_at", 1));

            // Import History collection
            getImportHistoryCollection().createIndex(new Document("import_date", 1));

            System.out.println("[MongoDB] ✓ Indexes initialized");
        } catch (Exception e) {
            System.err.println("[MongoDB] Warning: " + e.getMessage());
        }
    }

    /**
     * Đóng kết nối MongoDB
     */
    public synchronized void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
            System.out.println("[MongoDB] ✓ Connection closed");
        }
    }

    /**
     * Kiểm tra trạng thái kết nối
     */
    public boolean isConnected() {
        return mongoClient != null && database != null;
    }

    /**
     * Lấy thông tin cấu hình (cho debugging)
     */
    public String getConfigInfo() {
        String uri = properties.getProperty("mongodb.uri", "Not set");
        String dbName = properties.getProperty("mongodb.database.name", "attendance_db");
        return String.format("MongoDB URI: %s, Database: %s", uri.substring(0, Math.min(30, uri.length())) + "...", dbName);
    }
}
