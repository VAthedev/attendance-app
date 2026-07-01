package controller.auth;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

import client.network.SocketClient;
import controller.lecturer.LecturerDashboardController;
import controller.student.StudentDashboardController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import protocol.Request;
import protocol.RequestType;
import util.FxmlUtil;

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
        if (roleGroup == null) {
            roleGroup = new ToggleGroup();
        }
        btnRoleStudent.setToggleGroup(roleGroup);
        btnRoleLecturer.setToggleGroup(roleGroup);
        roleGroup.selectToggle(btnRoleStudent);

        // Keep exactly one role selected and keep prompt text in sync.
        roleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                if (oldToggle != null) {
                    oldToggle.setSelected(true);
                } else {
                    btnRoleStudent.setSelected(true);
                }
                return;
            }
            onRoleChanged();
        });
        onRoleChanged();

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

    private void onRoleChanged() {
        boolean isLecturer = btnRoleLecturer.isSelected();
        txtUsername.setPromptText(isLecturer
            ? "Nhập tên đăng nhập hoặc MSGV"
            : "Nhập tên đăng nhập hoặc MSSV");
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
                SocketClient.getInstance().setCurrentUser(
                        response.getDataValue("userId"),
                        response.getDataValue("fullName"),
                        response.getDataValue("role"));
                String reqChangeStr = response.getDataValue("requirePasswordChange");
                boolean reqChange = "true".equalsIgnoreCase(reqChangeStr);
                if (reqChange) {
                    javafx.application.Platform.runLater(() -> promptPasswordChange(username, response));
                } else {
                    navigateToDashboard(response);
                }
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

    private void promptPasswordChange(String username, protocol.Response loginResponse) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Yêu cầu đổi mật khẩu");
        dialog.setHeaderText("Đây là lần đăng nhập đầu tiên.\nVui lòng đổi mật khẩu để tiếp tục.");
        dialog.setContentText("Mật khẩu mới:");
        dialog.showAndWait().ifPresent(newPw -> {
            if (newPw.trim().isEmpty()) {
                showError("Mật khẩu mới không được để trống!");
                return;
            }
            Request req = new Request(RequestType.RESET_PASSWORD, java.util.Map.of("username", username, "password", newPw));
            SocketClient.getInstance().sendAsync(req, res -> {
                if (res.isOk()) {
                    javafx.application.Platform.runLater(() -> navigateToDashboard(loginResponse));
                } else {
                    javafx.application.Platform.runLater(() -> showError("Lỗi đổi mật khẩu: " + res.getMessage()));
                }
            });
        });
    }

    private void navigateToDashboard(protocol.Response response) {
        String role = response.getDataValue("role");
        String fullName = valueOr(response.getDataValue("fullName"), "");
        String studentId = valueOr(response.getDataValue("studentId"), "");
        String username = valueOr(response.getDataValue("username"), "");
        String token = valueOr(response.getDataValue("token"), "");

        try {
            FXMLLoader loader;
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            if ("LECTURER".equals(role)) {
                loader = FxmlUtil.loader("/fxml/lecturer/LecturerDashboard.fxml");
                stage.setScene(new Scene(loader.load()));
                stage.setTitle("Dashboard");
                LecturerDashboardController controller = loader.getController();
                controller.setUserInfo(fullName, !studentId.isBlank() ? studentId : (!username.isBlank() ? username : String.valueOf(response.getDataValue("userId"))), token);
            } else {
                loader = FxmlUtil.loader("/fxml/student/StudentDashboard.fxml");
                stage.setScene(new Scene(loader.load()));
                stage.setTitle("Dashboard");
                StudentDashboardController controller = loader.getController();
                controller.setUserInfo(fullName, !studentId.isBlank() ? studentId : (!username.isBlank() ? username : String.valueOf(response.getDataValue("userId"))), token);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            showError("Loi: " + e.getMessage() + "\nChi tiet: " + rootCause.getMessage());
        }
    }

    private String valueOr(String value, String fallback) {
        return value != null ? value : fallback;
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = FxmlUtil.loader(fxmlPath);
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
        } catch (Exception e) {
            showError("Loi: " + e.getMessage());
        }
    }
}
