package server;

import model.Role;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {
    private final EmployeeDirectory employeeDirectory = new EmployeeDirectory();
    private final AccountService accountService = new AccountService(employeeDirectory, new PasswordPolicy());

    @Test
    void managerCanCreateAValidAccount() {
        CreateAccountRequest request = new CreateAccountRequest(
                "100000009", "Roi Biton", "roi.b", "abcdef1", Role.SELLER, "B1");

        CreateAccountResponse response = accountService.createAccount(Role.MANAGER, request);

        assertTrue(response.isSuccess());
        assertNotNull(employeeDirectory.findByUsername("roi.b"));
    }

    @Test
    void nonManagerCannotCreateAnAccount() {
        CreateAccountRequest request = new CreateAccountRequest(
                "100000009", "Roi Biton", "roi.b", "abcdef1", Role.SELLER, "B1");

        CreateAccountResponse response = accountService.createAccount(Role.CASHIER, request);

        assertFalse(response.isSuccess());
        assertNull(employeeDirectory.findByUsername("roi.b"));
    }

    @Test
    void weakPasswordIsRejected() {
        CreateAccountRequest request = new CreateAccountRequest(
                "100000009", "Roi Biton", "roi.b", "weak", Role.SELLER, "B1");

        CreateAccountResponse response = accountService.createAccount(Role.MANAGER, request);

        assertFalse(response.isSuccess());
    }

    @Test
    void duplicateUsernameIsRejected() {
        CreateAccountRequest request = new CreateAccountRequest(
                "100000009", "Second Dana", "dana.l", "abcdef1", Role.SELLER, "B1");

        CreateAccountResponse response = accountService.createAccount(Role.MANAGER, request);

        assertFalse(response.isSuccess());
    }

    @Test
    void unknownBranchIsRejected() {
        CreateAccountRequest request = new CreateAccountRequest(
                "100000009", "Roi Biton", "roi.b", "abcdef1", Role.SELLER, "B9");

        CreateAccountResponse response = accountService.createAccount(Role.MANAGER, request);

        assertFalse(response.isSuccess());
    }
}
