
package com.foodordering.servlet;

import com.foodordering.model.User;
import com.foodordering.model.Customer;
import com.foodordering.model.Admin;
import com.foodordering.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import java.security.MessageDigest;
import java.security.SecureRandom;

import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * UserServlet - Module 1: User Management
 *
 * Handles:
 * 1. Customer registration
 * 2. Customer and Admin login
 * 3. Password validation and hashing
 * 4. Session management
 * 5. Logout
 *
 * Compatible with Java 17 and Tomcat 10.1.
 *
 * Uses the existing DBConnection utility.
 */
@WebServlet("/user")
public class UserServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Settings for secure password hashing
    private static final int ITERATIONS = 210000;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH = 256;

    private static final SecureRandom RANDOM =
            new SecureRandom();

    /**
     * Handles POST requests.
     *
     * Expected action values:
     * register, login, logout
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        response.setContentType(
                "text/plain;charset=UTF-8");

        response.setHeader(
                "Cache-Control", "no-store");

        String action = request.getParameter("action");

        if (action == null) {
            sendMessage(response, 400,
                    "Action is required.");
            return;
        }

        switch (action) {

            case "register":
                registerUser(request, response);
                break;

            case "login":
                loginUser(request, response);
                break;

            case "logout":
                logoutUser(request, response);
                break;

            default:
                sendMessage(response, 400,
                        "Invalid action.");
        }
    }

    /**
     * GET is not used for registration,
     * login or logout.
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setHeader(
                "Cache-Control", "no-store");

        sendMessage(response, 405,
                "Use POST for user operations.");
    }

    /**
     * Registers a new CUSTOMER.
     *
     * Public registration cannot create ADMIN users.
     */
    private void registerUser(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        // Check required fields
        if (isBlank(name) ||
                isBlank(email) ||
                isBlank(password)) {

            sendMessage(response, 400,
                    "All fields are required.");
            return;
        }

        name = name.trim();
        email = email.trim();

        // Check limits from the database schema
        if (name.length() > 100 ||
                email.length() > 100) {

            sendMessage(response, 400,
                    "Name or email is too long.");
            return;
        }

        // Validate email format
        if (!email.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

            sendMessage(response, 400,
                    "Invalid email address.");
            return;
        }

        // Validate password strength
        if (!isValidPassword(password)) {

            sendMessage(response, 400,
                    "Password must contain 8 to 72 "
                    + "characters, including uppercase, "
                    + "lowercase, digit and special character.");
            return;
        }

        try {
            // Hash password before database insertion
            String hashedPassword =
                    hashPassword(password);

            // Create a Customer model object
            Customer customer = new Customer(
                    name,
                    email,
                    hashedPassword
            );

            // Exact column names from the users table
            String sql =
                    "INSERT INTO users "
                    + "(name, email, password, role) "
                    + "VALUES (?, ?, ?, ?)";

            // Use the shared DBConnection class
            try (Connection con =
                         DBConnection.getConnection()) {

                if (con == null) {

                    sendMessage(response, 500,
                            "Database connection failed.");
                    return;
                }

                try (PreparedStatement ps =
                             con.prepareStatement(sql)) {

                    ps.setString(
                            1, customer.getName());

                    ps.setString(
                            2, customer.getEmail());

                    ps.setString(
                            3, customer.getPassword());

                    ps.setString(
                            4, customer.getRole());

                    int rows = ps.executeUpdate();

                    if (rows == 1) {

                        sendMessage(response, 201,
                                "Registration successful.");

                    } else {

                        sendMessage(response, 500,
                                "Registration failed.");
                    }
                }
            }

        } catch (SQLIntegrityConstraintViolationException e) {

            // Duplicate email violates UNIQUE constraint
            sendMessage(response, 409,
                    "Email is already registered.");

        } catch (SQLException e) {

            log("Registration database error", e);

            sendMessage(response, 500,
                    "Registration failed due to a "
                    + "database error.");

        } catch (Exception e) {

            log("Registration error", e);

            sendMessage(response, 500,
                    "Registration failed.");
        }
    }

    /**
     * Authenticates CUSTOMER or ADMIN.
     */
    private void loginUser(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (isBlank(email) || isBlank(password)) {

            sendMessage(response, 400,
                    "Email and password are required.");
            return;
        }

        email = email.trim();

        // Query uses existing users table columns
        String sql =
                "SELECT user_id, name, email, "
                + "password, role "
                + "FROM users WHERE email = ?";

        try (Connection con =
                     DBConnection.getConnection()) {

            if (con == null) {

                sendMessage(response, 500,
                        "Database connection failed.");
                return;
            }

            try (PreparedStatement ps =
                         con.prepareStatement(sql)) {

                ps.setString(1, email);

                try (ResultSet rs =
                             ps.executeQuery()) {

                    if (!rs.next()) {

                        sendMessage(response, 401,
                                "Invalid email or password.");
                        return;
                    }

                    String storedPassword =
                            rs.getString("password");

                    // Verify entered password against hash
                    if (!verifyPassword(
                            password, storedPassword)) {

                        sendMessage(response, 401,
                                "Invalid email or password.");
                        return;
                    }

                    int userId =
                            rs.getInt("user_id");

                    String name =
                            rs.getString("name");

                    String userEmail =
                            rs.getString("email");

                    String role =
                            rs.getString("role");

                    User user;

                    // Create correct subclass
                    if ("ADMIN".equals(role)) {

                        user = new Admin(
                                userId,
                                name,
                                userEmail,
                                storedPassword
                        );

                    } else if ("CUSTOMER".equals(role)) {

                        user = new Customer(
                                userId,
                                name,
                                userEmail,
                                storedPassword
                        );

                    } else {

                        sendMessage(response, 403,
                                "User role is not supported.");
                        return;
                    }

                    // Remove previous session, if present
                    HttpSession oldSession =
                            request.getSession(false);

                    if (oldSession != null) {
                        oldSession.invalidate();
                    }

                    // Create a fresh authenticated session
                    HttpSession session =
                            request.getSession(true);

                    session.setAttribute(
                            "userId", user.getUserId());

                    session.setAttribute(
                            "userName", user.getName());

                    session.setAttribute(
                            "userRole", user.getRole());

                    // 30-minute inactivity timeout
                    session.setMaxInactiveInterval(30 * 60);

                    sendMessage(response, 200,
                            "Login successful. Role: "
                            + user.getRole());
                }
            }

        } catch (SQLException e) {

            log("Login database error", e);

            sendMessage(response, 500,
                    "Login failed due to a "
                    + "database error.");

        } catch (Exception e) {

            log("Login error", e);

            sendMessage(response, 500,
                    "Login failed.");
        }
    }

    /**
     * Logs out the currently logged-in user.
     */
    private void logoutUser(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        HttpSession session =
                request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        sendMessage(response, 200,
                "Logout successful.");
    }

    /**
     * Validates password strength.
     *
     * Rules:
     * 8 to 72 characters
     * At least one uppercase letter
     * At least one lowercase letter
     * At least one digit
     * At least one special character
     */
    private boolean isValidPassword(String password) {

        if (password == null ||
                password.length() < 8 ||
                password.length() > 72) {

            return false;
        }

        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;

        for (char ch : password.toCharArray()) {

            if (Character.isUpperCase(ch)) {
                upper = true;

            } else if (Character.isLowerCase(ch)) {
                lower = true;

            } else if (Character.isDigit(ch)) {
                digit = true;

            } else {
                special = true;
            }
        }

        return upper && lower && digit && special;
    }

    /**
     * Creates a salted PBKDF2 password hash.
     *
     * Stored format:
     * iterations:salt:hash
     */
    private String hashPassword(String password)
            throws Exception {

        byte[] salt = new byte[SALT_LENGTH];

        RANDOM.nextBytes(salt);

        byte[] hash = generateHash(
                password,
                salt,
                ITERATIONS
        );

        return ITERATIONS + ":"
                + Base64.getEncoder()
                        .encodeToString(salt)
                + ":"
                + Base64.getEncoder()
                        .encodeToString(hash);
    }

    /**
     * Verifies a password against its stored hash.
     */
    private boolean verifyPassword(
            String password,
            String storedPassword)
            throws Exception {

        if (storedPassword == null) {
            return false;
        }

        String[] parts = storedPassword.split(":");

        if (parts.length != 3) {
            return false;
        }

        int iterations;

        try {
            iterations = Integer.parseInt(parts[0]);

        } catch (NumberFormatException e) {
            return false;
        }

        if (iterations != ITERATIONS) {
            return false;
        }

        byte[] salt;
        byte[] originalHash;

        try {
            salt = Base64.getDecoder()
                    .decode(parts[1]);

            originalHash = Base64.getDecoder()
                    .decode(parts[2]);

        } catch (IllegalArgumentException e) {
            return false;
        }

        if (salt.length != SALT_LENGTH ||
                originalHash.length != KEY_LENGTH / 8) {

            return false;
        }

        byte[] enteredHash = generateHash(
                password,
                salt,
                iterations
        );

        // Compare hashes safely
        return MessageDigest.isEqual(
                originalHash,
                enteredHash
        );
    }

    /**
     * Generates a PBKDF2-HMAC-SHA256 hash.
     *
     * Uses Java 17 built-in cryptography.
     */
    private byte[] generateHash(
            String password,
            byte[] salt,
            int iterations)
            throws Exception {

        char[] characters =
                password.toCharArray();

        PBEKeySpec spec = new PBEKeySpec(
                characters,
                salt,
                iterations,
                KEY_LENGTH
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            return factory.generateSecret(spec)
                    .getEncoded();

        } finally {

            spec.clearPassword();

            Arrays.fill(characters, '\0');
        }
    }

    /**
     * Checks for null, empty or whitespace-only text.
     */
    private boolean isBlank(String value) {

        return value == null ||
                value.trim().isEmpty();
    }

    /**
     * Sends consistent plain-text HTTP responses.
     *
     * The frontend can read:
     * - HTTP status code
     * - Response message
     */
    private void sendMessage(
            HttpServletResponse response,
            int status,
            String message)
            throws IOException {

        response.setStatus(status);

        response.setContentType(
                "text/plain;charset=UTF-8");

        try (PrintWriter out =
                     response.getWriter()) {

            out.println(message);
        }
    }
}
