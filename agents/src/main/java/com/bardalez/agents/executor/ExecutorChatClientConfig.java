package com.bardalez.agents.executor;

import java.util.Map;

import com.bardalez.agents.guardrail.CanaryLeakAdvisor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Configuracion del ChatClient del Agent Executor.
 *
 * <p>Es el unico ChatClient del proyecto con herramientas registradas, y ahi esta el cambio de fondo
 * respecto a los otros tres agentes: hasta ahora el modelo solo producia texto: una palabra, una
 * respuesta o un JSON. Este modelo <strong>tiene un boton que aprieta</strong>. Con
 * {@code defaultTools(...)} Spring AI hace tres cosas por cada llamada:
 *
 * <ol>
 *   <li>Lee las anotaciones {@code @Tool} del bean y construye el esquema de cada herramienta.</li>
 *   <li>Lo manda en la peticion, para que el modelo sepa que puede invocar y con que argumentos.</li>
 *   <li>Cuando el modelo pide una llamada, ejecuta el metodo Java y le devuelve el resultado, tantas
 *       veces como haga falta, antes de dar por terminada la respuesta.</li>
 * </ol>
 *
 * <p>Ese bucle es transparente para {@link ExecutorAgent}: desde el codigo se ve un solo
 * {@code call()}.
 *
 * <p><strong>Las tools se registran aqui y en ningun otro sitio.</strong> El Router, Search y Process
 * tienen sus propios ChatClient sin herramientas, asi que ninguno de esos modelos puede invocar
 * {@code tool-A} ni aunque el empleado se lo pida con mucha educacion. La superficie de accion del
 * sistema no depende del prompt, depende de donde se registraron las herramientas.
 */
@Configuration
public class ExecutorChatClientConfig {

    @Bean
    ChatClient executorChatClient(ChatClient.Builder builder,
                                  @Value("classpath:prompts/executor-system.st") Resource systemPrompt,
                                  HerramientasFicticias herramientas) {

        String systemPromptRenderizado = new PromptTemplate(systemPrompt)
                .render(Map.of("canary", CanaryLeakAdvisor.CANARY));

        return builder
                .defaultSystem(systemPromptRenderizado)
                .defaultAdvisors(new CanaryLeakAdvisor())
                // Se le pasa el bean entero: Spring AI genera una tool por cada metodo anotado.
                .defaultTools(herramientas)
                .build();
    }
}
