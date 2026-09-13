package com.bardalez.agents.executor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.bardalez.agents.executor.dto.Ejecucion;
import com.bardalez.agents.guardrail.GuardrailViolationException;
import com.bardalez.agents.process.dto.Procedimiento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Agent Executor: el unico agente del sistema que produce efectos. Recibe un procedimiento ya resuelto
 * por Process e invoca sus tools en el orden que dicta la secuencia.
 *
 * <p><strong>No decide el orden: lo obedece.</strong> Y no decide tampoco que herramientas existen: las
 * tres tools estan registradas en {@link ExecutorChatClientConfig}, y los pasos vienen de una fila de
 * base de datos que copio {@code ProcessAgent}. El modelo solo pone las llamadas en marcha; el "que"
 * ya venia decidido cuando llego aqui.
 *
 * <p><strong>Solo se invoca despues de Process.</strong> Es el invariante de seguridad del proyecto: no
 * existe ningun camino para ejecutar tools sin una definicion previa en base de datos. El invariante se
 * sostiene en dos sitios: el Router solo llama a este metodo dentro de la rama donde Process encontro
 * un procedimiento, y la firma pide un {@link Procedimiento} —no una lista de cadenas— de modo que
 * quien quisiera saltarse Process tendria que fabricarse uno a mano. Si aun asi llega uno vacio, salta
 * una excepcion: es un fallo de programacion, no un resultado de negocio.
 *
 * <p>Aparte de eso, <strong>nunca propaga una excepcion al Router</strong>. El contrato de salida es
 * siempre un {@link Ejecucion} con su {@code estado}, porque cuando ya se han producido efectos lo
 * peor que se puede hacer es devolver un error generico sin decir que se alcanzo a hacer.
 *
 * <p>Aqui no llega la memoria de la conversacion, y es deliberado: cuando este agente entra en escena
 * la decision ya esta tomada. Darle el historial solo abriria la puerta a que reinterpretase el
 * procedimiento a la luz de algo que dijo el empleado tres turnos antes.
 */
@Service
public class ExecutorAgent {

    private static final Logger log = LoggerFactory.getLogger(ExecutorAgent.class);

    private final ChatClient chatClient;
    private final PromptTemplate userPromptTemplate;

    public ExecutorAgent(ChatClient executorChatClient,
                         @Value("classpath:prompts/executor-user.st") Resource userPrompt) {
        this.chatClient = executorChatClient;
        this.userPromptTemplate = new PromptTemplate(userPrompt);
    }

    /**
     * Ejecuta el procedimiento que resolvio el Agent Process.
     *
     * @param procedimiento el que devolvio {@code ProcessAgent}, con {@code encontrado == true} y al
     *                      menos un paso
     * @return el informe de la ejecucion, nunca {@code null}
     * @throws IllegalArgumentException si el procedimiento no viene de una resolucion valida de
     *                                  Process. No es un caso de negocio: es el invariante roto
     */
    public Ejecucion ejecutar(Procedimiento procedimiento) {
        if (procedimiento == null || !procedimiento.encontrado() || procedimiento.pasos().isEmpty()) {
            throw new IllegalArgumentException(
                    "El Agent Executor solo se puede invocar con un procedimiento encontrado por el "
                            + "Agent Process y con al menos un paso");
        }

        List<String> pasos = procedimiento.pasos();
        log.info("Executor: arranca '{}' con {} pasos: {}",
                procedimiento.proceso(), pasos.size(), String.join(" -> ", pasos));

        String mensajeRenderizado = userPromptTemplate.render(Map.of(
                "procedimiento", procedimiento.proceso(),
                "pasos", enumerar(pasos),
                "herramientas", String.join(", ", HerramientasFicticias.NOMBRES)));

        try {
            // Un solo call() para las dos cosas: dentro de el, Spring AI ejecuta todas las tools que
            // el modelo vaya pidiendo, y solo cuando el modelo deja de pedir llamadas se deserializa
            // su ultima respuesta en el informe. El bucle de tool calling no se ve desde aqui.
            Ejecucion ejecucion = chatClient.prompt()
                    .user(mensajeRenderizado)
                    .call()
                    .entity(Ejecucion.class);

            if (ejecucion == null) {
                log.error("Executor: el modelo no devolvio informe para '{}'", procedimiento.proceso());
                return Ejecucion.errorTecnico(pasos, "el modelo no devolvio ningun informe");
            }

            log.info("Executor: '{}' termino con estado {} ({} pasos correctos, {} fallidos)",
                    procedimiento.proceso(), ejecucion.estado(),
                    ejecucion.pasosExitosos().size(), ejecucion.pasosFallidos().size());
            return ejecucion;

        } catch (GuardrailViolationException e) {
            // El guardrail del canary vive en un Advisor de este mismo ChatClient, asi que su
            // excepcion aparece por aqui. Esta si sube: un bloqueo de seguridad tiene que llegar al
            // empleado como un 403, no disfrazado de "no se pudo ejecutar el paso 2".
            throw e;

        } catch (RuntimeException e) {
            // Cualquier otro fallo -por ejemplo, que OpenAI no responda- se convierte en informe. El
            // Router siempre recibe el mismo contrato, y el motivo real queda en el log.
            log.error("Executor: la ejecucion de '{}' se rompio: {}",
                    procedimiento.proceso(), e.getMessage(), e);
            return Ejecucion.errorTecnico(pasos, e.getMessage());
        }
    }

    /**
     * Convierte la lista de pasos en el texto numerado que lee el modelo.
     *
     * <p>Los numeros no son cosmeticos: son las posiciones que el informe tiene que devolver en el
     * campo {@code paso}, y son lo que hace distinguibles dos apariciones de la misma herramienta
     * dentro de un procedimiento.
     */
    private String enumerar(List<String> pasos) {
        return IntStream.range(0, pasos.size())
                .mapToObj(i -> "%d. %s".formatted(i + 1, pasos.get(i)))
                .collect(Collectors.joining("\n"));
    }
}
