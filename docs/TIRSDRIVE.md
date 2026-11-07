# Integración de TirsoDrive

TirsoDrive será la integración de Telegram usada por Tirsolador para acceder a los canales autorizados de juegos y, opcionalmente, respaldar partidas.

## Configuración registrada

- Nombre de integración: TirsoDrive
- Telegram API ID: `38798734`
- Telegram API hash: se configura como secreto de build y no se almacena en Git.

## Flujo previsto

1. La primera apertura muestra una bienvenida moderna para conectar TirsoDrive, definida en `design/WELCOME.md`.
2. La aplicación muestra un código QR real en Android TV cuando TDLib lo entregue.
3. El usuario lo escanea desde Telegram en su teléfono.
4. TDLib completa la autenticación autorizada y los pasos adicionales que solicite la cuenta.
5. La sesión se almacena cifrada en el dispositivo.
6. Tirsolador descubre los grupos autorizados, incluidos los archivados, y los asocia a sus consolas.
7. Los archivos se indexan por consola, nombre, región y extensión.
8. El juego se descarga bajo demanda y se abre con el emulador correspondiente.

## Seguridad

- No subir API hashes, sesiones ni tokens al repositorio o logs.
- La configuración local usa Android Keystore. CI también acepta un API hash como
  secreto de Actions; si se inyecta en BuildConfig será extraíble del APK. Esto
  corrige la afirmación anterior de que un secreto de build no llega al binario.
- Las sesiones y claves de la base de datos permanecen en el dispositivo, con backup deshabilitado.
- Cifrar la sesión local.
- Permitir cerrar sesión y revocar la sesión desde Ajustes.
- No registrar mensajes, archivos o credenciales en logs de producción.
- Separar configuración de desarrollo, prueba y producción.

## Canales

La captura del usuario define estas asociaciones iniciales. Los IDs aún no se conocen; deben obtenerse de la sesión autenticada, nunca deducirse de una captura.

| Consola | Grupo exacto |
| --- | --- |
| NES | TD - ROMs - NES |
| SNES | TD - ROMs - SNES |
| Sega Mega Drive | TD - ROMs - Genesis |
| Game Boy | TD - ROMs - Game Boy |
| Nintendo 64 | TD - ROMs - N64 |
| Nintendo DS | TD - ROMs - DS |

Game Boy Color, Game Boy Advance y MAME quedan sin fuente asignada hasta conocer sus grupos. El grupo GameCube aparece en la captura, pero está fuera de las consolas iniciales y no activa esa consola automáticamente. TD - Photos, TD - Compressed y TD - Audio quedan excluidos del catálogo de juegos.

La configuración está en `config/telegram-sources.json`. Los nombres sólo sirven para descubrir la fuente inicialmente: después se persiste la asociación por cuenta, consola e ID real de chat. Los títulos duplicados requieren selección manual; las fuentes ausentes no se sustituyen con otro grupo.

Los textos de adjuntos de la captura empiezan con `tirsodrive v1`. Antes de indexar deben comprobarse el formato real de esos mensajes, los nombres y archivos adjuntos, y si requieren reconstrucción. Ese texto por sí solo no demuestra que cada adjunto sea una ROM directamente ejecutable.
