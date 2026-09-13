package com.bardalez.agents.router.dto;

/**
 * Cuerpo de la peticion de entrada al sistema.
 *
 * @param prompt mensaje del empleado en lenguaje natural
 */
public record RouterRequest(String prompt) {
}
