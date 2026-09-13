package com.bardalez.agents.process;

import java.util.Map;

import com.bardalez.agents.guardrail.CanaryLeakAdvisor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Configuracion del ChatClient del Agent Process.
 *
 * <p>Es la mas sencilla de los tres agentes, y eso dice algo: aqui no hay RAG y no hay memoria. El
 * contexto que necesita el modelo no se busca, <strong>se le entrega</strong>: el catalogo lo trae
 * {@link CatalogoProcesos} por MCP y el historial lo pasa el Router. Solo queda el
 * {@link CanaryLeakAdvisor}, el mismo objeto que usan el Router y Search.
 *
 * <p>Tampoco hay herramientas registradas en el ChatClient. Las del servidor MCP se podrian exponer al
 * modelo con {@code spring.ai.mcp.client.toolcallback.enabled}, pero esta en {@code false} a
 * proposito: ver {@link CatalogoProcesos}.
 */
@Configuration
public class ProcessChatClientConfig {

    @Bean
    ChatClient processChatClient(ChatClient.Builder builder,
                                 @Value("classpath:prompts/process-system.st") Resource systemPrompt) {

        String systemPromptRenderizado = new PromptTemplate(systemPrompt)
                .render(Map.of("canary", CanaryLeakAdvisor.CANARY));

        return builder
                .defaultSystem(systemPromptRenderizado)
                .defaultAdvisors(new CanaryLeakAdvisor())
                .build();
    }
}
