package client.network;

import protocol.Request;
import protocol.Response;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketClient {

    private static final String HOST = "localhost";
    private static final int PORT = 9999;

    private static SocketClient instance;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected = false;

    public static SocketClient getInstance() {
        if (instance == null)
            instance = new SocketClient();
        return instance;
    }

    private SocketClient() {
    }

    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);// ket noi sever port localhost:9999
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));// Nhan
                                                                                                                // reponse
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);// Gui
                                                                                                                     // request
                                                                                                                     // dang
                                                                                                                     // JSON
            connected = true;
            System.out.println("[Client] Ket noi server thanh cong.");
            return true;
        } catch (IOException e) {
            connected = false;
            System.err.println("[Client] Khong the ket noi server: " + e.getMessage());
            return false;
        }
    }

    public Response send(Request request) {
        if (!connected) {
            if (!connect())
                return Response.error("Khong the ket noi server.");
        }
        try {
            String json = request.toJson();
            System.out.println("[Client] Gui: " + json);
            writer.println(json);
            String responseJson = reader.readLine();
            System.out.println("[Client] Nhan: " + responseJson);
            if (responseJson == null) {
                connected = false;
                return Response.error("Server ngat ket noi.");
            }
            return Response.fromJson(responseJson);
        } catch (IOException e) {
            connected = false;
            return Response.error("Loi mang: " + e.getMessage());
        }
    }

    // bọc thao tác gửi nhận vào một thread riêng để tránh làm đơ giao diện người
    // dùng
    public void sendAsync(Request request, ResponseCallback callback) {
        new Thread(() -> {
            Response res = send(request);
            javafx.application.Platform.runLater(() -> callback.onResponse(res));
        }).start();
    }

    public void disconnect() {
        connected = false;
        try {
            if (reader != null)
                reader.close();
            if (writer != null)
                writer.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            System.err.println("[Client] Loi ngat ket noi: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public interface ResponseCallback {
        void onResponse(Response response);
    }
}
