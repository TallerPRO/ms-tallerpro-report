package com.tallerpro.report.kafka;

import com.tallerpro.report.dto.JobEvent;
import com.tallerpro.report.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Contrato con ms-tallerpro-jobs: este JSON es la forma exacta de JobsEventEnvelope
 * publicada en jobs.events. Si jobs cambia el envelope, este test debe fallar.
 */
class JobsEventContractTest {

    private static final String EVENTO_DE_JOBS = """
            {
              "type": "OrdenDiagnosticada",
              "eventId": "b3f1c2d4-0000-0000-0000-000000000001",
              "timestamp": "2026-09-21T14:32:01.123456Z",
              "traceId": "trace-1",
              "correlationId": "b3f1c2d4-0000-0000-0000-000000000001",
              "aggregateId": "7248245a-dd95-4d81-830c-3ab5cd772ed7",
              "orderId": "7248245a-dd95-4d81-830c-3ab5cd772ed7",
              "tallerId": "11111111-1111-1111-1111-111111111111",
              "status": "DIAGNOSTICADA",
              "actorId": "3f2a-oid",
              "actorName": "Matias Baez",
              "actorRole": "JEFE_TALLER",
              "payload": "{\\"id\\":\\"7248245a-dd95-4d81-830c-3ab5cd772ed7\\",\\"estado\\":\\"DIAGNOSTICADA\\"}"
            }
            """;

    @Test
    void deserializaElEnvelopePublicadoPorJobs() {
        try (JsonDeserializer<JobEvent> deserializer = new JsonDeserializer<>(JobEvent.class)) {
            JobEvent msg = deserializer.deserialize("jobs.events", EVENTO_DE_JOBS.getBytes(StandardCharsets.UTF_8));

            assertNotNull(msg);
            assertEquals("7248245a-dd95-4d81-830c-3ab5cd772ed7", msg.orderId());
            assertEquals("11111111-1111-1111-1111-111111111111", msg.tallerId());
            assertEquals(OrderStatus.DIAGNOSTICADA, msg.status());
        }
    }
}
