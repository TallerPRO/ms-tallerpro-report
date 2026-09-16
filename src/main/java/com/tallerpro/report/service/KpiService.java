package com.tallerpro.report.service;

import com.tallerpro.report.dto.KpiDtos.ActiveOrdersByStatus;
import com.tallerpro.report.dto.KpiDtos.DwellTime;
import com.tallerpro.report.dto.KpiDtos.OperationsPanel;
import com.tallerpro.report.dto.KpiDtos.OrdersPerHour;
import com.tallerpro.report.model.OrderState;
import com.tallerpro.report.model.OrderStatus;
import com.tallerpro.report.repository.OrderEventRepository;
import com.tallerpro.report.repository.OrderEventRepository.TallerCountProjection;
import com.tallerpro.report.repository.OrderStateRepository;
import com.tallerpro.report.repository.OrderStateRepository.ActiveCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RF-13: calculo de los tres KPIs operativos de TallerPro.
 *
 * Las consultas van directo a la base propia de este microservicio
 * (nunca a ms-tallerpro-jobs), sobre columnas indexadas por
 * taller/estado/timestamp, lo que mantiene el calculo en el orden de
 * milisegundos y permite cumplir el RNF de desfase maximo de 5s entre
 * el evento Kafka y su reflejo en el panel.
 */
@Service
@RequiredArgsConstructor
public class KpiService {

    private final OrderEventRepository orderEventRepository;
    private final OrderStateRepository orderStateRepository;

    private static final Duration DEFAULT_WINDOW = Duration.ofHours(1);

    /** Ordenes por hora por taller (ventana deslizante de 60 min sobre eventos RECEPCIONADA). */
    public List<OrdersPerHour> ordersPerHour(String tallerId, Duration window) {
        Duration effectiveWindow = window != null ? window : DEFAULT_WINDOW;
        Instant now = Instant.now();
        Instant since = now.minus(effectiveWindow);

        List<TallerCountProjection> rows = (tallerId == null)
                ? orderEventRepository.countByStatusSince(OrderStatus.RECEPCIONADA, since)
                : orderEventRepository.countByStatusSinceAndTaller(OrderStatus.RECEPCIONADA, since, tallerId);

        return rows.stream()
                .map(r -> new OrdersPerHour(r.getTallerId(), r.getTotal(), since, now))
                .collect(Collectors.toList());
    }

    /** Tiempo de permanencia promedio (RECEPCIONADA -> ENTREGADA) por taller, en minutos. */
    public List<DwellTime> averageDwellTime(String tallerId, Instant since) {
        List<OrderState> completed = orderStateRepository
                .findCompletedForDwellTime(OrderStatus.ENTREGADA, tallerId, since);

        Map<String, List<OrderState>> byTaller = completed.stream()
                .collect(Collectors.groupingBy(OrderState::getTallerId, LinkedHashMap::new, Collectors.toList()));

        return byTaller.entrySet().stream()
                .map(e -> {
                    double avgMinutes = e.getValue().stream()
                            .mapToLong(os -> Duration.between(os.getReceivedAt(), os.getDeliveredAt()).toMinutes())
                            .average()
                            .orElse(0.0);
                    return new DwellTime(e.getKey(), Math.round(avgMinutes * 100.0) / 100.0, e.getValue().size());
                })
                .collect(Collectors.toList());
    }

    /** Ordenes activas (no ENTREGADA/ANULADA) agrupadas por estado, por taller. */
    public List<ActiveOrdersByStatus> activeOrdersByStatus(String tallerId) {
        List<ActiveCountProjection> rows = orderStateRepository.countActiveByStatus(tallerId);

        Map<String, Map<String, Long>> byTaller = new LinkedHashMap<>();
        for (ActiveCountProjection row : rows) {
            byTaller.computeIfAbsent(row.getTallerId(), k -> new LinkedHashMap<>())
                    .put(row.getStatus().name(), row.getTotal());
        }

        return byTaller.entrySet().stream()
                .map(e -> new ActiveOrdersByStatus(
                        e.getKey(),
                        e.getValue(),
                        e.getValue().values().stream().mapToLong(Long::longValue).sum()))
                .collect(Collectors.toList());
    }

    /** Panel consolidado para el endpoint principal del dashboard. */
    public OperationsPanel operationsPanel(String tallerId) {
        return new OperationsPanel(
                Instant.now(),
                ordersPerHour(tallerId, DEFAULT_WINDOW),
                averageDwellTime(tallerId, Instant.now().minus(Duration.ofDays(1))),
                activeOrdersByStatus(tallerId)
        );
    }
}
