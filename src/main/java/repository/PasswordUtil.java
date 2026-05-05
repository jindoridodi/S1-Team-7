package repository;

import at.favre.lib.crypto.bcrypt.BCrypt;

public final class PasswordUtil {
    private PasswordUtil() {}

    private static final int BCRYPT_COST = 12;

    /**
     * Enforces the application's password policy.
     * Passwords must be at least 8 characters and include at least one
     * uppercase letter, one lowercase letter, and one digit.
     *
     * @param password candidate password
     * @return true when the password meets the policy
     */
    public static boolean isStrongPassword(String password) {
        if (password == null) return false;
        if (password.length() < 8) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (hasUpper && hasLower && hasDigit) return true;
        }
        return false;
    }

    public static String hashPassword(String password) {
        if (password == null) throw new IllegalArgumentException("password is required");
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());
    }

    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) return false;
        return BCrypt.verifyer().verify(password.toCharArray(), storedHash).verified;
    }
}

