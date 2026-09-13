package com.bardalez.agents.router;

import com.bardalez.agents.router.dto.RouterRequest;
import com.bardalez.agents.router.dto.RouterResponse;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Punto de entrada REST del sistema. Endpoint de prueba: solo comprueba que el Router llega al
 * LLM y devuelve la intencion clasificada.
 */
@RestController
@RequestMapping("/api/v1")
public class RouterController {

    private final RouterAgent routerAgent;

    public RouterController(RouterAgent routerAgent) {
        this.routerAgent = routerAgent;
    }

    @PostMapping("/chat")
    public RouterResponse chat(@RequestBody RouterRequest request,
                               @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        String intencion = routerAgent.clasificarIntencion(request.prompt());
        return new RouterResponse(request.prompt(), intencion, sessionId);
    }
}
