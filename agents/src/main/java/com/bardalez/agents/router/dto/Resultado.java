package com.bardalez.agents.router.dto;

/**
 * Lo que el Router devuelve tras atender una peticion: como clasifico el mensaje y que hay que
 * contestarle al empleado.
 *
 * <p>No es la respuesta HTTP, es el contrato interno entre el {@code RouterAgent} y el
 * {@code RouterController}. El controlador lo traduce a {@link RouterResponse} anadiendo el prompt
 * original y el identificador de sesion.
 *
 * @param intencion {@code INFORMATIVA} o {@code ACCION}
 * @param respuesta texto para el empleado, o {@code null} si todavia no hay ningun agente capaz de
 *                  producirlo (es el caso de la rama de accion hasta que exista el Agent Process)
 */
public record Resultado(String intencion, String respuesta) {
}
