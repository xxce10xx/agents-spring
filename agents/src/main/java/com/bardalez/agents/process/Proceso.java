package com.bardalez.agents.process;

/**
 * Un procedimiento de negocio tal como lo devuelve el servidor MCP.
 *
 * <p>Este record es <strong>una copia</strong> del que vive en el proyecto {@code mcp-procesos}, y no
 * es duplicacion por descuido: los dos procesos son independientes y lo unico que comparten es el
 * JSON que viaja por el protocolo. No hay libreria comun, no hay clase compartida. Ese es el precio
 * —y la gracia— de integrar por MCP en lugar de por una dependencia Maven: el servidor puede
 * evolucionar sin recompilar el agente.
 *
 * <p>La consecuencia practica es que este contrato hay que probarlo, porque el compilador no lo
 * vigila: si alla renombran una columna, aqui llega {@code null} sin que nada falle.
 *
 * @param id clave de la fila en la tabla {@code procesos}
 * @param proceso nombre del procedimiento ("pedir vacaciones"); es el identificador que el LLM
 *                devuelve cuando elige, asi que tiene que llegar intacto
 * @param tipo familia del procedimiento ("vacaciones", "VPN")
 * @param descripcion que hace, en lenguaje natural. Es el campo que el LLM lee para decidir
 * @param secuencia los pasos como texto JSON, por ejemplo
 *                  {@code {"paso 1": "tool-A", "paso 2": "tool-B"}}. Llega sin interpretar: lo
 *                  traduce a lista {@link CatalogoProcesos#pasosDe(Proceso)}
 */
public record Proceso(Long id, String proceso, String tipo, String descripcion, String secuencia) {
}
