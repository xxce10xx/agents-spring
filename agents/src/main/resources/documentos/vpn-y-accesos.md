# VPN y acceso a herramientas

Documento interno de Soporte Informático. Contenido ficticio, creado para la demo del curso.

## Conexión a la VPN

El cliente oficial es GlobalConnect, disponible en el Centro de Software del equipo corporativo. La
conexión requiere el usuario de dominio, la contraseña y el código de doble factor de la aplicación
Authenticator.

El perfil que hay que seleccionar es `vpn.empresa-demo.com`. Los equipos personales no pueden
conectarse a la VPN corporativa; para trabajar desde un equipo no gestionado existe el escritorio
remoto en `remoto.empresa-demo.com`.

## Problemas frecuentes de VPN

- **Error 691 (credenciales rechazadas):** casi siempre es la contraseña de dominio caducada.
  Renuévala en el Portal de Identidad y vuelve a intentarlo.
- **Se conecta pero no hay acceso a los sistemas internos:** revisa que no tengas otra VPN activa,
  por ejemplo la de un antivirus doméstico. Dos túneles simultáneos se anulan.
- **Conexión muy lenta:** desactiva el túnel completo y usa el perfil de túnel dividido, que solo
  enruta el tráfico corporativo. Está en Ajustes avanzados del cliente.
- **Desconexiones cada pocos minutos:** suele ser la red WiFi doméstica en la banda de 2,4 GHz.
  Prueba con 5 GHz o con cable.

Si nada de esto lo resuelve, abre un ticket en el Portal de Soporte con la categoría "Conectividad
remota" y adjunta el registro del cliente, que se exporta desde Ayuda > Guardar diagnóstico.

## Restablecer la contraseña

La contraseña de dominio caduca cada 90 días y se cambia en `identidad.empresa-demo.com`. Debe tener
12 caracteres como mínimo y no puede coincidir con las 5 anteriores. Si ya está bloqueada, el
desbloqueo lo hace la mesa de ayuda en el 900 123 456 tras verificar la identidad.

## Alta en herramientas y permisos

Las altas en herramientas internas se solicitan en el Portal de Soporte, categoría "Accesos y
permisos". La solicitud la aprueba el responsable directo y, si la herramienta trata datos
personales, también el responsable del dato. El plazo habitual es de 2 días laborables.

Las licencias de software de pago requieren justificación de uso y aprobación presupuestaria. Las
herramientas del catálogo estándar (suite de oficina, cliente de correo, navegador corporativo) están
preinstaladas y no necesitan solicitud.

## Contacto

Mesa de ayuda: soporte@empresa-demo.com o 900 123 456, de lunes a viernes de 7:00 a 20:00.
