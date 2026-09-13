package com.bardalez.agents.executor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;

/**
 * Renderizado de las plantillas del Agent Executor.
 *
 * <p>La plantilla de usuario inyecta tres valores, y dos de ellos —el nombre del procedimiento y sus
 * pasos— vienen de una fila de base de datos que administra otra persona en otro repositorio. Si el
 * renderizado se rompe con lo que traiga esa fila, el modelo recibe un procedimiento a medias y ejecuta
 * pasos de verdad.
 */
class ExecutorPromptTemplateTest {

    @Test
    @DisplayName("El prompt de usuario lleva el procedimiento, los pasos numerados y las tools")
    void renderizaElProcedimientoYLosPasos() {
        PromptTemplate plantilla = new PromptTemplate(new ClassPathResource("prompts/executor-user.st"));

        String resultado = plantilla.render(Map.of(
                "procedimiento", "pedir vacaciones",
                "pasos", "1. tool-A\n2. tool-B",
                "herramientas", "tool-A, tool-B, tool-C"));

        assertTrue(resultado.contains("pedir vacaciones"));
        assertTrue(resultado.contains("1. tool-A"));
        assertTrue(resultado.contains("2. tool-B"));
        assertTrue(resultado.contains("tool-C"));
    }

    @Test
    @DisplayName("El nombre del procedimiento no rompe la plantilla si trae llaves")
    void toleraLlavesEnLosValoresInyectados() {
        PromptTemplate plantilla = new PromptTemplate(new ClassPathResource("prompts/executor-user.st"));

        // El nombre del procedimiento es un texto libre de la tabla procesos: nadie garantiza que no
        // lleve llaves, y el motor de plantillas las usa como delimitador.
        String resultado = plantilla.render(Map.of(
                "procedimiento", "proceso {raro} con {{llaves}}",
                "pasos", "1. tool-A",
                "herramientas", "tool-A"));

        assertTrue(resultado.contains("raro"));
        assertTrue(resultado.contains("1. tool-A"));
    }

    @Test
    @DisplayName("El system prompt renderiza el canary")
    void renderizaElCanaryEnElSystemPrompt() {
        PromptTemplate plantilla = new PromptTemplate(new ClassPathResource("prompts/executor-system.st"));

        String resultado = plantilla.render(Map.of("canary", "SPRINTAI-CANARY-TEST0000"));

        assertTrue(resultado.contains("SPRINTAI-CANARY-TEST0000"));
    }
}
