import database.EnrollmentRepository;
import database.NotificationRepository;
import java.util.List;
import org.bson.Document;
public class TestQuery {
    public static void main(String[] args) {
        String className = "MM003.Q22";
        List<Document> students = EnrollmentRepository.getInstance().findStudentsByClassCode(className);
        System.out.println("Students found: " + students.size());
        for (Document d : students) {
            System.out.println(d.getString("student_id"));
        }
    }
}
