package com.bardalez.agents.router;

import java.util.Map;

import com.bardalez.agents.guardrail.CanaryLeakAdvisor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Configuracion del ChatClient propio del Agent Router.
 *
 * <p>Cada agente declara su propio {@link ChatClient} con su propio system prompt. Comparar este
 * bean con {@code SearchChatClientConfig} es instructivo: mismo builder, advisors distintos.
 *
 * <p>El Router <strong>no lleva memoria</strong>, y es deliberado. Es un clasificador: su trabajo es
 * mirar un mensaje y decir INFORMATIVA o ACCION. La conversacion la mantiene el Agent Search, que es
 * quien de verdad habla con el empleado.
 */
@Configuration
public class RouterChatClientConfig {

    @Bean
    ChatClient routerChatClient(ChatClient.Builder builder,
                                @Value("classpath:prompts/router-system.st") Resource systemPrompt) {

        // El system prompt es a su vez una plantilla: se le inyecta el token canary que el
        // CanaryLeakAdvisor buscara despues en la respuesta.
        String systemPromptRenderizado = new PromptTemplate(systemPrompt)
                .render(Map.of("canary", CanaryLeakAdvisor.CANARY));

        return builder
                .defaultSystem(systemPromptRenderizado)
                .defaultAdvisors(new CanaryLeakAdvisor())
                .build();
    }
}
