package com.bardalez.agents.process.dto;

import java.util.List;

/**
 * Lo que el Agent Process devuelve al Router: el procedimiento que corresponde a lo que pidio el
 * empleado, con sus pasos en orden.
 *
 * <p>Los pasos NO los escribe el LLM. El modelo solo elige <em>que</em> procedimiento aplica; los
 * nombres de las herramientas se copian tal cual de la fila que llego por MCP. Asi el Agent Executor
 * no puede recibir nunca una herramienta inventada, que es el fallo que mas caro sale en la rama de
 * accion: un paso alucinado parece perfectamente valido hasta que se intenta ejecutar.
 *
 * @param encontrado {@code false} equivale al {@code NOT_FOUND} del contrato: ningun procedimiento del
 *                   catalogo sirve. No hay reintento ni fallback a Search; el Router se lo dice al
 *                   empleado
 * @param proceso nombre del procedimiento elegido, o {@code null} si no se encontro
 * @param tipo familia del procedimiento, o {@code null} si no se encontro
 * @param pasos herramientas en el orden en que hay que ejecutarlas; vacia si no se encontro
 */
public record Procedimiento(boolean encontrado, String proceso, String tipo, List<String> pasos) {

    /** El caso "no existe un procedimiento para esto", que es una respuesta valida y no un error. */
    public static Procedimiento noEncontrado() {
        return new Procedimiento(false, null, null, List.of());
    }
}
