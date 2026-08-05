package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int port;
    private final AuthService authService;
    private final AccountService accountService;
    private final SaleService saleService;
    private ServerSocket serverSocket;

    public Server(int port, AuthService authService, AccountService accountService, SaleService saleService) {
        this.port = port;
        this.authService = authService;
        this.accountService = accountService;
        this.saleService = saleService;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, authService, accountService, saleService)).start();
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
