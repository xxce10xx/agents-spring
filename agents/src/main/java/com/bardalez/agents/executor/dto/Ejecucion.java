package com.bardalez.agents.executor.dto;

import java.util.List;
import java.util.stream.IntStream;

/**
 * El informe de ejecucion: lo que el Agent Executor devuelve al Router.
 *
 * <p>Es una <em>salida estructurada</em>. Spring AI convierte este record en un esquema JSON, lo anade
 * a la peticion y deserializa la respuesta del modelo, asi que el Router recibe un objeto y no un
 * parrafo que habria que interpretar. El JSON que produce el LLM tiene esta forma:
 *
 * <pre>{@code
 * {
 *   "estado": "FALLIDO",
 *   "pasosExitosos": [ { "paso": 1, "herramienta": "tool-A", "detalle": "..." } ],
 *   "pasosFallidos": [ { "paso": 2, "herramienta": "tool-B", "detalle": "..." } ],
 *   "resumen": "Se completo el primer paso y el segundo no se pudo ejecutar."
 * }
 * }</pre>
 *
 * <p><strong>El constructor no se limita a copiar campos.</strong> El objeto lo rellena un modelo de
 * lenguaje, asi que llega con dos riesgos tipicos: campos ausentes (que en Java son {@code null}) y un
 * estado global que no concuerda con el detalle. Los dos se corrigen aqui, una sola vez, en lugar de
 * repartir comprobaciones por todo el Router.
 *
 * @param estado si el procedimiento se completo. Nunca es {@code null}: ante la duda, {@code FALLIDO}
 * @param pasosExitosos pasos que se ejecutaron, en orden. Nunca es {@code null}
 * @param pasosFallidos pasos que no se pudieron ejecutar, incluidos los que no se llegaron a intentar
 *                      porque uno anterior fallo. Nunca es {@code null}
 * @param resumen una o dos frases en espanol contando que paso. Es lo unico de este objeto que puede
 *                acabar leyendo el empleado
 */
public record Ejecucion(EstadoEjecucion estado,
                        List<PasoEjecutado> pasosExitosos,
                        List<PasoEjecutado> pasosFallidos,
                        String resumen) {

    public Ejecucion {
        pasosExitosos = pasosExitosos == null ? List.of() : List.copyOf(pasosExitosos);
        pasosFallidos = pasosFallidos == null ? List.of() : List.copyOf(pasosFallidos);

        // Fallar cerrado: si el modelo se olvido del estado, el procedimiento no se da por bueno.
        if (estado == null) {
            estado = EstadoEjecucion.FALLIDO;
        }

        // Y el estado global no lo decide el modelo, lo decide el detalle: un informe que lista pasos
        // fallidos y se declara EXITOSO es incoherente, y de los dos datos el fiable es la lista. Sin
        // esta linea el Router le diria al empleado "listo" sobre un procedimiento a medias.
        if (!pasosFallidos.isEmpty()) {
            estado = EstadoEjecucion.FALLIDO;
        }

        // El resumen puede acabar delante del empleado, asi que no puede quedar en blanco. Si el
        // modelo no lo escribio, el estado ya da para una frase honesta.
        if (resumen == null || resumen.isBlank()) {
            resumen = estado == EstadoEjecucion.EXITOSO
                    ? "El procedimiento se ejecutó completo."
                    : "El procedimiento no se pudo completar.";
        }
    }

    /**
     * El informe para cuando la ejecucion no se pudo ni intentar: el modelo no contesto, o la llamada
     * se rompio a mitad.
     *
     * <p>Existe porque el Executor <strong>nunca</strong> propaga una excepcion al Router: el contrato
     * de salida es siempre el mismo objeto, con {@code estado} como discriminante. Todos los pasos se
     * marcan como fallidos porque desde fuera no se sabe cuales llegaron a ejecutarse; decir "fallo
     * todo" es menos enganoso que adivinar.
     */
    public static Ejecucion errorTecnico(List<String> pasos, String motivo) {
        List<PasoEjecutado> fallidos = IntStream.range(0, pasos.size())
                .mapToObj(i -> new PasoEjecutado(i + 1, pasos.get(i), "No se pudo ejecutar: " + motivo))
                .toList();

        return new Ejecucion(EstadoEjecucion.FALLIDO, List.of(), fallidos,
                "No pude ejecutar el procedimiento por un fallo técnico.");
    }
}
