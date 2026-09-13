package com.bardalez.agents.process;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bardalez.agents.process.dto.Eleccion;
import com.bardalez.agents.process.dto.Procedimiento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Agent Process: atiende la rama de accion. Decide que procedimiento de la empresa corresponde a lo
 * que pidio el empleado y devuelve sus pasos en orden.
 *
 * <p>El ciclo completo son tres movimientos:
 *
 * <ol>
 *   <li>Pide el catalogo entero al servidor MCP ({@link CatalogoProcesos}).</li>
 *   <li>Se lo manda al LLM junto con la memoria que recibio del Router, y el modelo elige uno.</li>
 *   <li>Busca el nombre elegido en el catalogo y copia de alli la secuencia de pasos.</li>
 * </ol>
 *
 * <p>El tercer paso es la parte importante y no es obvia: <strong>el LLM decide, pero no dicta</strong>.
 * Podria haber devuelto los pasos directamente —los tiene delante— y habria funcionado casi siempre.
 * Casi siempre no basta cuando el siguiente eslabon va a ejecutar esos pasos: una herramienta
 * inventada es indistinguible de una real hasta que se intenta ejecutar. Al copiarlos de la fila que
 * vino por MCP, lo peor que puede pasar es elegir el procedimiento equivocado, que es un fallo
 * visible; y si el modelo devuelve un nombre que no esta en el catalogo, se trata como no encontrado.
 *
 * <p>Como Search, este agente <strong>no busca la memoria</strong>: le llega ya recuperada desde el
 * Router, que es su dueno.
 */
@Service
public class ProcessAgent {

    private static final Logger log = LoggerFactory.getLogger(ProcessAgent.class);

    private final ChatClient chatClient;
    private final PromptTemplate userPromptTemplate;
    private final CatalogoProcesos catalogoProcesos;

    public ProcessAgent(ChatClient processChatClient,
                        @Value("classpath:prompts/process-user.st") Resource userPrompt,
                        CatalogoProcesos catalogoProcesos) {
        this.chatClient = processChatClient;
        this.userPromptTemplate = new PromptTemplate(userPrompt);
        this.catalogoProcesos = catalogoProcesos;
    }

    /**
     * Resuelve que procedimiento aplica al mensaje del empleado.
     *
     * @param prompt la peticion, tal como la escribio el empleado
     * @param memoria el historial de la conversacion, recuperado por el Router. Puede venir vacio
     * @return el procedimiento con sus pasos, o {@link Procedimiento#noEncontrado()}
     */
    public Procedimiento resolver(String prompt, List<Message> memoria) {
        List<Proceso> catalogo = catalogoProcesos.listar();

        if (catalogo.isEmpty()) {
            // Sin catalogo no hay nada que elegir: preguntarle al LLM seria invitarle a inventar.
            log.warn("El catalogo de procedimientos esta vacio: no hay nada que decidir");
            return Procedimiento.noEncontrado();
        }

        String mensajeRenderizado = userPromptTemplate.render(Map.of(
                "catalogo", describir(catalogo),
                "prompt", prompt));
        log.debug("Process consulta al LLM con {} procedimientos y {} mensajes de contexto",
                catalogo.size(), memoria.size());

        // entity() en lugar de content(): Spring AI anade a la peticion el esquema JSON de Eleccion y
        // deserializa la respuesta. Asi "no lo encontre" llega como un booleano y no como una frase
        // que habria que interpretar.
        Eleccion eleccion = chatClient.prompt()
                .messages(memoria)
                .user(mensajeRenderizado)
                .call()
                .entity(Eleccion.class);

        if (eleccion == null || !eleccion.encontrado()) {
            log.debug("El LLM no encontro procedimiento. Motivo: {}",
                    eleccion == null ? "respuesta vacia" : eleccion.motivo());
            return Procedimiento.noEncontrado();
        }

        log.debug("El LLM eligio '{}'. Motivo: {}", eleccion.proceso(), eleccion.motivo());

        // El nombre elegido tiene que existir en el catalogo. Si no existe, el modelo se lo invento:
        // no es un procedimiento de la empresa, asi que no se ejecuta.
        return catalogo.stream()
                .filter(proceso -> proceso.proceso().equalsIgnoreCase(eleccion.proceso()))
                .findFirst()
                .map(proceso -> new Procedimiento(true, proceso.proceso(), proceso.tipo(),
                        catalogoProcesos.pasosDe(proceso)))
                .orElseGet(() -> {
                    log.warn("El LLM devolvio '{}', que no esta en el catalogo: se trata como no encontrado",
                            eleccion.proceso());
                    return Procedimiento.noEncontrado();
                });
    }

    /**
     * Convierte el catalogo en el texto que lee el modelo.
     *
     * <p>Una linea por procedimiento, con la descripcion —que es el campo con el que de verdad se
     * decide— y los pasos ya traducidos a lista. Se envia como texto plano y no como JSON por dos
     * razones: gasta menos tokens, y sobre todo no lleva llaves. El motor de plantillas usa
     * {@code llave} como delimitador, asi que meter JSON en un valor inyectado es jugar con fuego sin
     * ninguna ventaja.
     */
    private String describir(List<Proceso> catalogo) {
        return catalogo.stream()
                .map(proceso -> "- %s (tipo: %s). %s Pasos: %s".formatted(
                        proceso.proceso(),
                        proceso.tipo(),
                        proceso.descripcion(),
                        String.join(", ", catalogoProcesos.pasosDe(proceso))))
                .collect(Collectors.joining("\n"));
    }
}
