package server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void validPasswordReturnsNull() {
        assertNull(policy.validate("abcdef1"));
    }

    @Test
    void tooShortPasswordIsRejected() {
        assertNotNull(policy.validate("ab1"));
    }

    @Test
    void passwordWithoutDigitIsRejected() {
        assertNotNull(policy.validate("abcdefgh"));
    }
}
