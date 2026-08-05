package server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private final AuthService authService = new AuthService(new EmployeeDirectory());

    @Test
    void validCredentialsLogInSuccessfully() {
        LoginResponse response = authService.login("dana.l", "secret123");

        assertTrue(response.isSuccess());
        assertEquals("Dana Levi", response.getEmployee().getFullName());
    }

    @Test
    void wrongPasswordFails() {
        LoginResponse response = authService.login("dana.l", "wrongpassword");

        assertFalse(response.isSuccess());
        assertNull(response.getEmployee());
    }

    @Test
    void unknownUsernameFails() {
        LoginResponse response = authService.login("nobody", "whatever");

        assertFalse(response.isSuccess());
    }

    @Test
    void secondSimultaneousLoginForSameUserFails() {
        authService.login("dana.l", "secret123");

        LoginResponse response = authService.login("dana.l", "secret123");

        assertFalse(response.isSuccess());
    }

    @Test
    void loginSucceedsAgainAfterLogout() {
        authService.login("dana.l", "secret123");
        authService.logout("dana.l");

        LoginResponse response = authService.login("dana.l", "secret123");

        assertTrue(response.isSuccess());
    }
}
