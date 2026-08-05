package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int port;
    private final AuthService authService;
    private final AccountService accountService;
    private ServerSocket serverSocket;

    public Server(int port, AuthService authService, AccountService accountService) {
        this.port = port;
        this.authService = authService;
        this.accountService = accountService;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, authService, accountService)).start();
            }
        } catch (IOException e) {
            // Expected once stop() closes the server socket while accept() is blocked.
        }
    }

    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // socket already closed
        }
    }
}
