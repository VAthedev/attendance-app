import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TestTime {
    public static void main(String[] args) {
        long t = 1782321985044L;
        System.out.println("Epoch: " + t);
        System.out.println("System time: " + System.currentTimeMillis());
        System.out.println("Default Zone: " + ZoneId.systemDefault());
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(t), ZoneId.systemDefault());
        System.out.println("Local Time from DB: " + ldt);
        System.out.println("Current Local Time: " + LocalDateTime.now());
    }
}
