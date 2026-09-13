package com.bardalez.agents.guardrail;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce cualquier {@link GuardrailViolationException} en una respuesta unica y generica.
 *
 * <p>El detalle del bloqueo (que guardrail se activo y por que) queda en el log del servidor y
 * <strong>nunca</strong> viaja al cliente. Si la respuesta revelara la regla infringida, el usuario
 * podria ir tanteando hasta encontrar una formulacion que la esquive: la propia respuesta de error
 * se convertiria en un oraculo para construir el ataque.
 */
@RestControllerAdvice
public class GuardrailExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GuardrailExceptionHandler.class);

    static final String MENSAJE_GENERICO =
            "Tu solicitud no puede ser procesada por violación de políticas de seguridad";

    @ExceptionHandler(GuardrailViolationException.class)
    public ResponseEntity<Map<String, String>> manejar(GuardrailViolationException ex) {
        log.warn("Peticion rechazada por el guardrail '{}': {}", ex.getGuardrail(), ex.getMotivo());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("respuesta", MENSAJE_GENERICO));
    }
}
