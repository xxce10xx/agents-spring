package com.bardalez.agents.router;

import java.util.Map;

import com.bardalez.agents.guardrail.TopicGuardrail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Agent Router: hub del sistema. Por ahora solo clasifica la intencion del prompt del usuario
 * invocando al LLM; el enrutamiento hacia Search, Process y Executor llegara en pasos posteriores.
 *
 * <p>Los dos guardrails del proyecto se ven aqui, cada uno con un mecanismo distinto:
 *
 * <ul>
 *   <li>El <strong>tematico</strong> se invoca explicitamente, porque necesita al LLM para decidir.</li>
 *   <li>El del <strong>canary</strong> no aparece: es un Advisor registrado en el ChatClient, asi que
 *       Spring AI lo ejecuta solo alrededor de la llamada al modelo.</li>
 * </ul>
 */
@Service
public class RouterAgent {

    private static final Logger log = LoggerFactory.getLogger(RouterAgent.class);

    private final ChatClient chatClient;
    private final PromptTemplate userPromptTemplate;
    private final TopicGuardrail topicGuardrail;

    public RouterAgent(ChatClient routerChatClient,
                       @Value("classpath:prompts/router-user.st") Resource userPrompt,
                       TopicGuardrail topicGuardrail) {
        this.chatClient = routerChatClient;
        this.userPromptTemplate = new PromptTemplate(userPrompt);
        this.topicGuardrail = topicGuardrail;
    }

    /**
     * Clasifica la intencion de un prompt del empleado.
     *
     * @param prompt mensaje en lenguaje natural
     * @return la respuesta cruda del LLM (se espera {@code INFORMATIVA} o {@code ACCION})
     * @throws com.bardalez.agents.guardrail.GuardrailViolationException si un guardrail bloquea
     */
    public String clasificarIntencion(String prompt) {
        topicGuardrail.validar(prompt);

        String mensajeRenderizado = userPromptTemplate.render(Map.of("prompt", prompt));
        log.debug("Prompt renderizado enviando al LLM: {}", mensajeRenderizado);

        // El CanaryLeakAdvisor se ejecuta dentro de este call(), sin invocarlo explicitamente.
        String respuesta = chatClient.prompt()
                .user(mensajeRenderizado)
                .call()
                .content();

        log.debug("Intencion devuelta por el LLM: {}", respuesta);
        return respuesta;
    }
}
