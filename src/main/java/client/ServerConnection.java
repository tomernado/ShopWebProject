package client;

import server.CreateAccountRequest;
import server.CreateAccountResponse;
import server.LoginRequest;
import server.LoginResponse;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerConnection implements Closeable {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public ServerConnection(String host, int port) throws IOException {
        socket = new Socket(host, port);
        // create the output stream before the input stream on both ends —
        // ObjectInputStream's constructor blocks waiting for the other side's
        // stream header, so mismatched order deadlocks the connection.
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public LoginResponse login(String username, String password) throws IOException, ClassNotFoundException {
        out.writeObject(new LoginRequest(username, password));
        out.flush();
        return (LoginResponse) in.readObject();
    }

    public CreateAccountResponse createAccount(CreateAccountRequest request) throws IOException, ClassNotFoundException {
        out.writeObject(request);
        out.flush();
        return (CreateAccountResponse) in.readObject();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
