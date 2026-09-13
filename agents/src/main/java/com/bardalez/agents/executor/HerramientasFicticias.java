package com.bardalez.agents.executor;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Las tres tools de negocio del Agent Executor. De momento son ficticias: escriben una linea en la
 * consola y devuelven una confirmacion.
 *
 * <p>Son <strong>marcadores de posicion</strong> con nombres deliberadamente vacios de significado.
 * Eso es lo interesante para el curso: lo que hay que entender es el mecanismo (como se declara una
 * tool, como llega al modelo y quien decide invocarla), no la logica de negocio de un sistema de
 * RRHH concreto. El dia que {@code tool-A} sea {@code validarSaldoVacaciones}, nada del resto del
 * proyecto cambia.
 *
 * <p><strong>Los nombres son un contrato con la base de datos.</strong> La columna {@code secuencia}
 * de la tabla {@code procesos} del otro repositorio guarda literalmente
 * {@code {"paso 1": "tool-A", "paso 2": "tool-B"}}. No hay compilador que vigile eso: renombrar aqui
 * una tool sin tocar la tabla deja un procedimiento que apunta a una herramienta que ya no existe.
 * De ahi el test que comprueba los nombres por reflexion.
 *
 * <p>Cada metodo devuelve una cadena en lugar de ser {@code void}, y no es decorativo: lo que devuelve
 * una tool vuelve al modelo como resultado de la llamada, y es lo que le permite saber que el paso
 * salio bien antes de seguir con el siguiente.
 */
@Component
public class HerramientasFicticias {

    private static final Logger log = LoggerFactory.getLogger(HerramientasFicticias.class);

    /**
     * Los nombres publicados, para poder ensenarselos al modelo en el prompt.
     *
     * <p>Duplica lo que dicen las anotaciones, y esa duplicacion esta cubierta por un test: es
     * preferible a construir la lista por reflexion en produccion para ahorrarse tres cadenas.
     */
    public static final List<String> NOMBRES = List.of("tool-A", "tool-B", "tool-C");

    @Tool(name = "tool-A",
            description = """
                    Ejecuta el paso de negocio A de un procedimiento de la empresa. Invocala solo \
                    cuando la secuencia del procedimiento que estas ejecutando incluya 'tool-A', y \
                    en la posicion que indique esa secuencia.""")
    public String toolA() {
        log.info(">>> tool-A ejecutada");
        return "tool-A ejecutada correctamente";
    }

    @Tool(name = "tool-B",
            description = """
                    Ejecuta el paso de negocio B de un procedimiento de la empresa. Invocala solo \
                    cuando la secuencia del procedimiento que estas ejecutando incluya 'tool-B', y \
                    en la posicion que indique esa secuencia.""")
    public String toolB() {
        log.info(">>> tool-B ejecutada");
        return "tool-B ejecutada correctamente";
    }

    @Tool(name = "tool-C",
            description = """
                    Ejecuta el paso de negocio C de un procedimiento de la empresa. Invocala solo \
                    cuando la secuencia del procedimiento que estas ejecutando incluya 'tool-C', y \
                    en la posicion que indique esa secuencia.""")
    public String toolC() {
        log.info(">>> tool-C ejecutada");
        return "tool-C ejecutada correctamente";
    }
}
