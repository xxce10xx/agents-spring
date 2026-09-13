package com.bardalez.agents.router;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bardalez.agents.executor.ExecutorAgent;
import com.bardalez.agents.executor.dto.Ejecucion;
import com.bardalez.agents.executor.dto.EstadoEjecucion;
import com.bardalez.agents.executor.dto.PasoEjecutado;
import com.bardalez.agents.guardrail.TopicGuardrail;
import com.bardalez.agents.process.ProcessAgent;
import com.bardalez.agents.process.dto.Procedimiento;
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
    private final ProcessAgent processAgent;
    private final ExecutorAgent executorAgent;
    private final ChatMemory chatMemory;

    public RouterAgent(ChatClient routerChatClient,
                       @Value("classpath:prompts/router-user.st") Resource userPrompt,
                       TopicGuardrail topicGuardrail,
                       SearchAgent searchAgent,
                       ProcessAgent processAgent,
                       ExecutorAgent executorAgent,
                       ChatMemory chatMemory) {
        this.chatClient = routerChatClient;
        this.userPromptTemplate = new PromptTemplate(userPrompt);
        this.topicGuardrail = topicGuardrail;
        this.searchAgent = searchAgent;
        this.processAgent = processAgent;
        this.executorAgent = executorAgent;
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

        // Las dos ramas reciben lo mismo: el prompt y la memoria ya recuperada. Ninguno de los dos
        // agentes conoce la sesion ni toca la base de datos de conversacion.
        String respuesta = ACCION.equals(intencion)
                ? atenderAccion(prompt, memoria)
                : atenderInformativa(prompt, memoria);

        // Y el Router cierra el turno guardandolo, venga de donde venga la respuesta. Lo que queda en
        // MySQL es la conversacion real: la pregunta tal como la escribio el empleado y la respuesta
        // tal como la va a leer. Que el guardado este aqui, fuera del if, es justo el motivo de que la
        // memoria sea del Router: los dos agentes se benefician sin saber que existe.
        chatMemory.add(sessionId, List.of(new UserMessage(prompt), new AssistantMessage(respuesta)));

        return new Resultado(intencion, respuesta);
    }

    /**
     * Rama informativa: se delega en Search, que hace RAG sobre la documentacion corporativa.
     */
    private String atenderInformativa(String prompt, List<Message> memoria) {
        log.debug("Rama informativa: se enruta a Search");
        return searchAgent.responder(prompt, memoria);
    }

    /**
     * Rama de accion: dos agentes en cadena. Process dice <em>que</em> hay que hacer y Executor lo
     * hace.
     *
     * <p>Ninguno de los dos devuelve texto: Process devuelve un {@link Procedimiento} y Executor un
     * {@link Ejecucion}. Redactar la respuesta al empleado es trabajo del Router, aqui abajo, en
     * {@link #redactar}: los agentes especializados devuelven datos y el hub decide como se cuentan.
     */
    private String atenderAccion(String prompt, List<Message> memoria) {
        log.debug("Rama de accion: se enruta a Process");
        Procedimiento procedimiento = processAgent.resolver(prompt, memoria);

        if (!procedimiento.encontrado()) {
            // No hay reintento ni fallback a Search: si el procedimiento no existe, no existe. Y sobre
            // todo, se sale por aqui: el Executor no se llega a nombrar.
            log.debug("Process no encontro ningun procedimiento aplicable");
            return "No tengo ningún procedimiento definido para eso, así que no puedo tramitarlo. "
                    + "Si crees que debería existir, escríbele a la mesa de ayuda.";
        }

        log.debug("Process resolvio el procedimiento '{}' con {} pasos",
                procedimiento.proceso(), procedimiento.pasos().size());

        // La unica invocacion al Executor de todo el proyecto, y esta dentro de la rama en la que
        // Process encontro un procedimiento: es el invariante de seguridad escrito como control de
        // flujo. Lo que se le pasa es el procedimiento entero, con su secuencia de pasos ya copiada
        // del catalogo, asi que el Executor no elige que ejecutar; solo ejecuta.
        Ejecucion ejecucion = executorAgent.ejecutar(procedimiento);

        return redactar(procedimiento, ejecucion);
    }

    /**
     * Convierte el informe del Executor en la respuesta que lee el empleado.
     *
     * <p>El informe es un JSON con dos listas y un estado; lo que necesita el empleado es saber si su
     * tramite quedo hecho. Los dos casos se cuentan distinto a proposito: un procedimiento a medias no
     * se puede resumir con un "listo", porque lo importante es justo lo que falto.
     */
    private String redactar(Procedimiento procedimiento, Ejecucion ejecucion) {
        if (ejecucion.estado() == EstadoEjecucion.EXITOSO) {
            return """
                    Listo, ya tramité «%s». %s

                    Pasos ejecutados:
                    %s""".formatted(
                    procedimiento.proceso(),
                    ejecucion.resumen(),
                    listar(ejecucion.pasosExitosos()));
        }

        String pendientes = ejecucion.pasosFallidos().isEmpty()
                ? ""
                : "\n\nNo se pudo completar:\n" + listar(ejecucion.pasosFallidos());

        String hechos = ejecucion.pasosExitosos().isEmpty()
                ? ""
                : "\n\nSí quedó hecho:\n" + listar(ejecucion.pasosExitosos());

        return "No pude completar «%s». %s%s%s".formatted(
                procedimiento.proceso(), ejecucion.resumen(), hechos, pendientes);
    }

    /** Una linea por paso, con su numero, su herramienta y el detalle que escribio el Executor. */
    private String listar(List<PasoEjecutado> pasos) {
        return pasos.stream()
                .map(paso -> "%d. %s — %s".formatted(paso.paso(), paso.herramienta(), paso.detalle()))
                .collect(Collectors.joining("\n"));
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
