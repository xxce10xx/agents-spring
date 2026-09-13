package com.bardalez.agents.router;

import java.util.List;
import java.util.Map;

import com.bardalez.agents.guardrail.TopicGuardrail;
import com.bardalez.agents.router.dto.Resultado;
import com.bardalez.agents.search.SearchAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Agent Router: hub del sistema. Clasifica la intencion del prompt del empleado y enruta al agente
 * que corresponda.
 *
 * <p>Los dos guardrails del proyecto se ven aqui, cada uno con un mecanismo distinto:
 *
 * <ul>
 *   <li>El <strong>tematico</strong> se invoca explicitamente, porque necesita al LLM para decidir.</li>
 *   <li>El del <strong>canary</strong> no aparece: es un Advisor registrado en el ChatClient, asi que
 *       Spring AI lo ejecuta solo alrededor de la llamada al modelo.</li>
 * </ul>
 *
 * <p><strong>El Router es el dueno de la memoria de conversacion</strong>, y tiene que serlo: es el
 * unico componente que conoce la sesion del empleado y el unico por el que pasan las dos ramas. Si
 * la memoria viviera en el Agent Search, la rama de accion (Process y Executor) se quedaria sin
 * conversacion. Aqui la memoria se recupera una vez y sirve para dos cosas: dar contexto al
 * clasificador y viajar al agente destino como un dato mas.
 */
@Service
public class RouterAgent {

    private static final Logger log = LoggerFactory.getLogger(RouterAgent.class);

    private static final String INFORMATIVA = "INFORMATIVA";
    private static final String ACCION = "ACCION";

    private final ChatClient chatClient;
    private final PromptTemplate userPromptTemplate;
    private final TopicGuardrail topicGuardrail;
    private final SearchAgent searchAgent;
    private final ChatMemory chatMemory;

    public RouterAgent(ChatClient routerChatClient,
                       @Value("classpath:prompts/router-user.st") Resource userPrompt,
                       TopicGuardrail topicGuardrail,
                       SearchAgent searchAgent,
                       ChatMemory chatMemory) {
        this.chatClient = routerChatClient;
        this.userPromptTemplate = new PromptTemplate(userPrompt);
        this.topicGuardrail = topicGuardrail;
        this.searchAgent = searchAgent;
        this.chatMemory = chatMemory;
    }

    /**
     * Atiende una peticion de principio a fin: guardrail, memoria, clasificacion y enrutamiento.
     *
     * @param prompt mensaje en lenguaje natural
     * @param sessionId identificador de la conversacion; con el se recupera y se guarda la memoria
     * @throws com.bardalez.agents.guardrail.GuardrailViolationException si un guardrail bloquea
     */
    public Resultado atender(String prompt, String sessionId) {
        topicGuardrail.validar(prompt);

        // La memoria se busca aqui, en el hub. Se recupera una sola vez por peticion.
        List<Message> memoria = chatMemory.get(sessionId);
        log.debug("Sesion {}: {} mensajes en memoria", sessionId, memoria.size());

        String intencion = clasificarIntencion(prompt, memoria);

        if (ACCION.equals(intencion)) {
            // TODO: enrutar al Agent Process, que recuperara la definicion del procedimiento.
            log.debug("Rama de accion: el Agent Process todavia no esta implementado");
            return new Resultado(intencion, null);
        }

        // Rama informativa: se delega en Search, que hace RAG sobre la documentacion corporativa.
        // Se le pasa la pregunta y la memoria ya recuperada; Search no toca la base de datos.
        log.debug("Rama informativa: se enruta a Search");
        String respuesta = searchAgent.responder(prompt, memoria);

        // Y el Router cierra el turno guardandolo. Lo que queda en MySQL es la conversacion real:
        // la pregunta tal como la escribio el empleado y la respuesta tal como la va a leer.
        chatMemory.add(sessionId, List.of(new UserMessage(prompt), new AssistantMessage(respuesta)));

        return new Resultado(intencion, respuesta);
    }

    /**
     * Clasifica la intencion del mensaje llamando al LLM.
     *
     * <p>Devuelve siempre {@code INFORMATIVA} o {@code ACCION}. Cualquier otra cosa que responda el
     * modelo se trata como {@code INFORMATIVA}: es la rama de solo lectura, la que no produce
     * efectos, y por tanto la opcion segura ante la duda.
     *
     * <p>La memoria tambien se le pasa al clasificador, porque hay mensajes que aislados no se
     * pueden clasificar: "y tambien el 27" solo es una accion si antes se pidio reservar algo.
     */
    private String clasificarIntencion(String prompt, List<Message> memoria) {
        String mensajeRenderizado = userPromptTemplate.render(Map.of("prompt", prompt));
        log.debug("Prompt renderizado enviado al LLM: {}", mensajeRenderizado);

        // El CanaryLeakAdvisor se ejecuta dentro de este call(), sin invocarlo explicitamente.
        String respuesta = chatClient.prompt()
                .messages(memoria)
                .user(mensajeRenderizado)
                .call()
                .content();

        log.debug("Intencion devuelta por el LLM: {}", respuesta);

        String normalizada = respuesta == null ? "" : respuesta.trim().toUpperCase();
        if (!ACCION.equals(normalizada) && !INFORMATIVA.equals(normalizada)) {
            log.warn("El clasificador devolvio algo inesperado ('{}'); se asume INFORMATIVA", respuesta);
            return INFORMATIVA;
        }
        return normalizada;
    }
}
