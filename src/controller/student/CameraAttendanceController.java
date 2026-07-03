package controller.student;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;
import java.util.UUID;

public class CameraAttendanceController implements Initializable {

    @FXML private ImageView cameraView;
    @FXML private Label lblStatus;
    @FXML private Button btnCapture;

    private Webcam webcam;
    private boolean isRunning = false;
    private Runnable onSuccessCallback;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        new Thread(() -> {
            webcam = Webcam.getDefault();
            if (webcam != null) {
                webcam.setViewSize(WebcamResolution.VGA.getSize());
                webcam.open();
                isRunning = true;
                Platform.runLater(() -> lblStatus.setText("Sẵn sàng chụp ảnh. Vui lòng nhìn thẳng vào camera."));
                startVideoFeed();
            } else {
                Platform.runLater(() -> {
                    lblStatus.setText("Lỗi: Không tìm thấy camera!");
                    btnCapture.setDisable(true);
                });
            }
        }).start();
    }

    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    private void startVideoFeed() {
        new Thread(() -> {
            while (isRunning && webcam.isOpen()) {
                try {
                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        Image fxImage = SwingFXUtils.toFXImage(image, null);
                        Platform.runLater(() -> cameraView.setImage(fxImage));
                    }
                    Thread.sleep(33); // ~30 FPS
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @FXML
    public void handleCapture(ActionEvent event) {
        if (webcam == null || !webcam.isOpen()) return;

        btnCapture.setDisable(true);
        lblStatus.setText("Đang phân tích khuôn mặt...");

        BufferedImage image = webcam.getImage();
        
        new Thread(() -> {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos);
                byte[] imageBytes = baos.toByteArray();

                String boundary = "Boundary-" + UUID.randomUUID().toString();
                String bodyPrefix = "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"capture.jpg\"\r\n" +
                        "Content-Type: image/jpeg\r\n\r\n";
                String bodySuffix = "\r\n--" + boundary + "--\r\n";

                ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
                requestBody.write(bodyPrefix.getBytes(StandardCharsets.UTF_8));
                requestBody.write(imageBytes);
                requestBody.write(bodySuffix.getBytes(StandardCharsets.UTF_8));

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8000/recognize"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody.toByteArray()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    btnCapture.setDisable(false);
                    try {
                        if (response.statusCode() == 200) {
                            JSONObject json = new JSONObject(response.body());
                            if ("success".equals(json.optString("status"))) {
                                String recognizedId = json.optString("id");
                                if (recognizedId.equals(StudentDashboardController.currentStudentId)) {
                                    closeCamera();
                                    if (onSuccessCallback != null) onSuccessCallback.run();
                                    ((Stage) btnCapture.getScene().getWindow()).close();
                                } else {
                                    lblStatus.setText("Khuôn mặt không khớp! (Nhận diện: " + recognizedId + ")");
                                }
                            } else {
                                lblStatus.setText("Lỗi: " + json.optString("message"));
                            }
                        } else {
                            lblStatus.setText("Lỗi kết nối tới AI Server. HTTP " + response.statusCode());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        lblStatus.setText("Lỗi xử lý phản hồi từ AI Server.");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblStatus.setText("Lỗi khi gửi ảnh tới AI Server.");
                    btnCapture.setDisable(false);
                });
            }
        }).start();
    }


    @FXML
    public void handleCancel(ActionEvent event) {
        closeCamera();
        ((Stage) btnCapture.getScene().getWindow()).close();
    }

    private void closeCamera() {
        isRunning = false;
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }
}
