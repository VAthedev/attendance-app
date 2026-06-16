import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;

public class TestMongo {
    public static void main(String[] args) {
        String uri = "mongodb+srv://lel470959_db_user:H6caFfkP4q4z4ig0@cluster0.tf3itmc.mongodb.net/?appName=Cluster0";
        try (MongoClient client = MongoClients.create(uri)) {
            System.out.println("Testing connection...");
            client.listDatabaseNames().first();
            System.out.println("Connection successful!");
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}
