package com.bardalez.agents.auditoria;

/**
 * Si el informe de una ejecucion llego al sistema externo de reporteria.
 *
 * <p>Es una dimension <strong>independiente</strong> de si el procedimiento se ejecuto bien: por eso
 * hay dos columnas en la tabla y dos enums en el codigo, en lugar de un solo estado con cuatro
 * combinaciones. La combinacion que importa es {@code EXITOSO} + {@link #PENDIENTE_ENVIO}: el tramite
 * se hizo, pero el sistema externo no se entero. Un unico campo mezclado no permitiria consultarla, y
 * es precisamente la que la reporteria externa no puede detectar por si sola: desde fuera, una
 * ejecucion que nunca llego es indistinguible de una que nunca ocurrio.
 */
public enum EstadoEnvio {

    /** La ejecucion esta registrada aqui, pero el sistema externo todavia no la ha recibido. */
    PENDIENTE_ENVIO,

    /** El sistema externo confirmo la recepcion del informe. */
    ENVIADO
}
