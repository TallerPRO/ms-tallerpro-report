package com.tallerpro.report.repository;

import com.tallerpro.report.model.OrderEvent;
import com.tallerpro.report.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    boolean existsByEventId(String eventId);

    /**
     * Ventana deslizante: cuenta ordenes RECEPCIONADAS por taller dentro
     * de [since, now]. Se usa para el KPI "ordenes por hora".
     */
    @Query("""
            select oe.tallerId as tallerId, count(oe) as total
            from OrderEvent oe
            where oe.status = :status
              and oe.eventTimestamp >= :since
            group by oe.tallerId
            """)
    List<TallerCountProjection> countByStatusSince(@Param("status") OrderStatus status,
                                                     @Param("since") Instant since);

    @Query("""
            select oe.tallerId as tallerId, count(oe) as total
            from OrderEvent oe
            where oe.status = :status
              and oe.eventTimestamp >= :since
              and oe.tallerId = :tallerId
            group by oe.tallerId
            """)
    List<TallerCountProjection> countByStatusSinceAndTaller(@Param("status") OrderStatus status,
                                                              @Param("since") Instant since,
                                                              @Param("tallerId") String tallerId);

    interface TallerCountProjection {
        String getTallerId();
        Long getTotal();
    }
}
