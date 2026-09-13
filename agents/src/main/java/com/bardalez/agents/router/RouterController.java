package com.bardalez.agents.router;

import java.util.List;
import java.util.Map;

import com.bardalez.agents.router.dto.RouterRequest;
import com.bardalez.agents.router.dto.RouterResponse;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Punto de entrada REST del sistema. Endpoint de prueba: solo comprueba que el Router llega al
 * LLM y devuelve la intencion clasificada.
 *
 * <p>La cabecera {@code X-Session-Id} identifica la conversacion: dos peticiones con el mismo valor
 * comparten memoria, con valores distintos no se ven entre si. Si el cliente no la manda se usa
 * {@link #SESION_POR_DEFECTO}, suficiente para una demo con un solo usuario.
 */
@RestController
@RequestMapping("/api/v1")
public class RouterController {

    static final String SESION_POR_DEFECTO = "demo";

    private final RouterAgent routerAgent;
    private final ChatMemory chatMemory;

    public RouterController(RouterAgent routerAgent, ChatMemory chatMemory) {
        this.routerAgent = routerAgent;
        this.chatMemory = chatMemory;
    }

    @PostMapping("/chat")
    public RouterResponse chat(@RequestBody RouterRequest request,
                               @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        String sesion = (sessionId == null || sessionId.isBlank()) ? SESION_POR_DEFECTO : sessionId;
        String intencion = routerAgent.clasificarIntencion(request.prompt(), sesion);
        return new RouterResponse(request.prompt(), intencion, sesion);
    }

    /**
     * Endpoint de apoyo para la clase: devuelve el historial que la memoria reenviara al modelo en
     * la siguiente llamada de esa sesion.
     *
     * <p>Existe porque el Router es un clasificador: responde una sola palabra, asi que la memoria
     * no se puede comprobar leyendo su respuesta. Aqui se ve directamente.
     */
    @GetMapping("/chat/{sessionId}/memoria")
    public List<Map<String, String>> memoria(@PathVariable String sessionId) {
        return chatMemory.get(sessionId).stream()
                .map(mensaje -> Map.of(
                        "tipo", mensaje.getMessageType().getValue(),
                        "texto", mensaje.getText()))
                .toList();
    }

    /** Borra el historial de una sesion, para empezar la demo de cero. */
    @DeleteMapping("/chat/{sessionId}/memoria")
    public void olvidar(@PathVariable String sessionId) {
        chatMemory.clear(sessionId);
    }
}
