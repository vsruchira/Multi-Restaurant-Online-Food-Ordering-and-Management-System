package com.foodordering.service;

import com.foodordering.model.Payment;
import com.foodordering.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Handles Cash on Delivery payments
public class CashPayment implements Payment {

    // Register a cash payment
    @Override
    public String processPayment(int orderId, BigDecimal amount)
            throws SQLException {

        if (orderId <= 0 || amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid order ID or payment amount!");
        }

        String sql = "INSERT INTO payments "
                + "(order_id, payment_method, amount, payment_status) "
                + "VALUES (?, 'CASH', ?, 'PENDING')";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new SQLException("Database connection failed!");
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                ps.setBigDecimal(2, amount);

                int rows = ps.executeUpdate();

                if (rows == 1) {
                    return "PENDING";
                }

                throw new SQLException("Cash payment could not be saved!");
            }
        }
    }

    // Restaurant confirms that cash has been collected
    public void confirmCashCollection(int orderId) throws SQLException {

        if (orderId <= 0) {
            throw new IllegalArgumentException("Invalid order ID!");
        }

        String selectSql = "SELECT payment_id FROM payments "
                + "WHERE order_id = ? "
                + "AND payment_method = 'CASH' "
                + "AND payment_status = 'PENDING' FOR UPDATE";

        String updateSql = "UPDATE payments "
                + "SET payment_status = 'SUCCESS' "
                + "WHERE payment_id = ? "
                + "AND payment_status = 'PENDING'";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new SQLException("Database connection failed!");
            }

            boolean originalAutoCommit = con.getAutoCommit();

            try {
                con.setAutoCommit(false);

                int paymentId;

                // Find the pending cash payment
                try (PreparedStatement ps = con.prepareStatement(selectSql)) {
                    ps.setInt(1, orderId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("No pending cash payment found!");
                        }

                        paymentId = rs.getInt("payment_id");

                        // Ensure only one pending cash payment exists
                        if (rs.next()) {
                            throw new SQLException("Multiple pending cash payments found!");
                        }
                    }
                }

                // Update the existing payment record
                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setInt(1, paymentId);

                    int rows = ps.executeUpdate();

                    if (rows != 1) {
                        throw new SQLException("Cash payment confirmation failed!");
                    }
                }

                con.commit();

            } catch (SQLException e) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;

            } finally {
                con.setAutoCommit(originalAutoCommit);
            }
        }
    }
}
