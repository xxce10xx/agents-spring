package com.bardalez.agents.guardrail;

/**
 * Se lanza cuando un guardrail bloquea la ejecucion.
 *
 * <p>Los datos que transporta ({@code guardrail} y {@code motivo}) son para el log interno. Al
 * usuario se le devuelve siempre el mismo mensaje generico, definido en
 * {@link GuardrailExceptionHandler}.
 */
public class GuardrailViolationException extends RuntimeException {

    private final String guardrail;
    private final String motivo;

    public GuardrailViolationException(String guardrail, String motivo) {
        super("Guardrail '" + guardrail + "' bloqueo la ejecucion: " + motivo);
        this.guardrail = guardrail;
        this.motivo = motivo;
    }

    public String getGuardrail() {
        return guardrail;
    }

    public String getMotivo() {
        return motivo;
    }
}
