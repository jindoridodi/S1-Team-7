package repository;

import db.DBConnection;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Data access layer for user lifecycle operations.
 *
 * This repository enforces authentication-related constraints such as active account status and derives roles
 * from presence in role tables.
 */
public final class UserRepository {
    private UserRepository() {}

    /**
     * Checks whether an active user account exists for the given email.
     *
     * @param email user email
     * @return true if an active account exists
     */
    public static boolean hasUser(String email) {
        /* Checks for an existing active account to prevent duplicate registrations. */
        String sql = "SELECT 1 FROM Users WHERE Email = ? AND Account_Status = 'active' LIMIT 1";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("hasUser failed", e);
        }
    }

    /**
     * Creates a new user account and associated role rows.
     *
     * Returns null when an active user already exists for the email.
     *
     * @param firstName first name
     * @param lastName last name
     * @param email email address (used as login key)
     * @param sjsuId campus identifier
     * @param gender user gender string as provided by UI
     * @param password plaintext password (validated and hashed)
     * @param roles set containing driver and/or passenger
     * @param licenseNumber required when creating a driver role
     * @return created user model or null if account exists
     */
    public static User createUser(
            String firstName,
            String lastName,
            String email,
            String sjsuId,
            String gender,
            String password,
            Set<String> roles,
            String licenseNumber) {

        if (hasUser(email)) return null;
        if (!PasswordUtil.isStrongPassword(password)) throw new IllegalArgumentException("password does not meet policy");

        /* Creates the base user record as an active account (roles are inserted separately below). */
        String insertUser =
                "INSERT INTO Users (SJSU_ID, First_Name, Last_Name, Email, Gender, " +
                "                   Password_Hash, Account_Status) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'active')";

        try (Connection c = DBConnection.get()) {
            String passwordHash = PasswordUtil.hashPassword(password);

            int userId;
            try (PreparedStatement ps = c.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sjsuId);
                ps.setString(2, firstName);
                ps.setString(3, lastName);
                ps.setString(4, email);
                ps.setString(5, gender);
                ps.setString(6, passwordHash);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No generated key returned");
                    userId = keys.getInt(1);
                }
            }

            if (roles.contains("driver")) {
                try (PreparedStatement pd = c.prepareStatement(
                        /* Adds driver role details; verification is pending until admin approval. */
                        "INSERT INTO Drivers (User_ID, License_Number, Verification_Status, Driver_Rating) VALUES (?, ?, 'pending', 0.0)")) {
                    pd.setInt(1, userId);
                    pd.setString(2, licenseNumber);
                    pd.executeUpdate();
                }
            }
            if (roles.contains("passenger")) {
                try (PreparedStatement pp = c.prepareStatement(
                        /* Adds passenger role details with a fresh ride count. */
                        "INSERT INTO Passengers (User_ID, Total_Rides_Taken) VALUES (?, 0)")) {
                    pp.setInt(1, userId);
                    pp.executeUpdate();
                }
            }

            return new User(firstName, lastName, email, sjsuId, gender, passwordHash, roles);

        } catch (SQLException e) {
            throw new RuntimeException("createUser failed", e);
        }
    }

    /**
     * Authenticates an active account and returns the user profile with derived roles.
     *
     * @param email login email
     * @param password plaintext password
     * @return user if authentication succeeds, otherwise null
     */
    public static User authenticate(String email, String password) {
        /*
         * Loads the active user profile for login and derives roles via LEFT JOINs.
         * Returns null for missing/inactive accounts or when password verification fails.
         */
        String sql =
                "SELECT u.First_Name, u.Last_Name, u.SJSU_ID, u.Gender, u.Password_Hash, " +
                "       CASE WHEN d.User_ID IS NOT NULL THEN 1 ELSE 0 END AS Is_Driver, " +
                "       CASE WHEN p.User_ID IS NOT NULL THEN 1 ELSE 0 END AS Is_Passenger " +
                "FROM Users u " +
                "LEFT JOIN Drivers d ON d.User_ID = u.User_ID " +
                "LEFT JOIN Passengers p ON p.User_ID = u.User_ID " +
                "WHERE u.Email = ? AND u.Account_Status = 'active' LIMIT 1";

        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                String storedHash = rs.getString("Password_Hash");
                if (!PasswordUtil.verifyPassword(password, storedHash)) return null;

                String firstName    = rs.getString("First_Name");
                String lastName     = rs.getString("Last_Name");
                String sjsuId       = rs.getString("SJSU_ID");
                String gender       = rs.getString("Gender");
                boolean isDriver    = rs.getInt("Is_Driver") == 1;
                boolean isPassenger = rs.getInt("Is_Passenger") == 1;

                Set<String> roles = new HashSet<>();
                if (isDriver)    roles.add("driver");
                if (isPassenger) roles.add("passenger");

                return new User(firstName, lastName, email, sjsuId, gender, storedHash, roles);
            }
        } catch (SQLException e) {
            throw new RuntimeException("authenticate failed", e);
        }
    }

    /**
     * Soft-deletes a user account by switching account status.
     *
     * @param email email of the account to delete
     */
    public static void deleteUser(String email) {
        /* Soft-deletes the account so historical references remain valid. */
        String sql = "UPDATE Users SET Account_Status = 'deleted' WHERE Email = ?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteUser failed", e);
        }
    }

    /**
     * Updates the password for an active account.
     *
     * @param email account email
     * @param newPassword plaintext password (validated and hashed)
     * @return true if the password is updated
     */
    public static boolean updatePassword(String email, String newPassword) {
        if (!PasswordUtil.isStrongPassword(newPassword)) return false;
        /* Updates password only for active accounts to avoid reactivating soft-deleted users. */
        String sql = "UPDATE Users SET Password_Hash = ? WHERE Email = ? AND Account_Status = 'active'";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hashPassword(newPassword));
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("updatePassword failed", e);
        }
    }

    /**
     * Returns the driver's verification status for the active account.
     *
     * @param email authenticated user email
     * @return verification status string or null if not a driver
     */
    public static String getDriverVerificationStatus(String email) {
        /* Reads the driver verification gate used to allow ride/vehicle management actions. */
        String sql =
                "SELECT d.Verification_Status FROM Drivers d " +
                "JOIN Users u ON u.User_ID = d.User_ID " +
                "WHERE u.Email = ? AND u.Account_Status = 'active' LIMIT 1";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("Verification_Status") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("getDriverVerificationStatus failed", e);
        }
    }
}

