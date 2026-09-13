package com.bardalez.agents.router;

import java.util.Map;

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
 */
@Service
public class RouterAgent {

    private static final Logger log = LoggerFactory.getLogger(RouterAgent.class);

    private final ChatClient chatClient;
    private final PromptTemplate userPromptTemplate;

    public RouterAgent(ChatClient routerChatClient,
                       @Value("classpath:prompts/router-user.st") Resource userPrompt) {
        this.chatClient = routerChatClient;
        this.userPromptTemplate = new PromptTemplate(userPrompt);
    }

    /**
     * Clasifica la intencion de un prompt del empleado.
     *
     * @param prompt mensaje en lenguaje natural
     * @return la respuesta cruda del LLM (se espera {@code INFORMATIVA} o {@code ACCION})
     */
    public String clasificarIntencion(String prompt) {
        String mensajeRenderizado = userPromptTemplate.render(Map.of("prompt", prompt));
        log.debug("Prompt renderizado enviando al LLM: {}", mensajeRenderizado);

        String respuesta = chatClient.prompt()
                .user(mensajeRenderizado)
                .call()
                .content();

        log.debug("Intencion devuelta por el LLM: {}", respuesta);
        return respuesta;
    }
}
