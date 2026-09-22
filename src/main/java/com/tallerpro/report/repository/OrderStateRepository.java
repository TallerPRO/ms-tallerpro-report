package com.tallerpro.report.repository;

import com.tallerpro.report.model.OrderState;
import com.tallerpro.report.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderStateRepository extends JpaRepository<OrderState, String> {

    List<OrderState> findByTallerId(String tallerId);

    Optional<OrderState> findByOrderIdAndTallerId(String orderId, String tallerId);

    /**
     * Ordenes activas (no finales) agrupadas por estado, opcionalmente filtradas por taller.
     *
     * Los `cast(...)` evitan el "could not determine data type" de PostgreSQL
     * cuando el filtro llega nulo: un parametro que solo aparece en `:p is null`
     * no tiene contexto para inferir su tipo. H2 (los tests) si lo tolera.
     */
    @Query("""
            select os.tallerId as tallerId, os.status as status, count(os) as total
            from OrderState os
            where os.status not in ('ENTREGADA', 'ANULADA')
              and (cast(:tallerId as String) is null or os.tallerId = :tallerId)
            group by os.tallerId, os.status
            """)
    List<ActiveCountProjection> countActiveByStatus(@Param("tallerId") String tallerId);

    /** Ordenes completas (con receivedAt y deliveredAt) para calcular tiempo de permanencia promedio. */
    @Query("""
            select os from OrderState os
            where os.status = :finalStatus
              and os.receivedAt is not null
              and os.deliveredAt is not null
              and (cast(:tallerId as String) is null or os.tallerId = :tallerId)
              and (cast(:since as Instant) is null or os.deliveredAt >= :since)
            """)
    List<OrderState> findCompletedForDwellTime(@Param("finalStatus") OrderStatus finalStatus,
                                                @Param("tallerId") String tallerId,
                                                @Param("since") Instant since);

    interface ActiveCountProjection {
        String getTallerId();
        OrderStatus getStatus();
        Long getTotal();
    }
}
