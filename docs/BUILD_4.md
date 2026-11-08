# Tirsolador 0.4.0 preview · Nintendo DS integrado

Nintendo DS ahora se ejecuta dentro de Tirsolador con el código original de
[melonDS](https://github.com/melonDS-emu/melonDS), fijado en
`497aa6cdd26f45f8064b78916b00a773eab4ded7`. No requiere otra APK.

## En esta versión

- Descargar y abrir juegos DS desde el catálogo TirsoDrive. Los archivos ya
  descargados pueden abrirse sin volver a descargarlos.
- Dos pantallas lado a lado, resolución nativa 256 × 192 cada una, renderer de
  software, audio estéreo 48 kHz. ARM de 32/64 bits y x86 de 32/64 bits.
- Atrás del remoto abre el menú pausado: información, FPS, guardar/cargar estado
  (ranura 1), respaldar/restaurar partidas, reiniciar y salir.
- Partidas locales separadas por SHA-256 de ROM. Los estados comprueban juego,
  versión del núcleo e integridad antes de cargarse.
- Exportar/importar respaldos ZIP mediante el selector de archivos de Android.
  Un proveedor de documentos de Google Drive puede aparecer si está instalado;
  no hay sincronización automática con Drive ni subida a Telegram todavía.
- Entrada por dispositivo: cruceta, HAT y stick izquierdo mantienen estados
  independientes. Stick derecho posiciona cursor; R3 presiona la pantalla táctil.
- ROM `.nds` o `.zip` con una sola ROM DS. No se ejecutan archivos `.7z`.

## Límites de esta preview

El intérprete está activado y el JIT desactivado en todas las arquitecturas.
Los juegos pesados pueden ir lentos en TVs modestas; mostrar FPS ayuda a evaluar
el rendimiento real. No se promete velocidad completa ni compatibilidad de todos
los juegos/controles. Los perfiles se reconocen por el sistema Android; falta
validación física de DualSense, Edge, DualShock 4, Ultimate 2C y Switch Pro.

Nintendo 64 y los otros núcleos siguen pendientes. DSi, cámara, micrófono real,
rumble y multijugador no están habilitados. Se usan FreeBIOS y firmware generado
de upstream, sin BIOS propietarias ni ROMs comerciales incluidas.

## Validación y fuentes

La CI prueba el núcleo nativo en Android con una ROM de diagnóstico propia:
arranque, imagen, cambio rojo/verde por botón A, salida de audio y recuperación
de estado; también respalda/restaura, rechaza archivos inválidos, prueba ZIP,
separación de controles y apertura del menú con Atrás. Las pruebas previas del
launcher y TDLib siguen ejecutándose. Estas pruebas no sustituyen una prueba
de rendimiento en la TV.

La release incluye el código fuente correspondiente de Tirsolador y melonDS,
con instrucciones de compilación y licencias. Código de integración GPL-3.0 o
posterior; los recursos conservan sus licencias específicas.

Esta entrega se instala como **Tirsolador Preview**, con identificador
`com.tirso.tirsolador.preview`, junto a la versión 0.3. La clave usada para firmar
0.3 no se conservó: Android no admite actualizarla con una firma diferente.
No desinstales la versión anterior; su sesión, catálogo, descargas y ajustes
permanecen intactos. Configura TirsoDrive y aprueba un nuevo QR en la preview.

La ruta de firma está ahora fijada explícitamente y la CI conserva la clave de
preview en su caché. Una caché puede caducar o eliminarse: para distribución
permanente hace falta una clave de producción guardada en Secrets, no en Git.
