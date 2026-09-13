package com.bardalez.agents.search;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Carga la documentacion corporativa de demo en Qdrant al arrancar.
 *
 * <p>Sin esto el RAG no tiene nada que recuperar y Search contesta siempre que no encuentra la
 * informacion. Los documentos de {@code resources/documentos/} son inventados: sirven para que la
 * demo tenga algo que buscar.
 *
 * <p>El pipeline de indexacion son tres pasos, y es el mismo en cualquier proyecto de RAG:
 *
 * <ol>
 *   <li><strong>Leer</strong> el documento ({@link TextReader}).</li>
 *   <li><strong>Trocear</strong> en fragmentos ({@link TokenTextSplitter}). Se indexan trozos, no
 *       ficheros enteros: la busqueda debe devolver el parrafo relevante, no un manual completo.</li>
 *   <li><strong>Guardar</strong> en la base vectorial ({@code VectorStore.add}), que calcula el
 *       embedding de cada fragmento por el camino.</li>
 * </ol>
 *
 * <p>Se activa con {@code sprintai.search.cargar-documentos: true}. Ojo: no comprueba si ya estaban
 * indexados, asi que cada arranque los inserta otra vez. Para una demo es aceptable; una vez
 * cargados, ponlo en {@code false}.
 */
@Component
@ConditionalOnProperty(name = "sprintai.search.cargar-documentos", havingValue = "true")
public class DocumentosLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentosLoader.class);

    /**
     * Tokens por fragmento. El valor por defecto de {@code TokenTextSplitter} es 800, y los
     * documentos de este corpus rondan los 500 tokens: con 800 cada documento entraba entero como un
     * unico vector, y un vector que resume un documento completo puntua bajo frente a una pregunta
     * concreta. Con 250 salen varios fragmentos por documento, el acertado puntua mas alto y al
     * prompt viaja el parrafo pertinente en lugar del manual entero.
     *
     * <p>Ojo si se baja mucho mas: el troceador solo corta en final de frase a partir del caracter
     * 350 ({@code minChunkSizeChars}), asi que por debajo de unos 120 tokens habria que bajar
     * tambien ese valor o los fragmentos se cortarian a mitad de frase.
     */
    static final int TAMANO_FRAGMENTO = 250;

    private final VectorStore vectorStore;
    private final Resource[] documentos;

    public DocumentosLoader(VectorStore vectorStore,
                            @Value("classpath:documentos/*.md") Resource[] documentos) {
        this.vectorStore = vectorStore;
        this.documentos = documentos;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Con builder(), no con el constructor: en Spring AI 2.0 todos los constructores de
        // TokenTextSplitter estan deprecados y marcados para eliminarse.
        //
        // chunkSize es el parametro que decide la calidad del RAG, porque el fragmento es a la vez
        // la unidad que se recupera y lo que se compara con la pregunta.
        TokenTextSplitter troceador = TokenTextSplitter.builder()
                .withChunkSize(TAMANO_FRAGMENTO)
                .build();

        for (Resource documento : documentos) {
            List<Document> fragmentos = troceador.apply(new TextReader(documento).get());
            vectorStore.add(fragmentos);
            log.info("Indexado {} en {} fragmentos", documento.getFilename(), fragmentos.size());
        }
    }
}
