package com.bardalez.agents.router;

import java.util.Map;

import com.bardalez.agents.guardrail.CanaryLeakAdvisor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Configuracion del ChatClient propio del Agent Router.
 *
 * <p>Cada agente declara su propio {@link ChatClient} con su propio system prompt. Cuando se
 * incorporen Search, Process y Executor, cada uno tendra un bean equivalente en su paquete.
 *
 * <p>Aqui se ve el patron central del curso: <strong>dos advisors</strong> registrados en el mismo
 * cliente, uno propio y uno de serie, y el agente no invoca ninguno de los dos.
 */
@Configuration
public class RouterChatClientConfig {

    /** Cuantos mensajes de historial se envian al modelo en cada llamada. */
    private static final int MENSAJES_RECORDADOS = 20;

    /**
     * La memoria de conversacion.
     *
     * <p>Son dos piezas y conviene no confundirlas:
     *
     * <ul>
     *   <li>{@link ChatMemoryRepository} es el <strong>almacen</strong>: solo sabe guardar y leer
     *       mensajes por {@code conversationId}. Spring AI lo autoconfigura como
     *       {@code JdbcChatMemoryRepository} sobre el {@code DataSource} de MySQL, gracias a la
     *       dependencia {@code spring-ai-starter-model-chat-memory-repository-jdbc}.</li>
     *   <li>{@link ChatMemory} es la <strong>politica</strong>: decide que parte de ese historial se
     *       le manda al modelo. {@link MessageWindowChatMemory} usa una ventana deslizante: conserva
     *       los ultimos N mensajes y descarta los mas viejos, para no crecer sin limite.</li>
     * </ul>
     *
     * <p>Spring AI ya crea este bean por su cuenta; se declara explicitamente para que la relacion
     * entre las dos piezas quede a la vista.
     */
    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(MENSAJES_RECORDADOS)
                .build();
    }

    @Bean
    ChatClient routerChatClient(ChatClient.Builder builder,
                                @Value("classpath:prompts/router-system.st") Resource systemPrompt,
                                ChatMemory chatMemory) {

        // El system prompt es a su vez una plantilla: se le inyecta el token canary que el
        // CanaryLeakAdvisor buscara despues en la respuesta.
        String systemPromptRenderizado = new PromptTemplate(systemPrompt)
                .render(Map.of("canary", CanaryLeakAdvisor.CANARY));

        return builder
                .defaultSystem(systemPromptRenderizado)
                .defaultAdvisors(
                        // Advisor de serie: en before() recupera el historial de la conversacion y lo
                        // añade a la peticion; en after() guarda el nuevo par pregunta/respuesta.
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // Advisor propio: en after() revisa que el canary no se haya filtrado.
                        new CanaryLeakAdvisor())
                .build();
    }
}
