package com.bardalez.agents.search;

import java.util.Map;

import com.bardalez.agents.guardrail.CanaryLeakAdvisor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Configuracion del ChatClient del Agent Search.
 *
 * <table border="1">
 *   <caption>Los dos advisors</caption>
 *   <tr><th>Advisor</th><th>Que hace</th><th>Gancho</th></tr>
 *   <tr><td>{@link QuestionAnswerAdvisor}</td><td>RAG: busca en Qdrant e inyecta los fragmentos</td>
 *       <td>{@code before()}</td></tr>
 *   <tr><td>{@link CanaryLeakAdvisor}</td><td>Guardrail: vigila que no se filtre el system prompt</td>
 *       <td>{@code after()}</td></tr>
 * </table>
 *
 * <p>Reparar en que el {@code CanaryLeakAdvisor} es <em>el mismo</em> que usa el Router: un advisor
 * se escribe una vez y se reutiliza en todos los agentes.
 *
 * <p><strong>Aqui NO hay advisor de memoria</strong>, y es una decision de arquitectura, no un
 * olvido. La memoria pertenece al Router, que es el unico que conoce la sesion del usuario y el
 * unico por el que pasan las dos ramas. Search recibe la memoria como un dato mas, en la propia
 * llamada. Ver {@link SearchAgent}.
 */
@Configuration
public class SearchChatClientConfig {

    /**
     * Orden del advisor de RAG.
     *
     * <p>Con dos advisors el orden importa, porque la cadena es <strong>anidada</strong>: el de
     * orden mas bajo envuelve a los demas. Con canary ({@code 0}) y RAG ({@code 10}) queda:
     *
     * <pre>
     * canary.before -&gt; rag.before -&gt; MODELO -&gt; rag.after -&gt; canary.after
     * </pre>
     *
     * <p>Interesa que el canary quede por fuera del RAG: asi inspecciona la respuesta final, ya
     * generada con los fragmentos dentro del prompt.
     */
    private static final int ORDEN_RAG = 10;

    @Bean
    ChatClient searchChatClient(ChatClient.Builder builder,
                                @Value("classpath:prompts/search-system.st") Resource systemPrompt,
                                VectorStore vectorStore,
                                @Value("${sprintai.search.top-k}") int topK,
                                @Value("${sprintai.search.umbral-similitud}") double umbralSimilitud) {

        String systemPromptRenderizado = new PromptTemplate(systemPrompt)
                .render(Map.of("canary", CanaryLeakAdvisor.CANARY));

        // Los parametros del RAG: cuantos fragmentos y con que parecido minimo.
        SearchRequest busqueda = SearchRequest.builder()
                .topK(topK)
                .similarityThreshold(umbralSimilitud)
                .build();

        return builder
                .defaultSystem(systemPromptRenderizado)
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(busqueda)
                                .order(ORDEN_RAG)
                                .build(),
                        new CanaryLeakAdvisor())
                .build();
    }
}
