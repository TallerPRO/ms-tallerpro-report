package com.tallerpro.report.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class KpiDtos {

    public record OrdersPerHour(String tallerId, long orders, Instant windowStart, Instant windowEnd) {}

    public record DwellTime(String tallerId, double avgMinutes, long completedOrders) {}

    public record ActiveOrdersByStatus(String tallerId, Map<String, Long> countByStatus, long totalActive) {}

    public record OperationsPanel(
            Instant generatedAt,
            List<OrdersPerHour> ordersPerHour,
            List<DwellTime> dwellTime,
            List<ActiveOrdersByStatus> activeOrders
    ) {}
}
