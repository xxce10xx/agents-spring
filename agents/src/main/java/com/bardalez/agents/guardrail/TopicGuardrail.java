package com.bardalez.agents.guardrail;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Guardrail 2: allowlist tematica, decidida por el LLM.
 *
 * <p>Contrapunto al {@link CanaryLeakAdvisor}: no todos los controles se pueden escribir con un
 * {@code if}. El alcance permitido incluye "y temas similares", y decidir si un tema es cercano a
 * "soporte de VPN" exige comprension semantica. Asi que este guardrail usa <strong>otro
 * ChatClient</strong> con su propio system prompt, que responde {@code PERMITIDO} o {@code BLOQUEADO}.
 *
 * <p>Se invoca desde el Router antes de clasificar la intencion.
 */
@Component
public class TopicGuardrail {

    private static final Logger log = LoggerFactory.getLogger(TopicGuardrail.class);

    private final ChatClient chatClient;
    private final PromptTemplate userPromptTemplate;

    public TopicGuardrail(ChatClient.Builder builder,
                          @Value("classpath:prompts/topic-guardrail-system.st") Resource systemPrompt,
                          @Value("classpath:prompts/topic-guardrail-user.st") Resource userPrompt) {
        this.chatClient = builder.defaultSystem(systemPrompt).build();
        this.userPromptTemplate = new PromptTemplate(userPrompt);
    }

    /**
     * @throws GuardrailViolationException si el tema queda fuera del alcance permitido
     */
    public void validar(String prompt) {
        String mensaje = userPromptTemplate.render(Map.of("prompt", prompt));

        String veredicto = chatClient.prompt().user(mensaje).call().content();
        log.debug("Veredicto del guardrail tematico: {}", veredicto);

        if (!"PERMITIDO".equalsIgnoreCase(veredicto.trim())) {
            throw new GuardrailViolationException("tematica", "tema fuera del alcance permitido");
        }
    }
}
