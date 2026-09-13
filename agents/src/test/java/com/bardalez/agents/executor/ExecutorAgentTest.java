package com.bardalez.agents.executor;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import com.bardalez.agents.process.dto.Procedimiento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * El invariante de seguridad del proyecto, comprobado: <strong>al Executor no se entra sin pasar por
 * Process</strong>.
 *
 * <p>El ChatClient se construye a {@code null} a proposito, y eso es parte de la prueba: si alguna de
 * estas llamadas llegase a hablar con el modelo, el test fallaria con un {@code NullPointerException}
 * en lugar de con la excepcion esperada. Es la forma mas corta de demostrar que el rechazo ocurre
 * antes de cualquier efecto.
 */
class ExecutorAgentTest {

    private final ExecutorAgent executor =
            new ExecutorAgent(null, new ClassPathResource("prompts/executor-user.st"));

    @Test
    @DisplayName("Un procedimiento no encontrado no se ejecuta")
    void rechazaElProcedimientoNoEncontrado() {
        // Este es el caso real: Process no encontro nada y alguien, por descuido, llama al Executor de
        // todas formas. El Router no lo hace -sale por el if antes-, pero el Executor no se fia.
        assertThatIllegalArgumentException()
                .isThrownBy(() -> executor.ejecutar(Procedimiento.noEncontrado()));
    }

    @Test
    @DisplayName("Un procedimiento sin pasos no se ejecuta")
    void rechazaElProcedimientoSinPasos() {
        // Una fila del catalogo con la secuencia vacia: no hay nada que ejecutar, asi que llamar al
        // modelo solo serviria para que se inventase pasos.
        Procedimiento vacio = new Procedimiento(true, "proceso sin pasos", "raro", List.of());

        assertThatIllegalArgumentException().isThrownBy(() -> executor.ejecutar(vacio));
    }

    @Test
    @DisplayName("Un procedimiento nulo no se ejecuta")
    void rechazaElProcedimientoNulo() {
        assertThatIllegalArgumentException().isThrownBy(() -> executor.ejecutar(null));
    }
}
