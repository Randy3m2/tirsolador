# Tirsolador 0.1.0 Preview

Primera APK de prueba del launcher Android TV. Android 6.0 o superior.

## Disponible

- Inicio con nueve consolas y logos PNG transparentes incluidos en el APK.
- Interfaz horizontal nativa y foco para D-pad, OK y botones A/B.
- Ajustes de apariencia Claro/Oscuro con persistencia.
- Acentos Moderno, GBA clásico, Nintendo rojo y Retro naranja.
- Mostrar/ocultar consolas y restaurar visibilidad.
- Biblioteca alfabética, búsqueda, fichas y favoritos persistentes.
- Carátulas de Libretro con caché de memoria/disco y fallback sin conexión.
- Catálogo de muestra identificado como tal; no incluye ROMs.
- Importar un catálogo JSON local usando el selector de documentos de Android.
- Diagnóstico de controles conectados.

## Pendiente

Esta preview todavía **no ejecuta juegos**. No contiene núcleos de emulación,
descargas de ROMs, sesiones de Telegram, login QR, sincronización o backups.
Mupen64Plus y melonDS están seleccionados como proyectos para integración;
sus núcleos todavía no forman parte de esta APK. Los demás núcleos siguen por definir.

## Instalación

Descargar `Tirsolador-v0.1.0-preview.apk` de esta release e instalar en Android TV
permitiendo instalación desde la aplicación que abre el archivo. También puede
instalarse con `adb install -r Tirsolador-v0.1.0-preview.apk`.
El APK usa firma de depuración para pruebas, no firma de producción.
Las compilaciones intentan reutilizar la firma mediante caché de Actions;
para garantizar actualizaciones futuras se necesita una clave de distribución permanente.

## Build

JDK 17, Gradle 8.11.1, Android SDK 35 / Build Tools 35.0.0.
Ejecutar `gradle :app:assembleDebug :app:lintDebug` desde la raíz.
GitHub Actions genera el APK, informes de lint y una prerelease por push a main.

## Catálogo local

Copiar `app/src/main/assets/demo-catalog.json` como ejemplo y editar los juegos.
`schemaVersion` es 1. Cada juego requiere ID único, título y consola
(`NES`, `SNES`, `MD`, `GB`, `GBC`, `GBA`, `N64`, `MAME`, `NDS`).
`coverName` debe coincidir exactamente con el nombre del PNG en Libretro, sin extensión.
El import reemplaza el catálogo sólo después de validarlo; conserva favoritos por ID.
No incluye importación de ROMs ni arranque de emuladores.

## Validación

El workflow compila el APK y el APK de pruebas y ejecuta lint. Las pruebas
instrumentadas se ejecutan en el job smoke-test en un emulador Android.
Falta validación física en los televisores y mandos del usuario.

## Recursos

Logos procedentes de RetroPie/es-theme-carbon, commit
`b09973e0b0c589cb11fe772c169a6ff5d588b390`, autor Rookervik, basado en Nils Bonenberger.
SVG originales preservados en `design/logos`; conversión a PNG transparente para Android.
Se conserva el aviso upstream en `design/logos/CARBON-LICENSE.txt`.
Los recursos Carbon están bajo CC BY-NC-SA; las marcas pertenecen a sus propietarios.
Carátulas servidas bajo demanda desde https://thumbnails.libretro.com/.
