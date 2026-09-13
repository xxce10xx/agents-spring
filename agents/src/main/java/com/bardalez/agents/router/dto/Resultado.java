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
 * @param respuesta texto para el empleado. Lo redacta el Router: en la rama informativa es lo que
 *                  contesto Search; en la de accion, los pasos que resolvio Process (o el aviso de
 *                  que no existe ese procedimiento). Los agentes devuelven datos, el hub los cuenta
 */
public record Resultado(String intencion, String respuesta) {
}
