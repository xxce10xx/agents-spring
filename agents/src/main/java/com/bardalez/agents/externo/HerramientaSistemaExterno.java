package com.bardalez.agents.externo;

import com.bardalez.agents.executor.dto.Ejecucion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * La tool que publica el informe de una ejecucion en el sistema externo de reporteria. Como las tres
 * del Executor, es ficticia: escribe el JSON en la consola y devuelve una confirmacion.
 *
 * <p><strong>La invoca el Router con codigo, no el modelo.</strong> Lleva {@link Tool} —es una tool con
 * todas las letras, y el dia que el envio sea una llamada HTTP real solo cambia el cuerpo del metodo—
 * pero no esta registrada en ningun {@code ChatClient}, asi que ningun LLM la ve ni puede decidir
 * invocarla. Es deliberado y es la misma decision que ya se tomo en {@code CatalogoProcesos}: el envio
 * no es opcional. Ocurre en el 100% de las ejecuciones, asi que preguntarle al modelo si conviene
 * enviar seria pagar una llamada extra para que conteste lo que ya sabemos, y arriesgarse a que
 * contestara que no.
 *
 * <p>La diferencia con {@code tool-A}, {@code tool-B} y {@code tool-C} vale la pena entenderla, porque
 * es la que decide donde se pone una tool:
 *
 * <table border="1">
 *   <caption>Quien decide la llamada</caption>
 *   <tr><th>Tool</th><th>Quien la invoca</th><th>Por que</th></tr>
 *   <tr><td>{@code tool-A/B/C}</td><td>El modelo</td>
 *       <td>Cuales y en que orden depende del procedimiento; la decision es genuina</td></tr>
 *   <tr><td>{@code tool-enviar-ejecucion}</td><td>El Router, con una linea de Java</td>
 *       <td>Se envia siempre; no hay nada que decidir</td></tr>
 * </table>
 *
 * <p>Que el sistema externo sea de terceros tiene una consecuencia de diseno: esta clase no valida ni
 * reinterpreta el informe, lo envia tal como salio del Executor. La normalizacion ya se hizo en
 * {@link Ejecucion}, y hacerla otra vez aqui abriria la puerta a que MySQL y la reporteria contaran
 * versiones distintas de la misma ejecucion.
 */
@Component
public class HerramientaSistemaExterno {

    private static final Logger log = LoggerFactory.getLogger(HerramientaSistemaExterno.class);

    /** El nombre publicado, en el mismo estilo que las tools del Executor. */
    public static final String NOMBRE = "tool-enviar-ejecucion";

    private final JsonMapper jsonMapper;

    public HerramientaSistemaExterno(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Envia el informe de una ejecucion al sistema externo.
     *
     * <p>Se serializa el {@link Ejecucion} para pintarlo porque lo que interesa ver en clase es
     * <em>el payload</em>: exactamente el mismo JSON que se guardo en la columna {@code informe} y
     * exactamente el que viajaria por HTTP. Un {@code toString()} de record mostraria otra cosa.
     *
     * @param executionId el identificador que genero el Router; es lo que permite cruzar esta linea del
     *                    log con su fila en la tabla {@code ejecucion}
     * @param ejecucion el structured output que devolvio el Agent Executor
     * @return confirmacion de recepcion
     */
    @Tool(name = NOMBRE,
            description = """
                    Publica en el sistema externo de reporteria el informe de un procedimiento que ya \
                    termino de ejecutarse, para que quede constancia fuera de esta aplicacion. \
                    Invocala una sola vez por ejecucion y solo cuando la ejecucion haya terminado.""")
    public String enviar(String executionId, Ejecucion ejecucion) {
        String payload = jsonMapper.writeValueAsString(ejecucion);

        log.info(">>> {} envia la ejecucion {} al External System: {}", NOMBRE, executionId, payload);

        return "Ejecucion " + executionId + " recibida por el External System";
    }
}
