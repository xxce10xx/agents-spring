package com.bardalez.agents.router;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Configuracion del ChatClient propio del Agent Router.
 *
 * <p>Cada agente declara su propio {@link ChatClient} con su propio system prompt. Cuando se
 * incorporen Search, Process y Executor, cada uno tendra un bean equivalente en su paquete y se
 * inyectaran por nombre de bean.
 */
@Configuration
public class RouterChatClientConfig {

    /**
     * ChatClient del Router, preconfigurado con el system prompt del clasificador de intencion.
     */
    @Bean
    ChatClient routerChatClient(ChatClient.Builder builder,
                                @Value("classpath:prompts/router-system.st") Resource systemPrompt) {
        return builder
                .defaultSystem(systemPrompt)
                .build();
    }
}
