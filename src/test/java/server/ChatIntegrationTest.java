package server;

import chat.ChatDispatcher;
import chat.ChatMessage;
import chat.ChatQueued;
import chat.ChatRequest;
import chat.ChatStarted;
import chat.JoinChatRequest;
import chat.JoinChatResponse;
import chat.ParticipantJoined;
import model.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class ChatIntegrationTest {
    private static final int TEST_PORT = 6200;
    private Server server;

    @BeforeEach
    void resetDispatcher() {
        ChatDispatcher.getInstance().resetForTests();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void queuedEmployeeGetsNotifiedWhenADifferentBranchEmployeeRequestsChat() throws Exception {
        startServer();

        try (Socket yossiSocket = new Socket("localhost", TEST_PORT);
             ObjectOutputStream yossiOut = new ObjectOutputStream(yossiSocket.getOutputStream());
             ObjectInputStream yossiIn = new ObjectInputStream(yossiSocket.getInputStream());
             Socket noaSocket = new Socket("localhost", TEST_PORT);
             ObjectOutputStream noaOut = new ObjectOutputStream(noaSocket.getOutputStream());
             ObjectInputStream noaIn = new ObjectInputStream(noaSocket.getInputStream())) {

            login(yossiOut, yossiIn, "yossi.c", "pass456");
            login(noaOut, noaIn, "noa.b", "qwerty789");

            yossiOut.writeObject(new ChatRequest());
            yossiOut.flush();
            assertInstanceOf(ChatQueued.class, yossiIn.readObject());

            noaOut.writeObject(new ChatRequest());
            noaOut.flush();
            Object noaAck = noaIn.readObject();
            assertInstanceOf(ChatStarted.class, noaAck);
            ChatStarted noaStarted = (ChatStarted) noaAck;
            assertEquals("Yossi Cohen", noaStarted.getPeer().getFullName());

            // yossi isn't making a new request — this readObject() receives the
            // unprompted push notifying them a partner became available.
            Object yossiPush = yossiIn.readObject();
            assertInstanceOf(ChatStarted.class, yossiPush);
            ChatStarted yossiStarted = (ChatStarted) yossiPush;
            assertEquals("Noa Biton", yossiStarted.getPeer().getFullName());
            assertEquals(noaStarted.getSessionId(), yossiStarted.getSessionId());
        }
    }

    @Test
    void managerJoinsAndBroadcastsAMessageToBothParticipants() throws Exception {
        startServer();

        try (Socket yossiSocket = new Socket("localhost", TEST_PORT);
             ObjectOutputStream yossiOut = new ObjectOutputStream(yossiSocket.getOutputStream());
             ObjectInputStream yossiIn = new ObjectInputStream(yossiSocket.getInputStream());
             Socket noaSocket = new Socket("localhost", TEST_PORT);
             ObjectOutputStream noaOut = new ObjectOutputStream(noaSocket.getOutputStream());
             ObjectInputStream noaIn = new ObjectInputStream(noaSocket.getInputStream());
             Socket managerSocket = new Socket("localhost", TEST_PORT);
             ObjectOutputStream managerOut = new ObjectOutputStream(managerSocket.getOutputStream());
             ObjectInputStream managerIn = new ObjectInputStream(managerSocket.getInputStream())) {

            login(yossiOut, yossiIn, "yossi.c", "pass456");
            login(noaOut, noaIn, "noa.b", "qwerty789");
            login(managerOut, managerIn, "dana.l", "secret123");

            yossiOut.writeObject(new ChatRequest());
            yossiOut.flush();
            yossiIn.readObject(); // ChatQueued

            noaOut.writeObject(new ChatRequest());
            noaOut.flush();
            ChatStarted started = (ChatStarted) noaIn.readObject();

            yossiIn.readObject(); // the ChatStarted push yossi receives

            managerOut.writeObject(new JoinChatRequest(started.getSessionId()));
            managerOut.flush();
            JoinChatResponse joinResponse = (JoinChatResponse) managerIn.readObject();
            assertTrue(joinResponse.isSuccess());

            assertInstanceOf(ParticipantJoined.class, yossiIn.readObject());
            assertInstanceOf(ParticipantJoined.class, noaIn.readObject());

            managerOut.writeObject(new ChatMessage(started.getSessionId(), null, "שלום לכולם"));
            managerOut.flush();

            ChatMessage yossiMessage = (ChatMessage) yossiIn.readObject();
            assertEquals("שלום לכולם", yossiMessage.getText());
            assertEquals("Dana Levi", yossiMessage.getSender().getFullName());

            ChatMessage noaMessage = (ChatMessage) noaIn.readObject();
            assertEquals("שלום לכולם", noaMessage.getText());
        }
    }

    @Test
    void nonManagerCannotJoinAnExistingChat() throws Exception {
        startServer();

        try (Socket yossiSocket = new Socket("localhost", TEST_PORT);
             ObjectOutputStream yossiOut = new ObjectOutputStream(yossiSocket.getOutputStream());
             ObjectInputStream yossiIn = new ObjectInputStream(yossiSocket.getInputStream());
             Socket noaSocket = new Socket("localhost", TEST_PORT);
             ObjectOutputStream noaOut = new ObjectOutputStream(noaSocket.getOutputStream());
             ObjectInputStream noaIn = new ObjectInputStream(noaSocket.getInputStream());
             Socket managerSocket = new Socket("localhost", TEST_PORT);
             ObjectOutputStream managerOut = new ObjectOutputStream(managerSocket.getOutputStream());
             ObjectInputStream managerIn = new ObjectInputStream(managerSocket.getInputStream());
             Socket cashierSocket = new Socket("localhost", TEST_PORT);
             ObjectOutputStream cashierOut = new ObjectOutputStream(cashierSocket.getOutputStream());
             ObjectInputStream cashierIn = new ObjectInputStream(cashierSocket.getInputStream())) {

            login(yossiOut, yossiIn, "yossi.c", "pass456");
            login(noaOut, noaIn, "noa.b", "qwerty789");
            login(managerOut, managerIn, "dana.l", "secret123");

            managerOut.writeObject(new CreateAccountRequest(
                    "100000010", "Moshe Katz", "050-1000010", "AC-1010", "E-010",
                    "moshe.k", "pw123456", Role.CASHIER, "B1"));
            managerOut.flush();
            CreateAccountResponse createResponse = (CreateAccountResponse) managerIn.readObject();
            assertTrue(createResponse.isSuccess());

            login(cashierOut, cashierIn, "moshe.k", "pw123456");

            yossiOut.writeObject(new ChatRequest());
            yossiOut.flush();
            yossiIn.readObject(); // ChatQueued

            noaOut.writeObject(new ChatRequest());
            noaOut.flush();
            ChatStarted started = (ChatStarted) noaIn.readObject();
            yossiIn.readObject(); // push to yossi

            cashierOut.writeObject(new JoinChatRequest(started.getSessionId()));
            cashierOut.flush();
            JoinChatResponse response = (JoinChatResponse) cashierIn.readObject();

            assertFalse(response.isSuccess());
        }
    }

    private void login(ObjectOutputStream out, ObjectInputStream in, String username, String password)
            throws Exception {
        out.writeObject(new LoginRequest(username, password));
        out.flush();
        LoginResponse response = (LoginResponse) in.readObject();
        assertTrue(response.isSuccess());
    }

    private void startServer() throws InterruptedException {
        EmployeeDirectory employeeDirectory = new EmployeeDirectory();
        AuthService authService = new AuthService(employeeDirectory);
        AccountService accountService = new AccountService(employeeDirectory, new PasswordPolicy());
        SaleService saleService = new SaleService(new ProductCatalog(employeeDirectory));
        server = new Server(TEST_PORT, authService, accountService, saleService);

        Thread serverThread = new Thread(server::start);
        serverThread.start();
        Thread.sleep(200);
    }
}
