package com.foodordering.service;

import com.foodordering.model.Payment;
import com.foodordering.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Handles simulated online payments
public class OnlinePayment implements Payment {

    // Stores the simulated payment result
    private boolean simulateSuccess;

    // Default constructor: simulate successful payment
    public OnlinePayment() {
        this.simulateSuccess = true;
    }

    // Constructor to simulate success or failure
    public OnlinePayment(boolean simulateSuccess) {
        this.simulateSuccess = simulateSuccess;
    }

    // Process online payment
    @Override
    public String processPayment(int orderId, BigDecimal amount)
            throws SQLException {

        if (orderId <= 0 || amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid order ID or payment amount!");
        }

        // Determine simulated payment status
        String status;

        if (simulateSuccess) {
            status = "SUCCESS";
        } else {
            status = "FAILED";
        }

        // SQL query to save payment information
        String sql = "INSERT INTO payments "
                + "(order_id, payment_method, amount, payment_status) "
                + "VALUES (?, 'ONLINE', ?, ?)";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new SQLException("Database connection failed!");
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                ps.setBigDecimal(2, amount);
                ps.setString(3, status);

                int rows = ps.executeUpdate();

                if (rows == 1) {
                    return status;
                }

                throw new SQLException("Online payment could not be saved!");
            }
        }
    }
}
