package com.bardalez.agents.router;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * La memoria se puede probar sin llamar al LLM: el advisor es solo el que la invoca, pero
 * {@link ChatMemory} y {@link ChatMemoryRepository} son beans normales.
 *
 * <p>En los tests el repositorio JDBC apunta a H2 en memoria (ver {@code src/test/resources}); en
 * ejecucion real apunta al MySQL configurado en {@code application.yaml}. El codigo es el mismo.
 */
@SpringBootTest
class ChatMemoryTest {

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private ChatMemoryRepository repository;

    @Test
    @DisplayName("Cada conversationId guarda su propio historial")
    void aislaLasConversaciones() {
        chatMemory.add("sesion-A", new UserMessage("¿cuantos dias de vacaciones me quedan?"));
        chatMemory.add("sesion-A", new AssistantMessage("INFORMATIVA"));
        chatMemory.add("sesion-B", new UserMessage("reinicia mi VPN"));

        assertThat(chatMemory.get("sesion-A")).hasSize(2);
        assertThat(chatMemory.get("sesion-B")).hasSize(1);

        // El repositorio es el almacen real: los mensajes ya estan en la tabla.
        assertThat(repository.findByConversationId("sesion-A")).hasSize(2);
        assertThat(repository.findConversationIds()).contains("sesion-A", "sesion-B");
    }

    @Test
    @DisplayName("clear() borra el historial de una conversacion")
    void borraElHistorial() {
        chatMemory.add("sesion-C", new UserMessage("hola"));
        assertThat(chatMemory.get("sesion-C")).hasSize(1);

        chatMemory.clear("sesion-C");

        assertThat(chatMemory.get("sesion-C")).isEmpty();
    }
}
