package client;

import server.CreateAccountRequest;
import server.CreateAccountResponse;
import server.GetCustomersRequest;
import server.GetCustomersResponse;
import server.GetEmployeesRequest;
import server.GetEmployeesResponse;
import server.GetInventoryRequest;
import server.GetInventoryResponse;
import server.GetSalesReportRequest;
import server.GetSalesReportResponse;
import server.LoginRequest;
import server.LoginResponse;
import server.RecordSaleRequest;
import server.RecordSaleResponse;

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

    public RecordSaleResponse recordSale(RecordSaleRequest request) throws IOException, ClassNotFoundException {
        out.writeObject(request);
        out.flush();
        return (RecordSaleResponse) in.readObject();
    }

    public GetInventoryResponse getInventory() throws IOException, ClassNotFoundException {
        out.writeObject(new GetInventoryRequest());
        out.flush();
        return (GetInventoryResponse) in.readObject();
    }

    public GetCustomersResponse getCustomers() throws IOException, ClassNotFoundException {
        out.writeObject(new GetCustomersRequest());
        out.flush();
        return (GetCustomersResponse) in.readObject();
    }

    public GetEmployeesResponse getEmployees() throws IOException, ClassNotFoundException {
        out.writeObject(new GetEmployeesRequest());
        out.flush();
        return (GetEmployeesResponse) in.readObject();
    }

    public GetSalesReportResponse getSalesReport(String groupBy, boolean todayOnly)
            throws IOException, ClassNotFoundException {
        out.writeObject(new GetSalesReportRequest(groupBy, todayOnly));
        out.flush();
        return (GetSalesReportResponse) in.readObject();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
