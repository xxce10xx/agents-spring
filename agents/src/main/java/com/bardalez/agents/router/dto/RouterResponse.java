package com.bardalez.agents.router.dto;

/**
 * Respuesta de la API.
 *
 * <p>{@code intencion} no es informacion que el empleado necesite: se devuelve para que en clase se
 * vea por que rama paso la peticion. Un cliente real solo leeria {@code respuesta}.
 *
 * @param prompt    mensaje original del empleado
 * @param intencion clasificacion del Router, {@code INFORMATIVA} o {@code ACCION}
 * @param respuesta texto en lenguaje natural para el empleado
 * @param sessionId identificador de la conversacion usado para la memoria
 */
public record RouterResponse(String prompt, String intencion, String respuesta, String sessionId) {
}
