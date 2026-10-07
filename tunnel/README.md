
# Capa de túnel público

El APK no ata el panel a un proveedor concreto.

El ejecutable que se coloque como:

`tunnel/tunnel`

debe aceptar:

`tunnel --port 25565`

y escribir en stdout la información del túnel, incluyendo la dirección pública cuando el proveedor la haya creado.

La siguiente implementación puede adaptarse a un proveedor de túnel compatible con Android/ARM64.

No se incluye un binario de terceros en este proyecto.
