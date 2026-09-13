package com.bardalez.agents.guardrail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/**
 * Un Advisor se puede probar sin arrancar Spring ni llamar al LLM: basta fabricar la respuesta e
 * invocar {@code after()} directamente.
 */
class CanaryLeakAdvisorTest {

    private final CanaryLeakAdvisor advisor = new CanaryLeakAdvisor();

    private static ChatClientResponse respuestaCon(String texto) {
        return new ChatClientResponse(
                new ChatResponse(List.of(new Generation(new AssistantMessage(texto)))), Map.of());
    }

    @Test
    @DisplayName("Bloquea si el canary aparece en la respuesta")
    void bloqueaLaFuga() {
        ChatClientResponse response = respuestaCon("Mi token es " + CanaryLeakAdvisor.CANARY);

        assertThatThrownBy(() -> advisor.after(response, null))
                .isInstanceOf(GuardrailViolationException.class);
    }

    @Test
    @DisplayName("Deja pasar una respuesta normal del clasificador")
    void permiteRespuestaNormal() {
        ChatClientResponse response = respuestaCon("INFORMATIVA");

        assertThat(advisor.after(response, null)).isSameAs(response);
    }
}
