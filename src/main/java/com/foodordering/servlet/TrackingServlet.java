package com.foodordering.servlet;

import com.foodordering.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/tracking")
public class TrackingServlet extends HttpServlet {

    // Customer views their own order
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null
                || !"CUSTOMER".equalsIgnoreCase(
                        String.valueOf(session.getAttribute("role")))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Please log in as a customer.");
            return;
        }

        Object userIdObject = session.getAttribute("userId");

        if (!(userIdObject instanceof Number)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Customer ID is missing from the session.");
            return;
        }

        int userId = ((Number) userIdObject).intValue();
        String orderIdValue = request.getParameter("orderId");

        if (orderIdValue == null || orderIdValue.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Order ID is required.");
            return;
        }

        int orderId;

        try {
            orderId = Integer.parseInt(orderIdValue);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid order ID.");
            return;
        }

        if (orderId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid order ID.");
            return;
        }

        String sql = "SELECT order_id, status, order_date, total "
                   + "FROM orders WHERE order_id = ? AND user_id = ?";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new SQLException("Database connection failed.");
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                ps.setInt(2, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        request.setAttribute("orderId", rs.getInt("order_id"));
                        request.setAttribute("orderStatus", rs.getString("status"));
                        request.setAttribute("orderDate", rs.getTimestamp("order_date"));
                        request.setAttribute("orderTotal", rs.getBigDecimal("total"));

                        request.getRequestDispatcher(
                                "/WEB-INF/views/tracking.jsp"
                        ).forward(request, response);

                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                "Order not found.");
                    }
                }
            }

        } catch (SQLException e) {
            throw new ServletException("Unable to track the order.", e);
        }
    }

    // Restaurant or admin updates an order's status
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Please log in first.");
            return;
        }

        String role = String.valueOf(session.getAttribute("role"));
        boolean isRestaurant = "RESTAURANT".equalsIgnoreCase(role);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);

        if (!isRestaurant && !isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "You are not allowed to update order status.");
            return;
        }

        int orderId;
        String newStatus = request.getParameter("status");

        try {
            String orderIdValue = request.getParameter("orderId");
            orderId = Integer.parseInt(orderIdValue);
            if (orderId <= 0) {
                throw new NumberFormatException("Order ID must be positive.");
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid order ID.");
            return;
        }

        String previousStatus;

        switch (newStatus == null ? "" : newStatus) {
            case "Preparing":
                previousStatus = "Placed";
                break;
            case "Ready":
                previousStatus = "Preparing";
                break;
            case "Delivered":
                previousStatus = "Ready";
                break;
            default:
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid order status.");
                return;
        }

        String sql;

        if (isRestaurant) {
            Object restaurantIdObject = session.getAttribute("restaurantId");

            if (!(restaurantIdObject instanceof Number)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Restaurant ID is missing from the session.");
                return;
            }

            int restaurantId = ((Number) restaurantIdObject).intValue();

            sql = "UPDATE orders SET status = ? "
                + "WHERE order_id = ? AND restaurant_id = ? AND status = ?";

            try (Connection con = DBConnection.getConnection()) {
                if (con == null) {
                    throw new SQLException("Database connection failed.");
                }

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, orderId);
                    ps.setInt(3, restaurantId);
                    ps.setString(4, previousStatus);

                    int rows = ps.executeUpdate();

                    if (rows == 1) {
                        response.sendRedirect(
                                request.getContextPath()
                                + "/restaurant/orders?updated=true"
                        );
                    } else {
                        response.sendError(HttpServletResponse.SC_CONFLICT,
                                "Order not found, not assigned to your restaurant, "
                                + "or status cannot be advanced.");
                    }
                }
            } catch (SQLException e) {
                throw new ServletException("Unable to update order status.", e);
            }

        } else {
            sql = "UPDATE orders SET status = ? "
                + "WHERE order_id = ? AND status = ?";

            try (Connection con = DBConnection.getConnection()) {
                if (con == null) {
                    throw new SQLException("Database connection failed.");
                }

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, orderId);
                    ps.setString(3, previousStatus);

                    int rows = ps.executeUpdate();

                    if (rows == 1) {
                        response.sendRedirect(
                                request.getContextPath()
                                + "/admin/orders?updated=true"
                        );
                    } else {
                        response.sendError(HttpServletResponse.SC_CONFLICT,
                                "Order not found or status cannot be advanced.");
                    }
                }
            } catch (SQLException e) {
                throw new ServletException("Unable to update order status.", e);
            }
        }
    }
}
