package com.bardalez.agents.auditoria;

import java.time.LocalDateTime;

import com.bardalez.agents.executor.dto.EstadoEjecucion;

/**
 * Una fila de la tabla {@code ejecucion}: el rastro que queda de un procedimiento ejecutado.
 *
 * <p>Es el modelo de <strong>lectura</strong> de la auditoria. Escribir no usa este record —el
 * repositorio recibe el {@code Procedimiento} y el {@code Ejecucion} y los descompone en columnas—,
 * pero leer si: los nombres de los componentes son el contrato con las columnas, porque
 * {@code query(RegistroEjecucion.class)} mapea por nombre convirtiendo {@code execution_id} en
 * {@code executionId}. Renombrar una columna deja el componente en {@code null} sin que nada falle.
 *
 * @param executionId identificador de la ejecucion, generado por el Router
 * @param userId claim de identidad del empleado. Hoy siempre {@code null}: aun no hay autenticacion
 * @param sessionId conversacion en la que se pidio el tramite; correlaciona con la memoria
 * @param procedimiento nombre del procedimiento ejecutado, tal como esta en el catalogo
 * @param estadoEjecucion si el procedimiento se completo
 * @param estadoEnvio si el sistema externo llego a recibir el informe
 * @param informe el structured output del Executor completo, serializado como JSON
 * @param creadoEn cuando se registro; lo pone la base de datos, no la aplicacion
 */
public record RegistroEjecucion(String executionId,
                                String userId,
                                String sessionId,
                                String procedimiento,
                                EstadoEjecucion estadoEjecucion,
                                EstadoEnvio estadoEnvio,
                                String informe,
                                LocalDateTime creadoEn) {
}
