package repository;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Password hashing and verification utilities.
 *
 * This application stores only BCrypt hashes in the database (never plaintext). The cost factor is centralized
 * here so it can be tuned without touching authentication flows.
 */
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

    /**
     * Hashes a password using BCrypt with the configured cost factor.
     *
     * @param password plaintext password (never stored)
     * @return BCrypt hash string suitable for persistence
     * @throws IllegalArgumentException if {@code password} is null
     */
    public static String hashPassword(String password) {
        if (password == null) throw new IllegalArgumentException("password is required");
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());
    }

    /**
     * Verifies a plaintext password against a stored BCrypt hash.
     *
     * @param password candidate plaintext password
     * @param storedHash previously persisted BCrypt hash string
     * @return true when the password matches the hash
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) return false;
        return BCrypt.verifyer().verify(password.toCharArray(), storedHash).verified;
    }
}

