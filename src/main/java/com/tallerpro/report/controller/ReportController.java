package com.tallerpro.report.controller;

import com.tallerpro.report.dto.KpiDtos.ActiveOrdersByStatus;
import com.tallerpro.report.dto.KpiDtos.DwellTime;
import com.tallerpro.report.dto.KpiDtos.OperationsPanel;
import com.tallerpro.report.dto.KpiDtos.OrdersPerHour;
import com.tallerpro.report.service.KpiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * RF-14: endpoints de solo lectura para el panel de operaciones.
 * Protegidos por SecurityConfig (solo ROLE_Admin).
 *
 * Flujo de llamada real (seccion 6 del caso):
 * JWT -> API Gateway -> ms-tallerpro-bff -> ms-tallerpro-report
 */
@Tag(name = "Reporteria", description = "KPIs operativos de la red TallerPro")
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final KpiService kpiService;

    @Operation(summary = "Panel consolidado de KPIs (ordenes/hora, permanencia, activas)")
    @GetMapping("/kpis")
    public ResponseEntity<OperationsPanel> panel(
            @Parameter(description = "Filtrar por taller (opcional)")
            @RequestParam(required = false) String tallerId) {
        return ResponseEntity.ok(kpiService.operationsPanel(tallerId));
    }

    @Operation(summary = "Ordenes por hora por taller (ventana deslizante)")
    @GetMapping("/kpis/orders-per-hour")
    public ResponseEntity<List<OrdersPerHour>> ordersPerHour(
            @RequestParam(required = false) String tallerId,
            @Parameter(description = "Tamano de la ventana en minutos (default 60)")
            @RequestParam(required = false, defaultValue = "60") long windowMinutes) {
        return ResponseEntity.ok(kpiService.ordersPerHour(tallerId, Duration.ofMinutes(windowMinutes)));
    }

    @Operation(summary = "Tiempo de permanencia promedio (RECEPCIONADA -> ENTREGADA) por taller")
    @GetMapping("/kpis/dwell-time")
    public ResponseEntity<List<DwellTime>> dwellTime(
            @RequestParam(required = false) String tallerId,
            @Parameter(description = "Calcular desde esta fecha (ISO-8601). Default: ultimas 24h")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        Instant effectiveSince = since != null ? since : Instant.now().minus(Duration.ofDays(1));
        return ResponseEntity.ok(kpiService.averageDwellTime(tallerId, effectiveSince));
    }

    @Operation(summary = "Ordenes activas por estado y por taller, en tiempo real")
    @GetMapping("/kpis/active-orders")
    public ResponseEntity<List<ActiveOrdersByStatus>> activeOrders(
            @RequestParam(required = false) String tallerId) {
        return ResponseEntity.ok(kpiService.activeOrdersByStatus(tallerId));
    }
}
