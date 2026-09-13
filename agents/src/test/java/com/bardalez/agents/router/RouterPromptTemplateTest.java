package com.bardalez.agents.router;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;

/**
 * Verifica que las plantillas de prompt renderizan correctamente y, sobre todo, que un prompt de
 * usuario con llaves no rompe el renderizado. Es un riesgo real: si el motor de plantillas
 * reinterpretara el contenido inyectado, un usuario podria manipular la plantilla escribiendo
 * llaves en su mensaje.
 */
class RouterPromptTemplateTest {

    @Test
    void renderizaElPromptDeUsuario() {
        PromptTemplate plantilla = new PromptTemplate(new ClassPathResource("prompts/router-user.st"));

        String resultado = plantilla.render(Map.of("prompt", "¿Cuántos días de vacaciones tengo?"));

        assertTrue(resultado.contains("¿Cuántos días de vacaciones tengo?"));
    }

    @Test
    void toleraLlavesEnElPromptDelUsuario() {
        PromptTemplate plantilla = new PromptTemplate(new ClassPathResource("prompts/router-user.st"));

        String resultado = plantilla.render(Map.of("prompt", "dame {vacaciones} y {{otra_cosa}}"));

        assertTrue(resultado.contains("vacaciones"));
    }

    @Test
    void renderizaElCanaryEnElSystemPrompt() {
        PromptTemplate plantilla = new PromptTemplate(new ClassPathResource("prompts/router-system.st"));

        String resultado = plantilla.render(Map.of("canary", "SPRINTAI-CANARY-TEST0000"));

        assertTrue(resultado.contains("SPRINTAI-CANARY-TEST0000"));
    }
}
