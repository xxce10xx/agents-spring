package com.bardalez.agents.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;

/**
 * Los dos primeros pasos del pipeline de indexacion (leer y trocear) se pueden probar sin Qdrant y
 * sin gastar embeddings. Solo el tercero, guardar, necesita infraestructura.
 *
 * <p>Vale la pena tenerlo: si el corpus desaparece o deja de trocearse, el RAG no falla con un
 * error, simplemente responde que no encuentra nada. Eso cuesta mucho mas de diagnosticar.
 */
class DocumentosTest {

    @Test
    @DisplayName("La documentacion de demo se lee y se trocea en fragmentos")
    void elCorpusSeTrocea() throws IOException {
        Resource[] documentos = new PathMatchingResourcePatternResolver()
                .getResources("classpath:documentos/*.md");

        assertThat(documentos).isNotEmpty();

        TokenTextSplitter troceador = TokenTextSplitter.builder()
                .withChunkSize(DocumentosLoader.TAMANO_FRAGMENTO)
                .build();
        for (Resource documento : documentos) {
            List<Document> fragmentos = troceador.apply(new TextReader(documento).get());

            // Mas de uno a proposito: si un documento entra entero como un solo fragmento, su vector
            // queda promediado y el RAG puntua bajo aunque el documento sea el correcto. Es el fallo
            // que se estaba dando con el chunkSize por defecto de 800.
            assertThat(fragmentos)
                    .as("fragmentos de %s", documento.getFilename())
                    .hasSizeGreaterThan(1);
            assertThat(fragmentos).allSatisfy(
                    fragmento -> assertThat(fragmento.getText()).isNotBlank());
        }
    }
}
