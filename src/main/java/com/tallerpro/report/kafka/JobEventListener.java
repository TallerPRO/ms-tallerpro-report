package com.tallerpro.report.kafka;

import com.tallerpro.report.dto.JobEvent;
import com.tallerpro.report.service.OrderIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventListener {

    private final OrderIngestionService orderIngestionService;

    /**
     * Consume el topico jobs.events (fuente de verdad de la orden de
     * servicio). El ack manual se confirma solo despues de persistir
     * el evento, garantizando at-least-once sin bloquear al productor
     * (ms-tallerpro-jobs publica y sigue, no espera a este consumidor).
     */
    @KafkaListener(
            topics = "${app.kafka.topics.jobs-events}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onJobEvent(JobEvent event, Acknowledgment acknowledgment) {
        log.debug("Evento recibido: eventId={} orderId={} status={}",
                event.eventId(), event.orderId(), event.status());
        orderIngestionService.ingest(event);
        acknowledgment.acknowledge();
    }
}
