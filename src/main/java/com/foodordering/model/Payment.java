package com.foodordering.model;

import java.math.BigDecimal;
import java.sql.SQLException;

// Interface for different payment methods
public interface Payment {

    // Process payment for an order
    String processPayment(int orderId, BigDecimal amount)
            throws SQLException;
}
