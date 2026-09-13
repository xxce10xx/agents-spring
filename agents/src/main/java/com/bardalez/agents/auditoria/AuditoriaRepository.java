package com.bardalez.agents.auditoria;

import java.util.Optional;

import com.bardalez.agents.executor.dto.Ejecucion;
import com.bardalez.agents.process.dto.Procedimiento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * La tabla {@code ejecucion}: donde queda constancia de cada procedimiento que el sistema ejecuta.
 *
 * <p>El dueno de esta escritura es el <strong>Agent Router</strong>, no el Executor, y no es un
 * detalle: el Executor produce los efectos y devuelve un informe, pero no sabe en que sesion esta ni
 * de que empleado, y sobre todo no debe poder decidir si su propia ejecucion se registra. Quien
 * audita nunca es el auditado.
 *
 * <p>Se guardan dos cosas a la vez, y el motivo de la duplicidad es practico:
 *
 * <ul>
 *   <li>El <strong>informe entero</strong>, serializado como JSON en una sola columna. Es la copia
 *       fiel de lo que devolvio el modelo, sin interpretar: si manana el contrato gana campos, las
 *       filas viejas siguen contando lo que se supo entonces.</li>
 *   <li>Cuatro <strong>columnas sueltas</strong> (procedimiento, los dos estados y la sesion) que
 *       repiten datos que ya estan dentro del JSON. Se repiten para poder consultarlas con un
 *       {@code WHERE} normal: la pregunta que justifica esta tabla —"que ejecuciones salieron bien y
 *       nunca llegaron al sistema externo"— tiene que ser un {@code SELECT} simple.</li>
 * </ul>
 *
 * <p>Aqui no hay {@code try/catch}: si el {@code INSERT} falla, la excepcion sube. Quien decide que
 * hacer con ella es el Router, que es el unico que sabe que los efectos del tramite ya ocurrieron.
 */
@Repository
public class AuditoriaRepository {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaRepository.class);

    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    public AuditoriaRepository(JdbcClient jdbcClient, JsonMapper jsonMapper) {
        this.jdbcClient = jdbcClient;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Registra una ejecucion recien terminada, siempre como {@link EstadoEnvio#PENDIENTE_ENVIO}.
     *
     * <p>El estado de envio arranca en pendiente <strong>siempre</strong>, aunque el envio vaya a
     * ocurrir un milisegundo despues: primero se deja el rastro y luego se intenta publicar. Al
     * reves —enviar y registrar despues— un fallo entre las dos operaciones dejaria al sistema externo
     * enterado de algo que aqui no consta, que es la inconsistencia mas dificil de investigar.
     *
     * <p>La columna {@code user_id} no aparece en el {@code INSERT}: queda a {@code NULL} porque
     * todavia no hay claim de identidad que copiar.
     */
    public void registrar(String executionId,
                          String sessionId,
                          Procedimiento procedimiento,
                          Ejecucion ejecucion) {

        jdbcClient.sql("""
                        INSERT INTO ejecucion
                            (execution_id, session_id, procedimiento, estado_ejecucion, estado_envio, informe)
                        VALUES
                            (:executionId, :sessionId, :procedimiento, :estadoEjecucion, :estadoEnvio, :informe)
                        """)
                .param("executionId", executionId)
                .param("sessionId", sessionId)
                .param("procedimiento", procedimiento.proceso())
                .param("estadoEjecucion", ejecucion.estado().name())
                .param("estadoEnvio", EstadoEnvio.PENDIENTE_ENVIO.name())
                // El structured output completo, tal como lo devolvio el modelo.
                .param("informe", jsonMapper.writeValueAsString(ejecucion))
                .update();

        log.info("Auditoria: ejecucion {} registrada ('{}', {}, {})",
                executionId, procedimiento.proceso(), ejecucion.estado(), EstadoEnvio.PENDIENTE_ENVIO);
    }

    /**
     * Marca una ejecucion ya registrada como recibida por el sistema externo.
     *
     * <p>Si no actualiza ninguna fila no lanza excepcion, solo avisa: significa que el {@code INSERT}
     * anterior fallo, y eso ya se registro como error en su momento. Convertirlo aqui en una segunda
     * excepcion solo taparia la primera.
     */
    public void marcarEnviado(String executionId) {
        int filas = jdbcClient.sql("""
                        UPDATE ejecucion
                           SET estado_envio = :estado
                         WHERE execution_id = :executionId
                        """)
                .param("estado", EstadoEnvio.ENVIADO.name())
                .param("executionId", executionId)
                .update();

        if (filas == 0) {
            log.warn("Auditoria: no hay fila que marcar como {} para la ejecucion {}",
                    EstadoEnvio.ENVIADO, executionId);
            return;
        }

        log.info("Auditoria: ejecucion {} marcada como {}", executionId, EstadoEnvio.ENVIADO);
    }

    /**
     * Recupera una ejecucion registrada. La auditoria que nadie puede leer no es auditoria; este
     * metodo es la lectura minima que lo demuestra, y es lo que comprueban los tests.
     */
    public Optional<RegistroEjecucion> buscar(String executionId) {
        return jdbcClient.sql("""
                        SELECT execution_id, user_id, session_id, procedimiento,
                               estado_ejecucion, estado_envio, informe, creado_en
                          FROM ejecucion
                         WHERE execution_id = :executionId
                        """)
                .param("executionId", executionId)
                .query(RegistroEjecucion.class)
                .optional();
    }
}
