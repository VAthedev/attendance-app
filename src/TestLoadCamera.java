import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class TestLoadCamera extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student/CameraAttendance.fxml"));
            Parent root = loader.load();
            System.out.println("FXML LOADED SUCCESSFULLY");
            primaryStage.setScene(new javafx.scene.Scene(root));
            primaryStage.show();
            System.out.println("STAGE SHOWN. Exiting in 3s...");
            new Thread(() -> {
                try { Thread.sleep(3000); } catch(Exception e){}
                System.exit(0);
            }).start();
        } catch (Throwable t) {
            System.out.println("FAILED TO LOAD:");
            t.printStackTrace();
            System.exit(1);
        }
    }
    public static void main(String[] args) {
        launch(args);
    }
}
