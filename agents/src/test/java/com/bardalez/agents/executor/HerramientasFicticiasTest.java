package com.bardalez.agents.executor;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

/**
 * Las tres tools ficticias, vistas como las ve Spring AI: por reflexion.
 *
 * <p>No se prueba que escriban en consola —eso ya lo hace el logger—, sino lo que puede romperse sin
 * que nada falle: los nombres. La columna {@code secuencia} de la tabla {@code procesos} del
 * repositorio del MCP guarda literalmente {@code tool-A}, {@code tool-B} y {@code tool-C}. Ese
 * contrato lo une una cadena de texto que cruza dos procesos y una base de datos, asi que ningun
 * compilador lo vigila: renombrar un metodo o su anotacion dejaria procedimientos apuntando a
 * herramientas inexistentes, y el sintoma seria un paso fallido en tiempo de ejecucion.
 */
class HerramientasFicticiasTest {

    private final HerramientasFicticias herramientas = new HerramientasFicticias();

    @Test
    @DisplayName("Las tres tools se publican con los nombres que usa la secuencia de la base de datos")
    void publicaLosTresNombresDelContrato() {
        List<String> nombres = Arrays.stream(HerramientasFicticias.class.getDeclaredMethods())
                .map(metodo -> metodo.getAnnotation(Tool.class))
                .filter(Objects::nonNull)
                .map(Tool::name)
                .toList();

        assertThat(nombres).containsExactlyInAnyOrder("tool-A", "tool-B", "tool-C");
        // Y la lista publica, que es la que se le ensena al modelo en el prompt, dice lo mismo.
        assertThat(HerramientasFicticias.NOMBRES).containsExactlyInAnyOrderElementsOf(nombres);
    }

    @Test
    @DisplayName("Cada tool lleva descripcion: es lo unico que el modelo lee para decidir")
    void todasLlevanDescripcion() {
        List<Method> anotados = Arrays.stream(HerramientasFicticias.class.getDeclaredMethods())
                .filter(metodo -> metodo.isAnnotationPresent(Tool.class))
                .toList();

        assertThat(anotados).hasSize(3);
        assertThat(anotados).allSatisfy(metodo ->
                assertThat(metodo.getAnnotation(Tool.class).description()).isNotBlank());
    }

    @Test
    @DisplayName("Cada tool devuelve una confirmacion con su propio nombre")
    void devuelvenConfirmacion() {
        // El valor de retorno no es adorno: vuelve al modelo como resultado de la llamada, y es lo que
        // le permite saber que el paso salio bien antes de seguir con el siguiente.
        assertThat(herramientas.toolA()).contains("tool-A");
        assertThat(herramientas.toolB()).contains("tool-B");
        assertThat(herramientas.toolC()).contains("tool-C");
    }
}
