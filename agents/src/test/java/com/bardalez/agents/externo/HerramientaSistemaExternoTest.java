package com.bardalez.agents.externo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.bardalez.agents.executor.dto.Ejecucion;
import com.bardalez.agents.executor.dto.EstadoEjecucion;
import com.bardalez.agents.executor.dto.PasoEjecutado;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import tools.jackson.databind.json.JsonMapper;

/**
 * La tool del sistema externo. Como es ficticia, lo unico que se puede comprobar sin red es lo que
 * realmente importa del contrato: que publica su nombre y que la confirmacion que devuelve identifica
 * la ejecucion.
 *
 * <p>El {@code JsonMapper} se construye a mano en lugar de inyectarlo: este test no necesita contexto de
 * Spring, y asi tarda milisegundos.
 */
class HerramientaSistemaExternoTest {

    private final HerramientaSistemaExterno herramienta =
            new HerramientaSistemaExterno(JsonMapper.builder().build());

    private static final Ejecucion INFORME = new Ejecucion(EstadoEjecucion.EXITOSO,
            List.of(new PasoEjecutado(1, "tool-A", "hecho")), List.of(), "Listo.");

    @Test
    @DisplayName("La tool se publica con su nombre y su descripcion")
    void publicaSuNombre() throws NoSuchMethodException {
        Tool anotacion = HerramientaSistemaExterno.class
                .getDeclaredMethod("enviar", String.class, Ejecucion.class)
                .getAnnotation(Tool.class);

        assertThat(anotacion).isNotNull();
        assertThat(anotacion.name()).isEqualTo(HerramientaSistemaExterno.NOMBRE);
        assertThat(anotacion.description()).isNotBlank();
    }

    @Test
    @DisplayName("El envio devuelve una confirmacion que identifica la ejecucion")
    void confirmaConElIdentificador() {
        // La confirmacion no es adorno: sin el executionId dentro, una linea de log de un envio no se
        // podria cruzar con su fila de auditoria, que es justo para lo que se envia.
        assertThat(herramienta.enviar("ejec-123", INFORME)).contains("ejec-123");
    }

    @Test
    @DisplayName("Un informe fallido tambien se envia: el sistema externo se entera de los dos casos")
    void enviaTambienLoQueFallo() {
        Ejecucion fallida = new Ejecucion(EstadoEjecucion.FALLIDO, List.of(),
                List.of(new PasoEjecutado(1, "tool-A", "no respondió")), "No se pudo.");

        assertThat(herramienta.enviar("ejec-456", fallida)).contains("ejec-456");
    }
}
