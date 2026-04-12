package controller.auth;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.FxmlUtil;

import java.net.URL;
import java.util.ResourceBundle;

public class ForgotPasswordController implements Initializable {

    // Buoc 1
    @FXML private VBox      paneStep1;
    @FXML private TextField txtEmail;
    @FXML private Label     lblError1;
    @FXML private Button    btnSendOTP;

    // Buoc 2
    @FXML private VBox      paneStep2;
    @FXML private Label     lblOTPSentTo;
    @FXML private TextField otp1, otp2, otp3, otp4, otp5, otp6;
    @FXML private Label     lblCountdown;
    @FXML private Label     lblError2;
    @FXML private Button    btnVerifyOTP;
    @FXML private Hyperlink lnkResend;

    // Buoc 3
    @FXML private VBox          paneStep3;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label         lblError3;
    @FXML private Button        btnResetPassword;

    // Buoc 4 (thanh cong)
    @FXML private VBox paneSuccess;

    private Timeline countdownTimer;
    private int countdownSeconds = 300; // 5 phut

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupOTPFields();
        showStep(1);
    }

    // ===== BUOC 1: GUI OTP =====
    @FXML
    private void handleSendOTP() {
        String email = txtEmail.getText().trim();
        if (email.isEmpty() || !email.contains("@")) {
            lblError1.setText("Email không hợp lệ.");
            return;
        }

        btnSendOTP.setDisable(true);
        btnSendOTP.setText("Đang gửi...");

        // TODO: Gui qua TCP Socket
        // protocol.Request req = new protocol.Request("FORGOT_PASSWORD", Map.of("email", email));
        // client.network.SocketClient.getInstance().send(req, response -> {
        //     if ("OK".equals(response.getStatus())) { goToStep2(email); }
        //     else lblError1.setText(response.getMessage());
        // });

        // Demo tam thoi
        goToStep2(email);
    }

    private void goToStep2(String email) {
        lblOTPSentTo.setText("Mã OTP đã được gửi đến: " + email);
        showStep(2);
        startCountdown();
        btnSendOTP.setDisable(false);
        btnSendOTP.setText("Gửi mã OTP");
    }

    // ===== BUOC 2: XAC NHAN OTP =====
    @FXML
    private void handleVerifyOTP() {
        String otp = otp1.getText() + otp2.getText() + otp3.getText()
                   + otp4.getText() + otp5.getText() + otp6.getText();

        if (otp.length() < 6) {
            lblError2.setText("Vui lòng nhập đủ 6 số.");
            return;
        }

        btnVerifyOTP.setDisable(true);
        btnVerifyOTP.setText("Đang xác nhận...");

        // TODO: Gui qua TCP Socket
        // protocol.Request req = new protocol.Request("VERIFY_OTP", Map.of("otp", otp));
        // ...

        // Demo: OTP la "123456"
        if (otp.equals("123456")) {
            stopCountdown();
            showStep(3);
        } else {
            lblError2.setText("Mã OTP không đúng. Vui lòng thử lại.");
        }
        btnVerifyOTP.setDisable(false);
        btnVerifyOTP.setText("Xác nhận OTP");
    }

    @FXML
    private void handleResendOTP() {
        countdownSeconds = 300;
        startCountdown();
        // TODO: Gui lai OTP qua socket
        lblError2.setText("");
    }

    // ===== BUOC 3: DAT MAT KHAU MOI =====
    @FXML
    private void handleResetPassword() {
        String newPw  = txtNewPassword.getText();
        String confirm = txtConfirmPassword.getText();

        if (newPw.length() < 8) { lblError3.setText("Mật khẩu phải tối thiểu 8 ký tự."); return; }
        if (!newPw.equals(confirm)) { lblError3.setText("Mật khẩu xác nhận không khớp."); return; }

        btnResetPassword.setDisable(true);
        btnResetPassword.setText("Đang cập nhật...");

        // TODO: Gui qua TCP Socket
        // String hashedPw = security.SHA256Util.hash(newPw);
        // protocol.Request req = new protocol.Request("RESET_PASSWORD", Map.of("password", hashedPw));
        // ...

        // Demo tam thoi
        showStep(4);
    }

    @FXML private void goToLogin() { loadScene("/fxml/auth/Login.fxml", "Dang nhap"); }

    // ===== HELPER =====

    private void showStep(int step) {
        paneStep1.setVisible(step == 1);   paneStep1.setManaged(step == 1);
        paneStep2.setVisible(step == 2);   paneStep2.setManaged(step == 2);
        paneStep3.setVisible(step == 3);   paneStep3.setManaged(step == 3);
        paneSuccess.setVisible(step == 4); paneSuccess.setManaged(step == 4);
    }

    private void setupOTPFields() {
        TextField[] fields = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (int i = 0; i < fields.length; i++) {
            final int next = i + 1;
            final TextField current = fields[i];
            final TextField nextField = (next < fields.length) ? fields[next] : null;
            current.textProperty().addListener((obs, ov, nv) -> {
                if (nv.length() > 1) current.setText(nv.substring(0, 1));
                if (nv.length() == 1 && nextField != null) nextField.requestFocus();
            });
        }
    }

    private void startCountdown() {
        stopCountdown();
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            countdownSeconds--;
            int min = countdownSeconds / 60;
            int sec = countdownSeconds % 60;
            lblCountdown.setText(String.format("%02d:%02d", min, sec));
            if (countdownSeconds <= 0) {
                stopCountdown();
                lblCountdown.setText("Hết hạn");
                lnkResend.setDisable(false);
            }
        }));
        countdownTimer.setCycleCount(300);
        countdownTimer.play();
    }

    private void stopCountdown() {
        if (countdownTimer != null) countdownTimer.stop();
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = FxmlUtil.loader(fxmlPath);
            Stage stage = (Stage) btnSendOTP.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
        } catch (Exception e) {
            lblError1.setText("Lỗi điều hướng: " + e.getMessage());
        }
    }
}
