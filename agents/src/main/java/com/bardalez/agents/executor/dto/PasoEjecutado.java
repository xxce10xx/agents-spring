package com.bardalez.agents.executor.dto;

/**
 * Un paso del procedimiento, tal como acabo.
 *
 * <p>El mismo record sirve para los pasos que salieron bien y para los que no: en que lista aparece es
 * lo que dice como acabo. Eso ahorra un campo de estado que seria redundante con la lista que lo
 * contiene.
 *
 * @param paso posicion del paso dentro del procedimiento, empezando en 1. Es lo que permite hablar
 *             del "paso 2" sin ambiguedad cuando un procedimiento repite la misma herramienta
 * @param herramienta nombre de la tool, tal como aparece en la secuencia que definio el procedimiento
 * @param detalle una linea en espanol sobre lo que paso. En un paso fallido es el campo importante:
 *                es lo unico que explica por que no se pudo hacer
 */
public record PasoEjecutado(int paso, String herramienta, String detalle) {
}
