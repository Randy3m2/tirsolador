# Tirsolador

Launcher de juegos retro para Android TV, con biblioteca remota, descarga bajo demanda y emulación integrada.

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

La lista inicial prevista incluye:

- Nintendo 64
- Game Boy
- Game Boy Color
- Game Boy Advance
- Super Nintendo
- PlayStation 1

La arquitectura debe permitir agregar otras consolas sin rehacer el launcher.

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

Repositorio inicializado. Pendiente de recibir:

1. Repositorios GitHub de los emuladores.
2. Imágenes y referencias visuales.
3. Canales o estructura de TirsoDrive.
4. Lista exacta de dispositivos Android TV objetivo.
5. Primera consola para el prototipo funcional.
