package com.tallerpro.report.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Log inmutable de cada evento de cambio de estado consumido desde
 * jobs.events. Sirve dos propositos:
 *
 *  1) Idempotencia: el eventId es unico, por lo que reprocesar el mismo
 *     mensaje (redelivery de Kafka) no duplica datos.
 *  2) Base para KPIs de ventana deslizante (ordenes/hora): se consulta
 *     por rango de tiempo y taller.
 */
@Entity
@Table(
        name = "order_event",
        indexes = {
                @Index(name = "idx_order_event_taller_status_ts", columnList = "tallerId,status,eventTimestamp"),
                @Index(name = "idx_order_event_order", columnList = "orderId")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_order_event_event_id", columnNames = "eventId")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(nullable = false, length = 60)
    private String orderId;

    @Column(nullable = false, length = 60)
    private String tallerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /** Timestamp original del evento (emitido por ms-tallerpro-jobs). */
    @Column(nullable = false)
    private Instant eventTimestamp;

    /** Momento en que este microservicio proceso el evento (para medir desfase). */
    @Column(nullable = false)
    private Instant processedAt;
}
