# Tirsolador 0.3.0 Preview — TirsoDrive nativo

## Implementado

- TDLib nativo dentro del APK: distribución Maven `io.github.tdlib-android:core:0.1.1`.
- Bibliotecas incluidas para arm64-v8a, armeabi-v7a, x86_64 y x86.
- Primera bienvenida para conectar TirsoDrive; alternativa explícita sin conexión.
- QR generado localmente a partir del enlace real que entrega Telegram, con
  actualizaciones de token recibidas de TDLib; no se utiliza un servicio externo de QR.
- Contraseña de dos pasos y código de autenticación cuando TDLib solicita esos estados.
- Persistencia de sesión, cierre de sesión y clave de base de datos protegida por Android Keystore.
- Descubrimiento de grupos principales y archivados. Asignación por coincidencia
  exacta y persistencia por cuenta, consola e ID de chat. Los duplicados requieren selección.
- UI para elegir, corregir o quitar un grupo por consola.
- Lectura paginada de historial, filtrado de documentos por extensión y catálogo
  persistente. Un error o cancelación conserva la biblioteca anterior.
- Refresco conectado al servicio de sincronización, sin simular una actualización local.
- Descarga bajo demanda, progreso y cancelación; archivos en el almacenamiento
  privado de la aplicación. Comprobación previa de espacio libre.
- Carga inicial limitada a 60 tarjetas, con «Mostrar más», para no inflar miles
  de vistas de una sola vez al conectar un catálogo grande.

## Asignaciones iniciales

| Consola | Nombre exacto |
| --- | --- |
| NES | TD - ROMs - NES |
| SNES | TD - ROMs - SNES |
| Sega Mega Drive | TD - ROMs - Genesis |
| Game Boy | TD - ROMs - Game Boy |
| Nintendo 64 | TD - ROMs - N64 |
| Nintendo DS | TD - ROMs - DS |

GBC, GBA y MAME requieren elegir su fuente manualmente hasta conocer los grupos.
GameCube no forma parte de esta versión. Fotos, audio y comprimidos generales
no se convierten automáticamente en fuentes de juegos.

## Activar la aplicación de Telegram

El API ID de TirsoDrive está preconfigurado. El API hash no está en el repositorio.
Hay dos opciones:

1. En la bienvenida, **Configurar TirsoDrive** e introducir el hash una vez en la
   TV (también se puede escribir con el teclado del teléfono conectado al remoto).
   Se almacena cifrado con una clave de Android Keystore. Luego aparece el QR real.
2. Configurar el secreto de Actions **TIRSO_TELEGRAM_API_HASH** y repetir el build.
   La configuración se inyecta mediante BuildConfig; el código fuente no contiene
   el valor. Como en cualquier aplicación cliente, un valor compilado dentro de
   un APK puede extraerse: un secreto de CI protege Git y los logs, no vuelve
   confidencial el binario distribuido.

`BUILD_STATUS.txt` en la release indica si el build recibió la configuración de CI,
sin mostrar su valor. La autorización de la cuenta siempre requiere aprobación
desde Telegram en el teléfono; el API ID/hash no autorizan por sí solos una cuenta.

## Requisitos

Android 8.0 / API 26 o superior por el mínimo del paquete TDLib seleccionado.
El applicationId y la firma de pruebas se conservan; versionCode aumenta a 3.

## Validación y límites

Las pruebas automatizadas comprueban carga JNI real y versión de TDLib, QR,
almacenamiento cifrado, parseo de documentos y asignación de fuentes, además
de las pruebas del launcher. No autorizan una cuenta de Telegram ni leen grupos
del usuario en CI. El login, historial real y descarga de sus archivos requieren
validación después de escanear el QR en su TV.

Los captions `tirsodrive v1` no se interpretan como si fueran necesariamente
ROMs: se usa el nombre real del documento y su extensión. ZIP/7z se indexan como
archivos comprimidos; no se presupone que contengan una única ROM válida. La
reconstrucción de formatos especiales del uploader requiere revisar archivos reales.

Los núcleos de emulación, descompresión validada, ejecución, FPS, estados rápidos,
respaldos y el menú durante una partida todavía no están integrados en esta release.
Los pasos de verificación de correo/registro que Telegram pueda exigir fuera del
flujo QR habitual no tienen formulario propio aún; se informa del estado adicional.

## Procedencia

- TDLib oficial: https://github.com/tdlib/td, Boost Software License 1.0.
- Distribución Android: https://github.com/AkashPriyadarshii/tdlib-android.
- SHA-256 del AAR core 0.1.1 inspeccionado:
  `1ec6d6c6c83b655349d8a91182145cdd5400392246136ad79432253ad6004106`.
- ZXing core 3.5.3, Apache License 2.0, generación de QR en el dispositivo.
- Logos y atribuciones previas de Carbon conservados.
