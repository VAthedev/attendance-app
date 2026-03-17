package server;

import database.DatabaseHelper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int    PORT        = 9999;
    private static final int    MAX_CLIENTS = 50;

    private ServerSocket        serverSocket;
    private ExecutorService     threadPool;
    private boolean             running = false;

    public Server() {
        threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
    }

    public void start() {
        // Khoi tao DB truoc khi nhan ket noi
        try {
            DatabaseHelper.getInstance().getConnection();
        } catch (Exception e) {
            System.err.println("[Server] Loi khoi tao DB: " + e.getMessage());
            return;
        }

        try {
            serverSocket = new ServerSocket(PORT);
            running      = true;
            System.out.println("[Server] Dang chay tren port " + PORT);
            System.out.println("[Server] Cho ket noi tu client...");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] Client ket noi: "
                    + clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.execute(handler);
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("[Server] Loi: " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            threadPool.shutdown();
            DatabaseHelper.getInstance().close();
            System.out.println("[Server] Da dung.");
        } catch (Exception e) {
            System.err.println("[Server] Loi khi dung: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}
