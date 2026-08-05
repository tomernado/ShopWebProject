package server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuthService authService;

    public ClientHandler(Socket socket, AuthService authService) {
        this.socket = socket;
        this.authService = authService;
    }

    @Override
    public void run() {
        String loggedInUsername = null;
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
                loggedInUsername = request.getUsername();
                waitForDisconnect(in);
            }
        } catch (EOFException e) {
            // client disconnected — normal end of the read loop
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            if (loggedInUsername != null) {
                authService.logout(loggedInUsername);
            }
            closeSocket();
        }
    }

    private void waitForDisconnect(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // Stage 2 only authenticates; later stages will read further message
        // types here on the same connection (e.g. chat in Stage 4).
        while (true) {
            in.readObject();
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
