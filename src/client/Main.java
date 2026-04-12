package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.FxmlUtil;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = FxmlUtil.loader("/fxml/auth/Login.fxml");
        Scene scene = new Scene(loader.load());
        stage.setTitle("Hệ thống TKB & Điểm danh");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
