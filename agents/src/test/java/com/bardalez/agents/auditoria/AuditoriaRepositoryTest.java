package com.bardalez.agents.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.UUID;

import com.bardalez.agents.executor.dto.Ejecucion;
import com.bardalez.agents.executor.dto.EstadoEjecucion;
import com.bardalez.agents.executor.dto.PasoEjecutado;
import com.bardalez.agents.process.dto.Procedimiento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * La auditoria, de punta a punta y sin LLM: se escribe una ejecucion y se vuelve a leer.
 *
 * <p>El test corre sobre <strong>H2 en memoria</strong> con el mismo {@code schema.sql} que se aplica a
 * MySQL (ver {@code src/test/resources/application.yaml}), asi que prueba tres cosas de golpe: que el
 * DDL es valido, que los nombres de las columnas concuerdan con los componentes del record —un mapeo
 * que se hace por nombre y falla en silencio dejando {@code null}— y que el {@code UPDATE} del estado
 * de envio encuentra su fila.
 */
@SpringBootTest
class AuditoriaRepositoryTest {

    @Autowired
    private AuditoriaRepository repository;

    private static final Procedimiento VACACIONES =
            new Procedimiento(true, "pedir vacaciones", "vacaciones", List.of("tool-A", "tool-B"));

    @Test
    @DisplayName("Una ejecucion registrada se puede volver a leer, con el informe completo")
    void registraYRecupera() {
        String executionId = UUID.randomUUID().toString();
        Ejecucion ejecucion = new Ejecucion(EstadoEjecucion.EXITOSO,
                List.of(new PasoEjecutado(1, "tool-A", "hecho"),
                        new PasoEjecutado(2, "tool-B", "hecho")),
                List.of(),
                "El procedimiento se ejecutó completo.");

        repository.registrar(executionId, "sesion-auditoria-1", VACACIONES, ejecucion);

        RegistroEjecucion registro = repository.buscar(executionId).orElseThrow();

        assertThat(registro.sessionId()).isEqualTo("sesion-auditoria-1");
        assertThat(registro.procedimiento()).isEqualTo("pedir vacaciones");
        assertThat(registro.estadoEjecucion()).isEqualTo(EstadoEjecucion.EXITOSO);
        assertThat(registro.creadoEn()).isNotNull();

        // El informe se guarda tal cual, serializado: es la copia fiel de lo que devolvio el modelo.
        assertThat(registro.informe())
                .contains("\"estado\"")
                .contains("EXITOSO")
                .contains("tool-A")
                .contains("tool-B");

        // Todavia no hay autenticacion, asi que la columna del claim queda vacia en lugar de inventada.
        assertThat(registro.userId()).isNull();
    }

    @Test
    @DisplayName("El envio arranca siempre en PENDIENTE_ENVIO y solo lo cambia marcarEnviado()")
    void elEstadoDeEnvioEsIndependienteDelDeEjecucion() {
        String executionId = UUID.randomUUID().toString();
        // El caso interesante de la tabla: el tramite salio bien. Aun asi, mientras el sistema externo
        // no confirme, la fila dice que no se ha enviado.
        Ejecucion ejecucion = new Ejecucion(EstadoEjecucion.EXITOSO,
                List.of(new PasoEjecutado(1, "tool-A", "hecho")), List.of(), "Listo.");

        repository.registrar(executionId, "sesion-auditoria-2", VACACIONES, ejecucion);
        assertThat(repository.buscar(executionId).orElseThrow().estadoEnvio())
                .isEqualTo(EstadoEnvio.PENDIENTE_ENVIO);

        repository.marcarEnviado(executionId);

        RegistroEjecucion registro = repository.buscar(executionId).orElseThrow();
        assertThat(registro.estadoEnvio()).isEqualTo(EstadoEnvio.ENVIADO);
        // Y el otro estado no se ha movido: son dos dimensiones distintas.
        assertThat(registro.estadoEjecucion()).isEqualTo(EstadoEjecucion.EXITOSO);
    }

    @Test
    @DisplayName("Una ejecucion fallida se registra igual, con sus pasos fallidos")
    void registraTambienLasFallidas() {
        String executionId = UUID.randomUUID().toString();
        Ejecucion ejecucion = new Ejecucion(EstadoEjecucion.FALLIDO,
                List.of(new PasoEjecutado(1, "tool-A", "hecho")),
                List.of(new PasoEjecutado(2, "tool-B", "no respondió")),
                "Se quedó a medias.");

        repository.registrar(executionId, "sesion-auditoria-3", VACACIONES, ejecucion);

        RegistroEjecucion registro = repository.buscar(executionId).orElseThrow();
        assertThat(registro.estadoEjecucion()).isEqualTo(EstadoEjecucion.FALLIDO);
        assertThat(registro.informe()).contains("pasosFallidos");
    }

    @Test
    @DisplayName("Marcar como enviada una ejecucion que no existe avisa, pero no rompe")
    void marcarEnviadoToleraLaFilaAusente() {
        // Ocurre de verdad: si el INSERT fallo, el envio se intenta igual y este UPDATE no encuentra
        // nada. Ese fallo ya se registro en su momento; una segunda excepcion solo taparia la primera.
        assertThatCode(() -> repository.marcarEnviado(UUID.randomUUID().toString()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Buscar un identificador inexistente devuelve vacio, no excepcion")
    void buscarDevuelveVacio() {
        assertThat(repository.buscar(UUID.randomUUID().toString())).isEmpty();
    }
}
