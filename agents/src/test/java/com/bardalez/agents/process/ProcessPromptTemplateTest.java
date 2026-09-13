package com.bardalez.agents.process;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;

/**
 * Renderizado de las plantillas del Agent Process.
 *
 * <p>Aqui hay mas riesgo que en el Router, porque en la plantilla no se inyecta solo el mensaje del
 * empleado: tambien el catalogo que llego por MCP. Son dos entradas que este proyecto no controla, y
 * el motor de plantillas usa la llave como delimitador.
 */
class ProcessPromptTemplateTest {

    @Test
    @DisplayName("El prompt de usuario lleva el catalogo y el mensaje del empleado")
    void renderizaElCatalogoYElPrompt() {
        PromptTemplate plantilla = new PromptTemplate(new ClassPathResource("prompts/process-user.st"));

        String resultado = plantilla.render(Map.of(
                "catalogo", "- pedir vacaciones (tipo: vacaciones). Solicitud de vacaciones. Pasos: tool-A, tool-B",
                "prompt", "Quiero pedir vacaciones para el 27"));

        assertTrue(resultado.contains("pedir vacaciones"));
        assertTrue(resultado.contains("tool-B"));
        assertTrue(resultado.contains("Quiero pedir vacaciones para el 27"));
    }

    @Test
    @DisplayName("Ni el mensaje ni el catalogo rompen la plantilla si traen llaves")
    void toleraLlavesEnLosValoresInyectados() {
        PromptTemplate plantilla = new PromptTemplate(new ClassPathResource("prompts/process-user.st"));

        // La secuencia se guarda como JSON, asi que un catalogo con llaves es un escenario real, no
        // rebuscado. Por eso ProcessAgent lo renderiza como texto plano.
        String resultado = plantilla.render(Map.of(
                "catalogo", "- proceso raro. Pasos: {\"paso 1\": \"tool-A\"}",
                "prompt", "ejecuta {sudo} y {{lo otro}}"));

        assertTrue(resultado.contains("tool-A"));
        assertTrue(resultado.contains("sudo"));
    }

    @Test
    @DisplayName("El system prompt renderiza el canary")
    void renderizaElCanaryEnElSystemPrompt() {
        PromptTemplate plantilla = new PromptTemplate(new ClassPathResource("prompts/process-system.st"));

        String resultado = plantilla.render(Map.of("canary", "SPRINTAI-CANARY-TEST0000"));

        assertTrue(resultado.contains("SPRINTAI-CANARY-TEST0000"));
    }
}
