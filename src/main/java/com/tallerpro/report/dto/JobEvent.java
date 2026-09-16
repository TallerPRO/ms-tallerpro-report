package com.tallerpro.report.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tallerpro.report.model.OrderStatus;

import java.time.Instant;

/**
 * Representa el envelope comun publicado por ms-tallerpro-jobs en el
 * topico Kafka "jobs.events" (type, eventId, timestamp, traceId,
 * correlationId + payload del dominio).
 *
 * Ejemplo de payload esperado:
 * {
 *   "eventId": "b3f1...",
 *   "type": "ORDER_STATUS_CHANGED",
 *   "timestamp": "2026-09-15T14:32:01Z",
 *   "traceId": "...",
 *   "correlationId": "...",
 *   "orderId": "ORD-1024",
 *   "tallerId": "TALLER-07",
 *   "status": "DIAGNOSTICADA"
 * }
 */
public record JobEvent(
        String eventId,
        String type,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp,
        String traceId,
        String correlationId,
        String orderId,
        String tallerId,
        OrderStatus status
) {
}
