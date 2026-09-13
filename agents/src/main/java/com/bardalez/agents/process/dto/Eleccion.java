package com.bardalez.agents.process.dto;

/**
 * La decision del LLM: cual de los procedimientos del catalogo corresponde al mensaje del empleado.
 *
 * <p>Es un objeto de <em>salida estructurada</em>. Spring AI convierte este record en un esquema JSON,
 * lo anade a la peticion y deserializa la respuesta del modelo, asi que en lugar de una frase que
 * habria que interpretar con expresiones regulares llega un objeto. La otra ventaja es que separa
 * limpiamente "no lo encontre" de "aqui esta": con texto libre, distinguirlos es adivinar.
 *
 * <p>Reparar en lo que <strong>no</strong> hay: los pasos. El modelo devuelve solo el nombre elegido,
 * y {@code ProcessAgent} lo busca en el catalogo para copiar la secuencia real. Si el modelo se
 * inventa un nombre, la busqueda falla y se trata como no encontrado, en lugar de propagar una
 * fantasia hasta el Executor.
 *
 * @param encontrado si alguno de los procedimientos del catalogo sirve para lo que pidio el empleado
 * @param proceso nombre del procedimiento elegido, copiado literalmente del catalogo
 * @param motivo por que lo eligio (o por que ninguno sirve). No se le ensena al empleado: es para el
 *               log, y sirve para entender las decisiones del modelo cuando se equivoca
 */
public record Eleccion(boolean encontrado, String proceso, String motivo) {
}
