package com.bardalez.agents.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La memoria de conversacion del sistema.
 *
 * <p>La usa el {@code RouterAgent}, que es su dueno, y la consulta el {@code RouterController} para
 * poder inspeccionarla en clase. Tiene paquete propio porque no es de nadie mas: los agentes
 * destino reciben la memoria como un dato, no como una dependencia.
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
@Configuration
public class ChatMemoryConfig {

    /** Cuantos mensajes de historial se envian al modelo en cada llamada. */
    private static final int MENSAJES_RECORDADOS = 20;

    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(MENSAJES_RECORDADOS)
                .build();
    }
}
