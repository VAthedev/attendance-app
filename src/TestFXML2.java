import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class TestFXML2 extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student/StudentDashboard.fxml"));
            loader.load();
            System.out.println("TEST_SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("TEST_FAIL: " + e.getMessage());
            Throwable cause = e.getCause();
            while(cause != null) {
                System.out.println("CAUSE: " + cause.getMessage());
                cause = cause.getCause();
            }
        }
        System.exit(0);
    }
    public static void main(String[] args) {
        launch(args);
    }
}
