package com.bardalez.agents.executor.dto;

/**
 * Estado general de una ejecucion: si el procedimiento se completo o no.
 *
 * <p>Solo dos valores, y a proposito. Un procedimiento a medias no esta hecho: si un paso falla, el
 * resultado global es {@code FALLIDO} aunque los anteriores hayan funcionado. El detalle de que se
 * hizo y que no vive en las dos listas de {@link Ejecucion}, que es donde se puede leer con precision;
 * inventar un tercer valor ("PARCIAL") solo repartiria la misma informacion en dos sitios.
 *
 * <p>Es un enum y no una cadena porque viaja en el esquema JSON que Spring AI le manda al modelo: el
 * LLM ve la lista cerrada de valores posibles y no puede responder "casi exitoso".
 */
public enum EstadoEjecucion {

    /** Todos los pasos del procedimiento se ejecutaron. */
    EXITOSO,

    /** Al menos un paso no se pudo ejecutar, o la ejecucion se rompio antes de terminar. */
    FALLIDO
}
