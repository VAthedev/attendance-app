package controller.auth;

import client.network.SocketClient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import protocol.Request;
import protocol.RequestType;
import protocol.Response;
import security.SHA256Util;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private ToggleButton  btnRoleStudent;
    @FXML private ToggleButton  btnRoleLecturer;
    @FXML private ToggleGroup   roleGroup;
    @FXML private TextField     txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblError;
    @FXML private Button        btnLogin;
    @FXML private Button        btnShowPass;

    private TextField txtPasswordVisible;
    private boolean   passwordVisible = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        btnRoleStudent.setSelected(true);
        btnRoleStudent.setOnAction(e -> { btnRoleStudent.setSelected(true);  btnRoleLecturer.setSelected(false); });
        btnRoleLecturer.setOnAction(e -> { btnRoleLecturer.setSelected(true); btnRoleStudent.setSelected(false); });

        txtPasswordVisible = new TextField();
        txtPasswordVisible.setPromptText("Nhap mat khau");
        txtPasswordVisible.getStyleClass().add("auth-field");
        txtPasswordVisible.setMaxWidth(Double.MAX_VALUE);
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);

        txtPassword.textProperty().addListener((o, ov, nv) -> { if (!passwordVisible) txtPasswordVisible.setText(nv); });
        txtPasswordVisible.textProperty().addListener((o, ov, nv) -> { if (passwordVisible) txtPassword.setText(nv); });

        txtPassword.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                javafx.application.Platform.runLater(() -> {
                    if (txtPassword.getParent() instanceof javafx.scene.layout.Pane pane) {
                        int idx = pane.getChildren().indexOf(txtPassword);
                        if (!pane.getChildren().contains(txtPasswordVisible))
                            pane.getChildren().add(idx + 1, txtPasswordVisible);
                    }
                });
            }
        });

        txtPassword.setOnAction(e -> handleLogin());
        txtPasswordVisible.setOnAction(e -> handleLogin());
        txtUsername.setOnAction(e -> txtPassword.requestFocus());
        txtUsername.textProperty().addListener((o, ov, nv) -> lblError.setText(""));
        txtPassword.textProperty().addListener((o, ov, nv) -> lblError.setText(""));
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = passwordVisible ? txtPasswordVisible.getText() : txtPassword.getText();

        if (username.isEmpty()) { showError("Vui long nhap ten dang nhap."); return; }
        if (password.isEmpty()) { showError("Vui long nhap mat khau."); return; }

        setLoading(true);

        // QUAN TRONG: KHONG hash o day
        // Server tu lay salt tu DB roi verify
        Request req = new Request(RequestType.LOGIN,
            Map.of("username", username, "password", password));

        SocketClient.getInstance().sendAsync(req, response -> {
            setLoading(false);
            if (response.isOk()) {
                navigateToDashboard(response.getDataValue("role"));
            } else {
                showError(response.getMessage());
            }
        });
    }

    @FXML
    private void toggleShowPassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            txtPasswordVisible.setText(txtPassword.getText());
            txtPassword.setVisible(false); txtPassword.setManaged(false);
            txtPasswordVisible.setVisible(true); txtPasswordVisible.setManaged(true);
            txtPasswordVisible.requestFocus();
            txtPasswordVisible.positionCaret(txtPasswordVisible.getText().length());
            btnShowPass.setText("🙈");
        } else {
            txtPassword.setText(txtPasswordVisible.getText());
            txtPasswordVisible.setVisible(false); txtPasswordVisible.setManaged(false);
            txtPassword.setVisible(true); txtPassword.setManaged(true);
            txtPassword.requestFocus();
            txtPassword.positionCaret(txtPassword.getText().length());
            btnShowPass.setText("👁");
        }
    }

    @FXML private void goToRegister()       { loadScene("/fxml/auth/Register.fxml",       "Dang ky"); }
    @FXML private void goToForgotPassword() { loadScene("/fxml/auth/ForgotPassword.fxml", "Quen mat khau"); }

    private void showError(String msg)  { lblError.setText(msg); }
    private void setLoading(boolean on) { btnLogin.setDisable(on); btnLogin.setText(on ? "Dang dang nhap..." : "Dang nhap"); }

    private void navigateToDashboard(String role) {
        loadScene("LECTURER".equals(role)
            ? "/fxml/lecturer/LecturerDashboard.fxml"
            : "/fxml/student/StudentDashboard.fxml", "Dashboard");
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
        } catch (Exception e) {
            showError("Loi: " + e.getMessage());
        }
    }
}
