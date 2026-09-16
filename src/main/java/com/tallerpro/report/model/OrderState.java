package com.tallerpro.report.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Snapshot (upsert) del estado ACTUAL de cada orden de servicio.
 * Se actualiza con cada evento consumido y es la fuente para:
 *
 *  - Conteo de ordenes activas por estado/taller en tiempo real.
 *  - Tiempo de permanencia (receivedAt -> deliveredAt) por taller.
 */
@Entity
@Table(
        name = "order_state",
        indexes = {
                @Index(name = "idx_order_state_taller_status", columnList = "tallerId,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderState {

    @Id
    @Column(length = 60)
    private String orderId;

    @Column(nullable = false, length = 60)
    private String tallerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /** Timestamp del evento RECEPCIONADA. */
    private Instant receivedAt;

    /** Timestamp del evento ENTREGADA (null mientras siga abierta). */
    private Instant deliveredAt;

    @Column(nullable = false)
    private Instant lastUpdated;

    @Version
    private Long version;
}
