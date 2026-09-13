package com.bardalez.agents.router.dto;

/**
 * Respuesta de prueba del Router. Verifica que la conexion con el LLM funciona; el contrato
 * definitivo de la API se definira cuando el Router enrute hacia los demas agentes.
 *
 * @param prompt    mensaje original del empleado
 * @param intencion clasificacion devuelta por el LLM
 * @param sessionId identificador de sesion recibido en la cabecera, si vino
 */
public record RouterResponse(String prompt, String intencion, String sessionId) {
}
