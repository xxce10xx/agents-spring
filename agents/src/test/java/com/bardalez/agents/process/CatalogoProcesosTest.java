package com.bardalez.agents.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Traduccion de la {@code secuencia} JSON a la lista de pasos.
 *
 * <p>No hay Spring ni servidor MCP: la clase se construye a mano con una lista de clientes vacia,
 * porque {@link CatalogoProcesos#pasosDe} no necesita ninguno. Lo que se prueba es justo la parte que
 * el compilador no vigila, porque el JSON llega como texto desde otro proceso.
 */
class CatalogoProcesosTest {

    private final CatalogoProcesos catalogo = new CatalogoProcesos(List.of(), JsonMapper.builder().build());

    @Test
    @DisplayName("pasosDe() devuelve las herramientas en el orden de la secuencia")
    void pasosDeOrdenaLosPasos() {
        Proceso proceso = new Proceso(1L, "pedir vacaciones", "vacaciones", "Solicitud de vacaciones",
                "{\"paso 1\": \"tool-A\", \"paso 2\": \"tool-B\"}");

        assertThat(catalogo.pasosDe(proceso)).containsExactly("tool-A", "tool-B");
    }

    @Test
    @DisplayName("El orden lo fija la clave, no el orden en que llegan las claves")
    void pasosDeNoDependeDelOrdenDelJson() {
        // MySQL normaliza el objeto al guardarlo en una columna JSON, asi que la secuencia puede
        // llegar con las claves en cualquier orden. Ejecutar los pasos al reves seria un desastre
        // silencioso: el TreeMap de pasosDe() es lo que lo evita.
        Proceso proceso = new Proceso(1L, "onboarding", "personal", "Alta de un empleado nuevo",
                "{\"paso 3\": \"tool-C\", \"paso 1\": \"tool-A\", \"paso 2\": \"tool-B\"}");

        assertThat(catalogo.pasosDe(proceso)).containsExactly("tool-A", "tool-B", "tool-C");
    }

    @Test
    @DisplayName("parsear() lee el JSON que devuelve el servidor MCP")
    void parseaElCatalogoDelServidor() {
        // Este es el contenido del TextContent que devuelve tools/call listar_procesos: el JSON
        // serializado de la lista de Proceso del otro proyecto. Como no hay clase compartida, este
        // fixture es el unico sitio donde el contrato queda escrito de este lado.
        String json = """
                [
                  {"id":1,"proceso":"pedir vacaciones","tipo":"vacaciones",
                   "descripcion":"Este proceso permite solicitar y reservar vacaciones.",
                   "secuencia":"{\\"paso 1\\": \\"tool-A\\", \\"paso 2\\": \\"tool-B\\"}"},
                  {"id":2,"proceso":"configurar VPN","tipo":"VPN",
                   "descripcion":"Este proceso permite solicitar la configuracion del acceso VPN.",
                   "secuencia":"{\\"paso 1\\": \\"tool-C\\"}"}
                ]
                """;

        List<Proceso> procesos = catalogo.parsear(json);

        assertThat(procesos).extracting(Proceso::proceso)
                .containsExactly("pedir vacaciones", "configurar VPN");
        // La secuencia llega como cadena, no como objeto: el servidor la transporta sin interpretarla.
        assertThat(catalogo.pasosDe(procesos.get(0))).containsExactly("tool-A", "tool-B");
    }

    @Test
    @DisplayName("Un procedimiento de un solo paso tambien se traduce")
    void pasosDeConUnSoloPaso() {
        Proceso proceso = new Proceso(2L, "configurar VPN", "VPN", "Acceso remoto",
                "{\"paso 1\": \"tool-C\"}");

        assertThat(catalogo.pasosDe(proceso)).containsExactly("tool-C");
    }
}
