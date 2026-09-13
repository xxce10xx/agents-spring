package com.bardalez.agents.executor.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El informe de ejecucion, con los tres desperfectos que llegan de un LLM.
 *
 * <p>Este record no lo construye el codigo del proyecto: lo rellena Spring AI con lo que devuelva el
 * modelo. Por eso lleva logica en el constructor, y por eso hay un test: un campo ausente es
 * {@code null} en Java, y un estado que no concuerda con el detalle es una respuesta plausible de un
 * modelo. Las dos cosas acabarian en la respuesta al empleado.
 */
class EjecucionTest {

    @Test
    @DisplayName("Las listas ausentes llegan vacias, no nulas")
    void normalizaLasListasNulas() {
        Ejecucion ejecucion = new Ejecucion(EstadoEjecucion.EXITOSO, null, null, "Todo bien.");

        assertThat(ejecucion.pasosExitosos()).isEmpty();
        assertThat(ejecucion.pasosFallidos()).isEmpty();
    }

    @Test
    @DisplayName("Sin estado se asume FALLIDO: se falla cerrado")
    void sinEstadoAsumeFallido() {
        Ejecucion ejecucion = new Ejecucion(null, List.of(), List.of(), "No se sabe.");

        assertThat(ejecucion.estado()).isEqualTo(EstadoEjecucion.FALLIDO);
    }

    @Test
    @DisplayName("Un paso fallido convierte el estado en FALLIDO, aunque el modelo dijera EXITOSO")
    void unPasoFallidoMandaSobreElEstado() {
        // El caso que de verdad importa: el modelo ejecuta dos pasos de tres, informa del que fallo y
        // aun asi se declara exitoso. De los dos datos, el fiable es la lista.
        Ejecucion ejecucion = new Ejecucion(EstadoEjecucion.EXITOSO,
                List.of(new PasoEjecutado(1, "tool-A", "hecho")),
                List.of(new PasoEjecutado(2, "tool-B", "no respondio")),
                "Se ejecuto casi todo.");

        assertThat(ejecucion.estado()).isEqualTo(EstadoEjecucion.FALLIDO);
    }

    @Test
    @DisplayName("Un resumen en blanco se sustituye por una frase honesta")
    void rellenaElResumenVacio() {
        Ejecucion ejecucion = new Ejecucion(EstadoEjecucion.EXITOSO, List.of(), List.of(), "  ");

        assertThat(ejecucion.resumen()).isNotBlank();
    }

    @Test
    @DisplayName("errorTecnico() marca todos los pasos como fallidos")
    void errorTecnicoMarcaTodosLosPasos() {
        // Es lo que devuelve el Executor cuando la llamada al modelo se rompe: no se sabe que se
        // alcanzo a ejecutar, y decir "fallo todo" es menos enganoso que adivinar.
        Ejecucion ejecucion = Ejecucion.errorTecnico(List.of("tool-A", "tool-B"), "timeout");

        assertThat(ejecucion.estado()).isEqualTo(EstadoEjecucion.FALLIDO);
        assertThat(ejecucion.pasosExitosos()).isEmpty();
        assertThat(ejecucion.pasosFallidos())
                .extracting(PasoEjecutado::paso, PasoEjecutado::herramienta)
                .containsExactly(tuple(1, "tool-A"), tuple(2, "tool-B"));
        assertThat(ejecucion.pasosFallidos().get(0).detalle()).contains("timeout");
    }
}
