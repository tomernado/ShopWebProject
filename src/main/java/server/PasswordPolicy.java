package server;

public class PasswordPolicy {
    private static final int MIN_LENGTH = 6;

    public String validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return "Password must be at least " + MIN_LENGTH + " characters";
        }
        if (!containsDigit(password)) {
            return "Password must contain at least one digit";
        }
        return null;
    }

    private boolean containsDigit(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }
}
