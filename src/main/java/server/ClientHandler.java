package server;

import chat.ActiveChatsResponse;
import chat.ChatDispatcher;
import chat.ChatMessage;
import chat.ChatParticipant;
import chat.ChatRequest;
import chat.JoinChatRequest;
import chat.ListActiveChatsRequest;
import model.Employee;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable, ChatParticipant {
    private final Socket socket;
    private final AuthService authService;
    private final AccountService accountService;
    private final SaleService saleService;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Employee loggedInEmployee;

    public ClientHandler(Socket socket, AuthService authService, AccountService accountService, SaleService saleService) {
        this.socket = socket;
        this.authService = authService;
        this.accountService = accountService;
        this.saleService = saleService;
    }

    @Override
    public Employee getEmployee() {
        return loggedInEmployee;
    }

    @Override
    public synchronized void send(Object message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            String username = loggedInEmployee != null ? loggedInEmployee.getUsername() : "unknown";
            System.err.println("Failed to push message to " + username + ": " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            LoginRequest request = (LoginRequest) in.readObject();
            LoginResponse response = authService.login(request.getUsername(), request.getPassword());
            send(response);

            if (response.isSuccess()) {
                loggedInEmployee = response.getEmployee();
                handleAuthenticatedSession();
            }
        } catch (EOFException e) {
            // client disconnected — normal end of the read loop
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            if (loggedInEmployee != null) {
                authService.logout(loggedInEmployee.getUsername());
            }
            closeSocket();
        }
    }

    private void handleAuthenticatedSession() throws IOException, ClassNotFoundException {
        while (true) {
            Object message = in.readObject();
            if (message instanceof CreateAccountRequest request) {
                send(accountService.createAccount(loggedInEmployee.getRole(), request));
            } else if (message instanceof RecordSaleRequest request) {
                send(saleService.recordSale(loggedInEmployee, request));
            } else if (message instanceof ChatRequest) {
                send(ChatDispatcher.getInstance().requestChat(this));
            } else if (message instanceof JoinChatRequest request) {
                send(ChatDispatcher.getInstance().joinChat(request.getSessionId(), this));
            } else if (message instanceof ChatMessage chatMessage) {
                ChatDispatcher.getInstance().sendMessage(chatMessage.getSessionId(), this, chatMessage.getText());
            } else if (message instanceof ListActiveChatsRequest) {
                send(new ActiveChatsResponse(ChatDispatcher.getInstance().listActiveChats()));
            }
        }
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // socket already closed
        }
    }
}
