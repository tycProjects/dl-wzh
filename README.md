# Android Server Hub v0.4

Panel Android para administrar un servidor Minecraft directamente desde el teléfono.

## Lo que añade v0.4
- Importación de un JRE Android en ZIP.
- Importación de `server.jar` mediante el selector de archivos de Android.
- Detección automática de `bin/java` aunque el ZIP tenga una carpeta superior.
- Inicio/parada/reinicio real del proceso Java.
- RAM inicial y máxima configurables.
- Consola en tiempo real.
- Envío de comandos al servidor (`list`, `stop`, etc.).
- Servicio Android en primer plano.
- Protección contra rutas maliciosas dentro del ZIP.

## Estructura esperada
No hace falta que el JRE tenga exactamente una carpeta en la raíz. El programa busca cualquier:

`.../bin/java`

El servidor se guarda internamente como:

`filesDir/servers/java/server.jar`

## Importante
Este proyecto es la base del APK, no incluye un JRE Android binario ni un `server.jar`.
El JRE debe ser compatible con la arquitectura Android del teléfono.

La parte Bedrock y el acceso público por Internet quedan como siguiente fase.


## v0.5 — Bedrock base
- Añadido selector/importador del ejecutable Bedrock.
- Servicio capaz de ejecutar un proceso Bedrock separado del proceso Java.
- Consola/comandos reutilizables.
- Java sigue funcionando con el JRE Android importado.

### Nota importante sobre Bedrock
El servidor Bedrock oficial normalmente se distribuye para plataformas concretas. Android no debe asumirse compatible con un binario Linux/Windows cualquiera. Esta versión deja el puente de ejecución preparado, pero la compatibilidad del ejecutable concreto debe verificarse antes de distribuirlo.

## Próxima fase: Internet público
La arquitectura prevista será:
Android Server Hub → servidor local → adaptador/túnel → dirección pública.
Se mantendrá separado del proceso del servidor para poder cambiar de proveedor de túnel sin tocar el panel.


## v0.6 — Capa de acceso público

Se añadió una capa independiente para publicar el puerto del servidor:
- Iniciar/detener túnel desde el panel.
- Puerto configurable.
- Proceso del túnel independiente del proceso Java/Bedrock.
- Logs del túnel.
- Espacio preparado para un proveedor compatible con Android/ARM64.
- No incluye binarios de terceros.

### Arquitectura
`Panel → ServerService → Minecraft`
`Panel → ServerService → Tunnel → Internet`

Así podemos cambiar el proveedor de túnel sin modificar el servidor.


## v0.7 — Administrador de archivos

Nuevo panel de gestión:
- Lista de archivos del servidor.
- Tamaños.
- Eliminación segura.
- Importación de mods, configs, mundos y packs.
- Separación Java/Bedrock.
- Protección contra rutas fuera de la carpeta del servidor.

Todavía no se descomprimen automáticamente ZIP de mundos/modpacks: se mantiene como archivo importado para evitar sobrescribir contenido accidentalmente. La siguiente fase puede añadir instalación de ZIP con vista previa.


## v0.8 — Instalador ZIP

Se añadió instalación directa de ZIP para:
- Mundos.
- Mods.
- Resource packs.
- Configuraciones.

El instalador:
- Rechaza rutas absolutas y `../`.
- Extrae únicamente archivos dentro de la carpeta seleccionada.
- Evita ejecutar contenido del ZIP.
- Muestra cuántos archivos fueron instalados.

Nota: esta primera versión instala los archivos directamente y no intenta ejecutar scripts ni instalar binarios. La detección avanzada de modpacks (Fabric/Forge/NeoForge) y su metadata queda para la siguiente fase.


## v0.9 — Detección de modpacks

Se añadió:
- Detección básica de Fabric, Forge y NeoForge.
- Detección de paquetes Modrinth y CurseForge.
- Detección de `overrides`.
- Instalación de contenido de modpacks en el servidor Java.
- Protección contra rutas fuera del directorio del servidor.

Importante: detectar el loader no significa instalar automáticamente el loader/JAR. La siguiente fase puede preparar el servidor con el loader correcto y validar la versión de Minecraft antes de arrancar.


## v1.0 — Validación de instalación

Se añadió una comprobación previa:
- Detecta `server.jar`.
- Comprueba `eula.txt`.
- Cuenta mods `.jar`.
- Intenta detectar Fabric, Forge y NeoForge.
- Muestra información básica de la instalación antes de iniciar.

Esto prepara el proyecto para una futura validación estricta de compatibilidad entre versión de Minecraft, loader y mods.
