package com.bardalez.agents.process;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * La puerta al servidor MCP: trae el catalogo de procedimientos y traduce la secuencia de cada uno.
 *
 * <p>Se llama con codigo, no con tool calling. Spring AI puede exponer las herramientas MCP al modelo
 * para que decida si las invoca ({@code toolcallback.enabled}), y aqui esta deliberadamente
 * desactivado: el Agent Process <strong>siempre</strong> necesita el catalogo completo, asi que
 * pedirle permiso al LLM seria pagar una llamada extra para que conteste lo que ya sabemos. El tool
 * calling se gana su sitio cuando la decision de llamar es genuina.
 *
 * <p>Aqui tampoco hay {@code try/catch}. Si el servidor MCP esta caido, la excepcion sube y la
 * peticion acaba en un 500 con el motivo en el log, que para una demo es la respuesta honesta: mejor
 * que un "no encontre el procedimiento" que le echaria la culpa al catalogo.
 */
@Component
public class CatalogoProcesos {

    private static final Logger log = LoggerFactory.getLogger(CatalogoProcesos.class);

    /** Nombre de la herramienta publicada por mcp-procesos. Es un contrato de texto, sin compilador. */
    static final String HERRAMIENTA = "listar_procesos";

    private static final TypeReference<List<Proceso>> LISTA_DE_PROCESOS = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, String>> PASOS = new TypeReference<>() {
    };

    /**
     * Los clientes MCP configurados, uno por conexion declarada en {@code application.yaml}. Spring
     * AI publica la lista entera; este proyecto declara una sola conexion y usa la primera.
     */
    private final List<McpSyncClient> clientesMcp;

    private final JsonMapper jsonMapper;

    public CatalogoProcesos(List<McpSyncClient> clientesMcp, JsonMapper jsonMapper) {
        this.clientesMcp = clientesMcp;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Invoca {@code listar_procesos} y devuelve el catalogo completo.
     *
     * <p>Se trae la tabla entera a proposito: son pocos procedimientos y el LLM elige mucho mejor
     * viendo todas las opciones con su descripcion al lado que intentando acertar un termino de
     * busqueda.
     */
    public List<Proceso> listar() {
        McpSyncClient cliente = clientesMcp.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay ningun cliente MCP configurado: revisa "
                                + "spring.ai.mcp.client.streamable-http.connections en el application.yaml"));

        // La peticion se construye con el builder: el constructor (nombre, argumentos) esta
        // deprecado en el SDK 2.0, porque el record tiene un tercer componente (_meta) y la version
        // corta ocultaba que llegaba a null.
        CallToolRequest peticion = CallToolRequest.builder(HERRAMIENTA)
                .arguments(Map.of())    // listar_procesos no tiene parametros
                .build();

        // Con initialized: false el handshake no se hizo al arrancar; lo lanza esta primera llamada.
        CallToolResult resultado = cliente.callTool(peticion);

        if (Boolean.TRUE.equals(resultado.isError())) {
            throw new IllegalStateException(
                    "El servidor MCP devolvio un error al invocar " + HERRAMIENTA + ": " + resultado.content());
        }

        // El contenido de una respuesta MCP viaja como texto, aunque el servidor devolviera una lista
        // de objetos: lo que hay dentro del TextContent es el JSON serializado.
        String json = ((TextContent) resultado.content().get(0)).text();
        List<Proceso> procesos = parsear(json);

        log.debug("El servidor MCP devolvio {} procedimientos", procesos.size());
        return procesos;
    }

    /**
     * Convierte el JSON que devolvio el servidor en la lista de procedimientos.
     *
     * <p>Esta extraido de {@link #listar()} para poder probarlo sin servidor: es justo la costura
     * donde se comprueba el contrato entre los dos proyectos, que ningun compilador vigila.
     */
    List<Proceso> parsear(String json) {
        return jsonMapper.readValue(json, LISTA_DE_PROCESOS);
    }

    /**
     * Convierte la {@code secuencia} de un procedimiento en la lista ordenada de sus pasos.
     *
     * <p>Del JSON {@code {"paso 1": "tool-A", "paso 2": "tool-B"}} salen {@code [tool-A, tool-B]}. El
     * orden se consigue con un {@link TreeMap}, que ordena por clave: no se puede confiar en el orden
     * en que Jackson lea las claves, ni en el que las devuelva MySQL (el tipo {@code JSON} normaliza
     * el objeto al guardarlo).
     *
     * <p>Ordenar textos tiene una trampa conocida: a partir del paso 10, {@code "paso 10"} va antes
     * que {@code "paso 2"}. Con procedimientos de dos o tres pasos no molesta; el dia que haga falta,
     * la solucion es numerar {@code paso 01} en la base de datos, no complicar este metodo.
     */
    public List<String> pasosDe(Proceso proceso) {
        Map<String, String> secuencia = new TreeMap<>(jsonMapper.readValue(proceso.secuencia(), PASOS));
        return List.copyOf(secuencia.values());
    }
}
