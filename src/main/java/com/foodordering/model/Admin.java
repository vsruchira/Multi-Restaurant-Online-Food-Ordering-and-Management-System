
package com.foodordering.model;

/**
 * Admin model for the Food Ordering System.
 *
 * Admin inherits common user properties
 * from the User class.
 *
 * No additional admin-specific fields are required
 * by the existing database schema.
 */
public class Admin extends User {

    // Default constructor
    public Admin() {
        super();
        setRole("ADMIN");
    }

    // Constructor for an existing admin
    public Admin(int userId, String name,
                 String email, String password) {

        super(userId, name, email, password, "ADMIN");
    }

    // Constructor for creating an admin object
    // before a database ID is assigned
    public Admin(String name, String email,
                 String password) {

        super(name, email, password, "ADMIN");
    }

    /*
     * Getters and setters are inherited from User.
     *
     * Examples:
     * getUserId()
     * setUserId(int userId)
     * getName()
     * setName(String name)
     * getEmail()
     * setEmail(String email)
     * getPassword()
     * setPassword(String password)
     * getRole()
     * setRole(String role)
     *
     * No additional getters or setters are needed
     * because Admin has no extra fields.
     */
}
