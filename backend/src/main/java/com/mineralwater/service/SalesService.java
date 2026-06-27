package com.mineralwater.service;

import com.mineralwater.model.Order;
import com.mineralwater.model.OrderItem;
import com.mineralwater.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final OrderRepository orderRepository;

    /**
     * Daily Sales — all orders for a specific date
     */
    public Map<String, Object> getDailySales(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        List<Order> delivered = filterByStatus(orders, Order.Status.DELIVERED);
        List<Order> pending   = filterByStatus(orders, Order.Status.PENDING);
        List<Order> cancelled = filterByStatus(orders, Order.Status.CANCELLED);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date.toString());
        result.put("totalOrders", orders.size());
        result.put("deliveredOrders", delivered.size());
        result.put("pendingOrders", pending.size());
        result.put("cancelledOrders", cancelled.size());
        result.put("totalRevenue", calcRevenue(delivered));
        result.put("totalItemsSold", calcItemsSold(delivered));
        result.put("topProducts", getTopProducts(delivered));
        result.put("orders", buildOrderSummaries(orders));
        return result;
    }

    /**
     * Monthly Sales — all orders for a specific month/year
     */
    public Map<String, Object> getMonthlySales(int year, int month) {
        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end   = LocalDate.of(year, month, 1)
                                .withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth())
                                .atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        List<Order> delivered = filterByStatus(orders, Order.Status.DELIVERED);
        List<Order> pending   = filterByStatus(orders, Order.Status.PENDING);
        List<Order> cancelled = filterByStatus(orders, Order.Status.CANCELLED);

        // Daily breakdown within the month
        Map<Integer, Double> dailyRevenue = new LinkedHashMap<>();
        Map<Integer, Integer> dailyOrders = new LinkedHashMap<>();
        int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
        for (int d = 1; d <= daysInMonth; d++) {
            dailyRevenue.put(d, 0.0);
            dailyOrders.put(d, 0);
        }
        for (Order o : delivered) {
            int day = o.getCreatedAt().getDayOfMonth();
            dailyRevenue.merge(day, o.getTotalAmount(), Double::sum);
            dailyOrders.merge(day, 1, Integer::sum);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("month", Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        result.put("totalOrders", orders.size());
        result.put("deliveredOrders", delivered.size());
        result.put("pendingOrders", pending.size());
        result.put("cancelledOrders", cancelled.size());
        result.put("totalRevenue", calcRevenue(delivered));
        result.put("totalItemsSold", calcItemsSold(delivered));
        result.put("topProducts", getTopProducts(delivered));
        result.put("dailyRevenue", dailyRevenue);
        result.put("dailyOrders", dailyOrders);
        return result;
    }

    /**
     * Yearly chart — month-wise revenue for the whole year
     */
    public Map<String, Object> getYearlyChartData(int year) {
        LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime end   = LocalDate.of(year, 12, 31).atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        List<Order> delivered = filterByStatus(orders, Order.Status.DELIVERED);

        List<String> months = new ArrayList<>();
        List<Double> revenue = new ArrayList<>();
        List<Integer> orderCounts = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            final int month = m;
            String monthName = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            List<Order> monthOrders = delivered.stream()
                    .filter(o -> o.getCreatedAt().getMonthValue() == month)
                    .collect(Collectors.toList());

            months.add(monthName);
            revenue.add(calcRevenue(monthOrders));
            orderCounts.add(monthOrders.size());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("months", months);
        result.put("revenue", revenue);
        result.put("orderCounts", orderCounts);
        result.put("totalYearRevenue", calcRevenue(delivered));
        result.put("totalYearOrders", delivered.size());
        return result;
    }

    // ===== Helpers =====

    private List<Order> filterByStatus(List<Order> orders, Order.Status status) {
        return orders.stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
    }

    private double calcRevenue(List<Order> orders) {
        return orders.stream().mapToDouble(Order::getTotalAmount).sum();
    }

    private int calcItemsSold(List<Order> orders) {
        return orders.stream()
                .flatMap(o -> o.getItems().stream())
                .mapToInt(OrderItem::getQuantity).sum();
    }

    private List<Map<String, Object>> getTopProducts(List<Order> orders) {
        Map<String, Integer> productCount = new LinkedHashMap<>();
        Map<String, Double> productRevenue = new LinkedHashMap<>();

        for (Order o : orders) {
            for (OrderItem item : o.getItems()) {
                String name = item.getProduct().getName();
                productCount.merge(name, item.getQuantity(), Integer::sum);
                productRevenue.merge(name, item.getPrice() * item.getQuantity(), Double::sum);
            }
        }

        return productCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("name", e.getKey());
                    p.put("quantitySold", e.getValue());
                    p.put("revenue", productRevenue.get(e.getKey()));
                    return p;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildOrderSummaries(List<Order> orders) {
        return orders.stream().map(o -> {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", o.getId());
            summary.put("customer", o.getUser().getName());
            summary.put("amount", o.getTotalAmount());
            summary.put("status", o.getStatus());
            summary.put("time", o.getCreatedAt().toString());
            return summary;
        }).collect(Collectors.toList());
    }
}
