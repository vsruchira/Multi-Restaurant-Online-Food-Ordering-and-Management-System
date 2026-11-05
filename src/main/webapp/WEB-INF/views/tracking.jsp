<%@ page import="java.sql.Timestamp" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.text.SimpleDateFormat" %>

<%!
    // Escape text before displaying it in HTML
    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
%>

<%
    Integer orderId = (Integer) request.getAttribute("orderId");
    String status = (String) request.getAttribute("orderStatus");
    Timestamp orderDate = (Timestamp) request.getAttribute("orderDate");
    BigDecimal total = (BigDecimal) request.getAttribute("orderTotal");

    if (orderId == null || status == null) {
        response.sendError(404, "Order details are unavailable.");
        return;
    }

    String formattedDate = "Not available";

    if (orderDate != null) {
        formattedDate = new SimpleDateFormat("dd-MM-yyyy hh:mm a")
                .format(orderDate);
    }

    int currentStage = -1;

    if ("Placed".equalsIgnoreCase(status)) {
        currentStage = 0;
    } else if ("Preparing".equalsIgnoreCase(status)) {
        currentStage = 1;
    } else if ("Ready".equalsIgnoreCase(status)) {
        currentStage = 2;
    } else if ("Delivered".equalsIgnoreCase(status)) {
        currentStage = 3;
    }
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Track Your Order</title>

    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f4f4f4;
            margin: 0;
            padding: 30px;
        }

        .container {
            max-width: 650px;
            margin: auto;
            background: white;
            padding: 25px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }

        h2 {
            text-align: center;
            color: #333;
        }

        .details {
            background: #f8f8f8;
            padding: 15px;
            border-radius: 8px;
            margin-top: 20px;
        }

        .details p {
            margin: 12px 0;
        }

        .status {
            text-align: center;
            margin: 25px 0;
            font-size: 20px;
            font-weight: bold;
        }

        .progress {
            display: flex;
            justify-content: space-between;
            margin-top: 30px;
            gap: 5px;
        }

        .step {
            flex: 1;
            text-align: center;
            color: #999;
            font-size: 13px;
        }

        .circle {
            width: 28px;
            height: 28px;
            line-height: 28px;
            border-radius: 50%;
            background: #ddd;
            color: white;
            margin: 0 auto 10px;
        }

        .completed .circle {
            background: #28a745;
        }

        .completed {
            color: #28a745;
            font-weight: bold;
        }

        .message {
            text-align: center;
            margin-top: 25px;
            color: #555;
        }

        @media (max-width: 480px) {
            body {
                padding: 12px;
            }

            .container {
                padding: 16px;
            }

            .step {
                font-size: 11px;
            }
        }
    </style>
</head>

<body>
<div class="container">

    <h2>Track Your Order</h2>

    <div class="details">
        <p><strong>Order ID:</strong> <%= orderId %></p>
        <p><strong>Order Date:</strong> <%= escapeHtml(formattedDate) %></p>
        <p>
            <strong>Total Amount:</strong>
            ₹<%= total == null ? "0.00" : total.toPlainString() %>
        </p>
    </div>

    <div class="status">
        Current Status: <%= escapeHtml(status) %>
    </div>

    <div class="progress">
        <%
            String[] stages = {"Placed", "Preparing", "Ready", "Delivered"};

            for (int i = 0; i < stages.length; i++) {
                String completed = (currentStage >= i) ? "completed" : "";
        %>
            <div class="step <%= completed %>">
                <div class="circle">
                    <%= (currentStage >= i) ? "✓" : (i + 1) %>
                </div>
                <%= stages[i] %>
            </div>
        <%
            }
        %>
    </div>

    <div class="message">
        <% if (currentStage == 3) { %>
            Your order has been delivered. Enjoy your meal!
        <% } else if (currentStage >= 0) { %>
            Your order is being processed. Please check again later.
        <% } else { %>
            Your order has an unrecognized status. Please contact the restaurant.
        <% } %>
    </div>

</div>
</body>
</html>
