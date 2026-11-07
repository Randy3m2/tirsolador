# Integración de TirsoDrive

TirsoDrive será la integración de Telegram usada por Tirsolador para acceder a los canales autorizados de juegos y, opcionalmente, respaldar partidas.

## Configuración registrada

- Nombre de integración: TirsoDrive
- Telegram API ID: `38798734`
- Telegram API hash: se configura como secreto de build y no se almacena en Git.

## Flujo previsto

1. La aplicación muestra un código QR en Android TV.
2. El usuario lo escanea desde Telegram en su teléfono.
3. TDLib completa la autenticación autorizada.
4. La sesión se almacena cifrada en el dispositivo.
5. Tirsolador consulta los canales autorizados.
6. Los archivos se indexan por consola, nombre, región y extensión.
7. El juego se descarga bajo demanda y se abre con el emulador correspondiente.

## Seguridad

- Nunca incluir el API hash, sesión de Telegram, tokens ni credenciales en el APK público.
- Usar secretos de CI/CD o configuración local protegida.
- Cifrar la sesión local.
- Permitir cerrar sesión y revocar la sesión desde Ajustes.
- No registrar mensajes, archivos o credenciales en logs de producción.
- Separar configuración de desarrollo, prueba y producción.

## Canales

Los identificadores de canales se configurarán después de recibir la estructura exacta de TirsoDrive. La aplicación no debe asumir nombres de canales ni descargar contenido fuera de los canales autorizados.
