# Tirsolador

Launcher de juegos retro para Android TV, con biblioteca remota, descarga bajo demanda y emulación integrada.

## APK actual: 0.4.0 Preview — Nintendo DS integrado

El launcher ya tiene código Android nativo. Las [prereleases](https://github.com/Randy3m2/tirsolador/releases)
publican una APK de prueba con consolas, logos PNG, temas claro/oscuro, favoritos,
búsqueda, orden alfabético y tema de consola a pantalla completa. La versión 0.3
incorpora TDLib, bienvenida QR, grupos por consola, sincronización y descarga.
Consulta [configuración, funciones y límites de TirsoDrive](docs/BUILD_3.md).
La versión 0.4 incorpora el núcleo original melonDS dentro del APK: dos pantallas,
audio, entrada del control, menú con Atrás, estados y respaldos locales.
Consulta [funciones y límites de 0.4](docs/BUILD_4.md) y
[compilación nativa y fuentes](docs/NATIVE_BUILD.md). Nintendo 64 y las demás
consolas siguen pendientes. La sesión real requiere aprobar el login desde Telegram.
Los apartados siguientes describen el objetivo completo del proyecto.
Esta entrega se instala como **Tirsolador Preview** junto a la versión 0.3 para
conservar sus datos; necesita una nueva configuración de TirsoDrive y login QR.

## Objetivo

Crear una experiencia estilo Batocera/launcher de consola para televisores Android:

- Biblioteca organizada por consola y alfabéticamente.
- Catálogo remoto conectado a Telegram y TirsoDrive.
- Inicio de sesión mediante código QR.
- Descarga del juego bajo demanda y ejecución local.
- Favoritos, recientes, búsqueda y filtros.
- Información, carátula y metadatos de cada juego.
- Guardados normales y estados rápidos almacenados en el televisor.
- Copias de seguridad hacia Telegram y/o Google Drive.
- Perfiles de controles y remapeo por consola o juego.
- Interfaz optimizada para navegación con D-pad y gamepads.

## Consolas iniciales

- NES
- SNES
- Sega Mega Drive
- Game Boy
- Game Boy Color
- Game Boy Advance
- Nintendo 64
- MAME
- Nintendo DS

La arquitectura debe permitir agregar otras consolas sin rehacer el launcher.

## Repositorios de emuladores asignados

| Consola | Proyecto |
|---|---|
| Nintendo 64 | [mupen64plus](https://github.com/mupen64plus) |
| Nintendo DS | [melonDS](https://github.com/melonDS-emu/melonDS) |

Los demás emuladores se incorporarán cuando se definan sus repositorios. Antes de integrar cada proyecto se revisarán su licencia, arquitectura Android, sistema de build y compatibilidad con Android TV.

## Integraciones previstas

### Telegram

- Autenticación QR con TDLib.
- Lectura de canales autorizados.
- Indexación de archivos de juegos.
- Descarga progresiva y reanudable.
- Respaldo opcional de partidas.

### TirsoDrive

TirsoDrive será la capa de acceso y organización de archivos cuando su API esté disponible. Se documentarán aquí sus endpoints, credenciales de desarrollo y reglas de sincronización sin incluir secretos en Git.

### Metadatos

Se incorporará un servicio de scraping o API para obtener carátulas, descripción, género, año, región, jugadores y nombre normalizado. El catálogo permitirá corregir manualmente una identificación incorrecta.

## Arquitectura prevista

- Android TV como plataforma principal.
- Código nativo de Android con módulos separados para interfaz, catálogo, descargas, almacenamiento, autenticación e integración de emuladores.
- Núcleos de emulación integrados o adaptados desde proyectos compatibles con sus licencias.
- Soporte para ABI arm64-v8a y armeabi-v7a cuando sea necesario.
- Resolución y filtros configurables, con un perfil predeterminado de bajo consumo.
- Base de datos local para catálogo, favoritos, historial, descargas y partidas.

## Estructura propuesta

- `app/` — aplicación Android TV.
- `emulators/` — integraciones y adaptadores de emuladores.
- `catalog/` — modelos, indexación y metadatos.
- `telegram/` — autenticación, canales y descargas.
- `tirso-drive/` — cliente de TirsoDrive.
- `design/` — referencias visuales, wireframes y recursos aprobados.
- `docs/` — decisiones técnicas, instalación y pruebas.
- `tools/` — utilidades de desarrollo y validación.

## Reglas del repositorio

- No subir tokens, contraseñas, sesiones de Telegram ni claves privadas.
- Cada emulador externo debe conservar su licencia y atribución.
- Las imágenes de diseño y recursos se guardarán en carpetas identificadas.
- Los cambios importantes se documentarán en commits claros.
- Los APK de prueba y builds grandes se publicarán mediante Releases o artifacts, no dentro del historial Git.

## Estado actual

- Consolas iniciales definidas.
- Repositorios de Nintendo 64 y Nintendo DS registrados.
- Pendiente de recibir repositorios para NES, SNES, Mega Drive, Game Boy, Game Boy Color, Game Boy Advance y MAME.
- Pendiente de recibir imágenes y referencias visuales.
- Pendiente de documentar los canales o estructura de TirsoDrive.
