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

import java.util.concurrent.TimeUnit;

public class DatabaseHelper {

    private static final String DATABASE_NAME = "attendance_db";
    private static final String DEFAULT_MONGO_URI =
        "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0";
    private static DatabaseHelper instance;
    private MongoClient mongoClient;
    private MongoDatabase database;

    private DatabaseHelper() {}

    // Singleton
    public static DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }
        return instance;
    }

    // Khoi tao ket noi MongoDB va tao index neu chua co
    public synchronized MongoDatabase getDatabase() throws MongoException {
        ensureInitialized();
        return database;
    }

    public synchronized MongoCollection<Document> getCollection(String name) {
        ensureInitialized();
        return database.getCollection(name);
    }

    public MongoCollection<Document> getUsersCollection()         { return getCollection("users"); }
    public MongoCollection<Document> getSubjectsCollection()      { return getCollection("subjects"); }
    public MongoCollection<Document> getEnrollmentsCollection()   { return getCollection("enrollments"); }
    public MongoCollection<Document> getSchedulesCollection()     { return getCollection("schedules"); }
    public MongoCollection<Document> getSessionsCollection()      { return getCollection("sessions"); }
    public MongoCollection<Document> getAttendanceCollection()    { return getCollection("attendance"); }
    public MongoCollection<Document> getChatMessagesCollection()  { return getCollection("chat_messages"); }
    public MongoCollection<Document> getNotificationsCollection() { return getCollection("notifications"); }

    private void ensureInitialized() {
        if (mongoClient != null) {
            return;
        }

        String uri = System.getenv("ATTENDANCE_MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            uri = DEFAULT_MONGO_URI;
        }

        ConnectionString connectionString = new ConnectionString(uri);
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .applyToConnectionPoolSettings(builder -> builder
                .maxSize(50)
                .minSize(5)
                .maxWaitTime(10, TimeUnit.SECONDS)
                .maxConnectionIdleTime(60, TimeUnit.SECONDS)
            )
            .build();

        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase(DATABASE_NAME);
        initCollectionsAndIndexes();
        System.out.println("[DB] MongoDB connected: " + DATABASE_NAME);
    }

    private void initCollectionsAndIndexes() {
        MongoCollection<Document> users = getUsersCollection();
        users.createIndex(new Document("username", 1), new IndexOptions().unique(true));
        users.createIndex(new Document("email", 1), new IndexOptions().unique(true));
        users.createIndex(new Document("session_token", 1));

        getSubjectsCollection().createIndex(new Document("code", 1), new IndexOptions().unique(true));

        getEnrollmentsCollection().createIndex(
            new Document("student_id", 1).append("subject_id", 1),
            new IndexOptions().unique(true)
        );

        getSchedulesCollection().createIndex(new Document("subject_id", 1));
        getSessionsCollection().createIndex(new Document("schedule_id", 1));

        MongoCollection<Document> attendance = getAttendanceCollection();
        attendance.createIndex(
            new Document("session_id", 1).append("student_id", 1),
            new IndexOptions().unique(true)
        );
        attendance.createIndex(
            new Document("session_id", 1).append("device_id", 1),
            new IndexOptions().unique(true)
        );

        getChatMessagesCollection().createIndex(new Document("subject_id", 1).append("sent_at", -1));
        getNotificationsCollection().createIndex(new Document("user_id", 1).append("created_at", -1));
    }

    // Dong ket noi
    public synchronized void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
            System.out.println("[DB] MongoDB connection closed.");
        }
    }

    // Kiem tra ket noi
    public synchronized boolean isConnected() {
        return mongoClient != null && database != null;
    }
}
