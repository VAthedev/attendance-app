import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class TestFXML extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student/StudentDashboard.fxml"));
            loader.load();
            System.out.println("SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
    public static void main(String[] args) {
        launch(args);
    }
}
