package controller.auth;

import client.network.SocketClient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import protocol.Request;
import protocol.RequestType;
import util.FxmlUtil;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML private ToggleButton  btnRoleStudent;
    @FXML private ToggleButton  btnRoleLecturer;
    @FXML private ToggleGroup   roleGroup;
    @FXML private Label         lblStudentId;
    @FXML private TextField     txtFullName;
    @FXML private TextField     txtStudentId;
    @FXML private TextField     txtUsername;
    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Region        pwBar1, pwBar2, pwBar3, pwBar4;
    @FXML private Label         lblPwStrength;
    @FXML private Label         lblError;
    @FXML private Button        btnRegister;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (roleGroup == null) {
            roleGroup = new ToggleGroup();
        }
        btnRoleStudent.setToggleGroup(roleGroup);
        btnRoleLecturer.setToggleGroup(roleGroup);
        roleGroup.selectToggle(btnRoleStudent);

        // Keep label/prompt synced whenever role selection changes.
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

        // Ensure label/prompt are always in sync with current role at startup.
        onRoleChanged();

        txtPassword.textProperty().addListener((o, ov, nv) -> updatePasswordStrength(nv));
        txtUsername.textProperty().addListener((o, ov, nv) -> lblError.setText(""));
        txtEmail.textProperty().addListener((o, ov, nv) -> lblError.setText(""));
    }

    @FXML
    private void onRoleChanged() {
        boolean isLecturer = btnRoleLecturer.isSelected();
        lblStudentId.setText(isLecturer ? "MSGV" : "MSSV");
        txtStudentId.setPromptText(isLecturer ? "VD: MSGV001" : "VD: 2151234567");
    }

    @FXML
    private void handleRegister() {
        String fullName  = txtFullName.getText().trim();
        String studentId = txtStudentId.getText().trim();
        String username  = txtUsername.getText().trim();
        String email     = txtEmail.getText().trim();
        String password  = txtPassword.getText();
        String confirm   = txtConfirmPassword.getText();
        String role      = btnRoleLecturer.isSelected() ? "LECTURER" : "STUDENT";

        // Validate
        if (fullName.isEmpty())  { showError("Vui long nhap ho ten."); return; }
        if (studentId.isEmpty()) { showError("Vui long nhap MSSV/MSGV."); return; }
        if (username.isEmpty())  { showError("Vui long nhap ten dang nhap."); return; }
        if (email.isEmpty() || !email.contains("@")) { showError("Email khong hop le."); return; }
        if (password.length() < 8) { showError("Mat khau phai toi thieu 8 ky tu."); return; }
        if (!password.equals(confirm)) { showError("Mat khau xac nhan khong khop."); return; }

        btnRegister.setDisable(true);
        btnRegister.setText("Dang tao tai khoan...");

        // Dung HashMap de dam bao dung key
        Map<String, Object> payload = new HashMap<>();
        payload.put("username",  username);
        payload.put("password",  password);  // gui thô, server tu hash
        payload.put("role",      role);
        payload.put("fullName",  fullName);
        payload.put("studentId", studentId);
        payload.put("email",     email);

        Request req = new Request(RequestType.REGISTER, payload);
        SocketClient.getInstance().sendAsync(req, response -> {
            btnRegister.setDisable(false);
            btnRegister.setText("Tao tai khoan");

            if (response.isOk()) {
                goToLogin();
            } else {
                showError(response.getMessage());
            }
        });
    }

    @FXML private void goToLogin() { loadScene("/fxml/auth/Login.fxml", "Dang nhap"); }

    private void updatePasswordStrength(String pw) {
        int score = 0;
        if (pw.length() >= 8)                       score++;
        if (pw.matches(".*[A-Z].*"))                score++;
        if (pw.matches(".*[0-9].*"))                score++;
        if (pw.matches(".*[!@#$%^&*()_+\\-=].*"))  score++;

        String[] barClasses = {"pw-bar-weak","pw-bar-medium","pw-bar-medium","pw-bar-strong"};
        String[] labels     = {"","Yeu","Trung binh","Manh","Rat manh"};
        Region[] bars       = {pwBar1, pwBar2, pwBar3, pwBar4};

        for (int i = 0; i < bars.length; i++) {
            bars[i].getStyleClass().removeAll("pw-bar-weak","pw-bar-medium","pw-bar-strong");
            if (i < score) bars[i].getStyleClass().add(barClasses[Math.min(score-1, 3)]);
        }
        lblPwStrength.setText(score > 0 ? labels[score] : "");
    }

    private void showError(String msg) { lblError.setText(msg); }

    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = FxmlUtil.loader(fxmlPath);
            Stage stage = (Stage) btnRegister.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
        } catch (Exception e) {
            showError("Loi: " + e.getMessage());
        }
    }
}
