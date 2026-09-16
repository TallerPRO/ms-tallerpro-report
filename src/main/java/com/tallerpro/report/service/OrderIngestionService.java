package com.tallerpro.report.service;

import com.tallerpro.report.dto.JobEvent;
import com.tallerpro.report.model.OrderEvent;
import com.tallerpro.report.model.OrderState;
import com.tallerpro.report.model.OrderStatus;
import com.tallerpro.report.repository.OrderEventRepository;
import com.tallerpro.report.repository.OrderStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Procesa cada JobEvent consumido de Kafka:
 *
 *  1) Idempotencia: si el eventId ya fue procesado (redelivery), se ignora.
 *  2) Log inmutable en order_event (para KPIs de ventana deslizante).
 *  3) Upsert del snapshot en order_state (para KPIs de estado activo y
 *     tiempo de permanencia).
 *
 * Todo ocurre en una transaccion local a este microservicio: no hay
 * llamadas salientes a ms-tallerpro-jobs, por lo que un pico de eventos
 * jamas puede bloquear el nucleo transaccional del dominio jobs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderIngestionService {

    private final OrderEventRepository orderEventRepository;
    private final OrderStateRepository orderStateRepository;

    @Transactional
    public void ingest(JobEvent event) {
        if (orderEventRepository.existsByEventId(event.eventId())) {
            log.info("Evento duplicado ignorado (idempotencia): eventId={}", event.eventId());
            return;
        }

        Instant processedAt = Instant.now();

        OrderEvent orderEvent = OrderEvent.builder()
                .eventId(event.eventId())
                .orderId(event.orderId())
                .tallerId(event.tallerId())
                .status(event.status())
                .eventTimestamp(event.timestamp() != null ? event.timestamp() : processedAt)
                .processedAt(processedAt)
                .build();
        orderEventRepository.save(orderEvent);

        long lagMs = java.time.Duration.between(orderEvent.getEventTimestamp(), processedAt).toMillis();
        if (lagMs > 5000) {
            log.warn("Desfase Kafka->panel superior a 5s: eventId={} lagMs={}", event.eventId(), lagMs);
        }

        upsertState(event, processedAt);
    }

    private void upsertState(JobEvent event, Instant processedAt) {
        OrderState state = orderStateRepository.findById(event.orderId())
                .orElseGet(() -> OrderState.builder()
                        .orderId(event.orderId())
                        .tallerId(event.tallerId())
                        .build());

        state.setTallerId(event.tallerId());
        state.setStatus(event.status());
        state.setLastUpdated(processedAt);

        if (event.status() == OrderStatus.RECEPCIONADA && state.getReceivedAt() == null) {
            state.setReceivedAt(event.timestamp() != null ? event.timestamp() : processedAt);
        }
        if (event.status() == OrderStatus.ENTREGADA) {
            state.setDeliveredAt(event.timestamp() != null ? event.timestamp() : processedAt);
        }

        orderStateRepository.save(state);
    }
}
