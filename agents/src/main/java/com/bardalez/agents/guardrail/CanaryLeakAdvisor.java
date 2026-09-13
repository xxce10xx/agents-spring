package com.bardalez.agents.guardrail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

/**
 * Guardrail 1: implementado como <strong>Advisor de Spring AI</strong>.
 *
 * <p>Este es el ejemplo del curso. Un {@code Advisor} es un interceptor de las llamadas al modelo:
 * Spring AI lo ejecuta alrededor de cada {@code chatClient.prompt()...call()}, sin que el codigo del
 * agente tenga que acordarse de nada. {@link BaseAdvisor} expone dos ganchos:
 *
 * <ul>
 *   <li>{@code before(...)} - se ejecuta con la peticion, antes de llamar al modelo.</li>
 *   <li>{@code after(...)} - se ejecuta con la respuesta, antes de devolverla al agente.</li>
 * </ul>
 *
 * <p>Aqui solo hace falta el segundo. El system prompt del Router contiene el {@link #CANARY} y la
 * orden de no revelarlo nunca; si aparece en la respuesta, es prueba de que el system prompt se
 * filtro y la respuesta no debe salir.
 *
 * <p>Se registra en {@code RouterChatClientConfig} con {@code .defaultAdvisors(...)}.
 *
 * <p><strong>Nota para la clase.</strong> En la practica este guardrail casi nunca salta, y eso es
 * correcto: es un detector de ultimo recurso. Tres cosas se lo impiden, por este orden:
 *
 * <ol>
 *   <li>El {@link TopicGuardrail} corre antes y bloquea todo intento de inyeccion por estar fuera de
 *       tema, asi que el modelo del Router ni se llega a invocar.</li>
 *   <li>El system prompt obliga a responder una sola palabra, {@code INFORMATIVA} o {@code ACCION}.</li>
 *   <li>{@code temperature: 0.0} mas la orden explicita de no revelar el token.</li>
 * </ol>
 *
 * <p>Para <em>ver</em> el advisor en accion sin montar un ataque, los logs DEBUG de {@code before} y
 * {@code after} se imprimen en cada llamada. Ver tambien la seccion del README sobre como forzar el
 * bloqueo en una demo.
 */
public class CanaryLeakAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(CanaryLeakAdvisor.class);

    /**
     * Token secreto que se inyecta en el system prompt del Router.
     *
     * <p>Truco de demo: cambia este valor por {@code "INFORMATIVA"} y cualquier consulta legitima
     * disparara el bloqueo, porque es justo lo que el clasificador responde. Sirve para comprobar en
     * un solo paso que el advisor intercepta de verdad. Recuerda revertirlo despues.
     */
    public static final String CANARY = "SPRINTAI-CANARY-7F3A9B2C";
    //public static final String CANARY = "INFORMATIVA";

    /** Un ChatClient puede tener varios advisors; este numero define en que orden se aplican. */
    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        // Este guardrail solo vigila la salida: la peticion se deja intacta.
        log.debug("before(): el advisor intercepta la peticion antes de llamar al modelo");
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        String texto = response.chatResponse().getResult().getOutput().getText();
        log.debug("after(): el advisor inspecciona la respuesta del modelo: '{}'", texto);

        if (texto != null && texto.contains(CANARY)) {
            log.error("Fuga de canary detectada: el system prompt se ha filtrado");
            throw new GuardrailViolationException("canary", "el token canary aparece en la respuesta");
        }

        return response;
    }
}
