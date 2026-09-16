package com.tallerpro.report.model;

/**
 * Estados del ciclo de vida de una orden de servicio, replicados desde
 * ms-tallerpro-jobs (fuente de verdad en el topico jobs.events).
 *
 * RECEPCIONADA -> DIAGNOSTICADA -> EN_REPARACION -> LISTA_RETIRO -> ENTREGADA
 *                                                                 -> ANULADA
 */
public enum OrderStatus {
    RECEPCIONADA,
    DIAGNOSTICADA,
    EN_REPARACION,
    LISTA_RETIRO,
    ENTREGADA,
    ANULADA;

    public boolean isFinal() {
        return this == ENTREGADA || this == ANULADA;
    }
}
