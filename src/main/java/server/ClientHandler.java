package server;

import model.Employee;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuthService authService;
    private final AccountService accountService;

    public ClientHandler(Socket socket, AuthService authService, AccountService accountService) {
        this.socket = socket;
        this.authService = authService;
        this.accountService = accountService;
    }

    @Override
    public void run() {
        Employee loggedInEmployee = null;
        try (
                // create the output stream before the input stream on both ends —
                // ObjectInputStream's constructor blocks waiting for the other side's
                // stream header, so mismatched order deadlocks the connection.
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            LoginRequest request = (LoginRequest) in.readObject();
            LoginResponse response = authService.login(request.getUsername(), request.getPassword());
            out.writeObject(response);
            out.flush();

            if (response.isSuccess()) {
                loggedInEmployee = response.getEmployee();
                handleAuthenticatedSession(in, out, loggedInEmployee);
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

    private void handleAuthenticatedSession(ObjectInputStream in, ObjectOutputStream out, Employee employee)
            throws IOException, ClassNotFoundException {
        // Stage 3 adds account-creation requests here; Stage 4 will add chat
        // message types on this same authenticated connection.
        while (true) {
            Object message = in.readObject();
            if (message instanceof CreateAccountRequest request) {
                CreateAccountResponse response = accountService.createAccount(employee.getRole(), request);
                out.writeObject(response);
                out.flush();
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
