-- Tabla de auditoria de la rama de accion: una fila por ejecucion del Agent Executor.
--
-- La ejecuta Spring al arrancar porque application.yaml declara spring.sql.init.mode: always. Con el
-- valor por defecto (embedded) este fichero no tocaria MySQL, solo una base en memoria.
-- CREATE TABLE IF NOT EXISTS la hace idempotente: se ejecuta en cada arranque y no borra nada.
--
-- La tabla de la memoria de conversacion (SPRING_AI_CHAT_MEMORY) NO esta aqui: la crea Spring AI por
-- su cuenta con spring.ai.chat.memory.repository.jdbc.initialize-schema. Son dos tablas en la misma
-- base de datos, creadas por dos mecanismos distintos, y conviene no confundirlos.
CREATE TABLE IF NOT EXISTS ejecucion (
    execution_id     CHAR(36)     NOT NULL,
    user_id          VARCHAR(100),
    session_id       VARCHAR(100) NOT NULL,
    procedimiento    VARCHAR(120) NOT NULL,
    estado_ejecucion VARCHAR(20)  NOT NULL,
    estado_envio     VARCHAR(20)  NOT NULL,
    informe          TEXT         NOT NULL,
    creado_en        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (execution_id)
);

-- Tres decisiones de tipos que no son obvias:
--
-- 1. user_id admite NULL. Todavia no hay autenticacion, asi que el claim no existe: dejar la columna
--    vacia dice la verdad, y rellenarla con un "desconocido" inventado convertiria la auditoria en
--    una fuente de datos falsos. Se llenara cuando el Router extraiga el claim del token.
--
-- 2. informe es TEXT y no JSON, al contrario que la columna secuencia del repositorio mcp-spring.
--    Alli el JSON lo escribe una persona a mano y que MySQL lo valide al insertar tiene valor; aqui
--    lo serializa Jackson desde un record, asi que la validacion no protege de nada. En cambio TEXT
--    permite un unico schema.sql para MySQL y para el H2 de los tests, que no acepta una cadena en
--    una columna JSON.
--
-- 3. estado_ejecucion y estado_envio son dos columnas, no una. Son dimensiones independientes: un
--    procedimiento puede haberse ejecutado bien y no haber llegado nunca al sistema externo, y ese
--    caso -EXITOSO + PENDIENTE_ENVIO- es justo el que hay que poder consultar.
