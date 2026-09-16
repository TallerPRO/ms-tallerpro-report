package com.tallerpro.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ms-tallerpro-report
 *
 * Consume eventos de Kafka (topico jobs.events) para calcular KPIs
 * operativos de la red TallerPro y expone un panel de solo lectura
 * para el rol Admin.
 *
 * El consumo es asincrono y desacoplado: este microservicio nunca
 * llama de vuelta a ms-tallerpro-jobs, por lo que su carga no puede
 * bloquear el nucleo transaccional (RNF de aislamiento).
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportApplication.class, args);
    }
}
