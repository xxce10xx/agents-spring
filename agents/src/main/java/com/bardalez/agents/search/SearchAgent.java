package com.bardalez.agents.search;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

/**
 * Agent Search: atiende la rama informativa. Responde preguntas del empleado con la documentacion
 * corporativa indexada en Qdrant.
 *
 * <p>Es <strong>estrictamente de solo lectura</strong>: no produce ningun efecto sobre ningun
 * sistema, ni siquiera sobre la memoria. Por eso es el destino seguro ante intenciones ambiguas.
 *
 * <p>Lo llamativo de esta clase es lo poco que hace. El RAG y el guardrail estan declarados como
 * advisors en {@code SearchChatClientConfig}, asi que aqui no hay ninguna busqueda vectorial. Y la
 * memoria no se busca: <strong>llega ya recuperada</strong> desde el Router, que es su dueno.
 */
@Service
public class SearchAgent {

    private static final Logger log = LoggerFactory.getLogger(SearchAgent.class);

    private final ChatClient chatClient;

    public SearchAgent(ChatClient searchChatClient) {
        this.chatClient = searchChatClient;
    }

    /**
     * Responde la pregunta del empleado en lenguaje natural.
     *
     * @param prompt la pregunta, tal como la escribio el empleado
     * @param memoria el historial de la conversacion, recuperado por el Router. Puede venir vacio
     */
    public String responder(String prompt, List<Message> memoria) {
        log.debug("Search recibe la pregunta '{}' con {} mensajes de contexto", prompt, memoria.size());

        // La pregunta va SIN plantilla, al contrario que en el Router: el RAG usa el texto del
        // usuario como consulta para buscar en Qdrant, y envolverlo en instrucciones ensucia la
        // busqueda. Las instrucciones de Search viven en su system prompt, que es donde toca.
        //
        // messages() coloca la memoria antes de la pregunta, en el formato nativo de la API: una
        // lista de turnos user/assistant. El modelo la lee como conversacion previa, no como texto.
        String respuesta = chatClient.prompt()
                .messages(memoria)
                .user(prompt)
                .call()
                .content();

        log.debug("Respuesta de Search: {}", respuesta);
        return respuesta;
    }
}
