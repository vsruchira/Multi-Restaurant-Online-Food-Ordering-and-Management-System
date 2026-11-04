
package com.foodordering.model;

/**
 * Customer model for the Food Ordering System.
 *
 * Customer inherits common user properties
 * from the User class.
 *
 * No additional customer fields are required
 * by the existing database schema.
 */
public class Customer extends User {

    // Default constructor
    public Customer() {
        super();
        setRole("CUSTOMER");
    }

    // Constructor for an existing customer
    public Customer(int userId, String name,
                    String email, String password) {

        super(userId, name, email, password, "CUSTOMER");
    }

    // Constructor for customer registration
    // The database generates the user ID.
    public Customer(String name, String email,
                    String password) {

        super(name, email, password, "CUSTOMER");
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
     * No extra getters or setters are needed because
     * Customer has no customer-specific fields.
     */
}
