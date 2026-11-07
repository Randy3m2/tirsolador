# Tirsolador 0.2.0 Preview — segundo build

## Cambios disponibles

- Launcher sin marca textual, títulos, avisos de catálogo ni instrucciones del mando.
- Sin botón Consolas; favoritos y ajustes mediante iconos con etiquetas accesibles.
- Tarjetas más grandes, logos PNG y sólo contador de juegos debajo.
- Biblioteca compacta: seis columnas en Android TV de 960 dp de ancho, cinco en
  pantallas más pequeñas, conservando las proporciones de las carátulas.
- Búsqueda, refresco y orden alfabético en la barra superior mediante iconos.
- Orden A–Z / Z–A persistente.
- Selector de temas en Ajustes y tema **Consola completa**: logo grande centrado
  de una consola, navegación a izquierda/derecha y entrada a su biblioteca.
- Modos Claro/Oscuro para ambas vistas de inicio.
- Eliminada la importación JSON de las pantallas y su flujo de documentos.
- Retorno de la ficha a la biblioteca conservando foco y desplazamiento.
- Estado de entradas separado por dispositivo para D-pad por teclas, D-pad HAT,
  stick izquierdo y derecho; controles de navegación sin modificar esos orígenes.
- Perfiles de zona muerta por VID/PID/descriptor y diagnóstico de ejes.
- Identificación de DualSense, DualSense Edge, DualShock 4, 8BitDo Ultimate 2C y
  Switch Pro cuando Android expone sus IDs/nombres conocidos; fallback genérico.
- Eliminación del estado temporal de entrada al desconectar un mando.

## Límites de esta preview

El refresco recarga el catálogo **local**; todavía no sincroniza Telegram y lo
indica al usarlo. La biblioteca sigue siendo de muestra o el catálogo local
conservado de la versión anterior. No contiene ROMs.

Los núcleos, ejecución de juegos, menú durante la partida, FPS, estados rápidos,
backups, remapeo para emulación y conexión QR siguen pendientes. El menú del
botón Atrás durante un juego se conectará al ciclo de vida real del núcleo;
esta APK todavía no tiene una sesión de emulación que pausar.

Los perfiles separan la entrada y permiten navegar; no garantizan funciones
específicas de todos los mandos ni equivalen a pruebas físicas de cada modelo.
Las pruebas de Android automatizadas no sustituyen la validación por USB y
Bluetooth en los televisores del usuario.

## Instalación y actualización

Descargar `Tirsolador-v0.2.0-preview.apk` e instalar sobre la preview anterior.
El applicationId se conserva y versionCode aumenta a 2; temas, favoritos y
consolas visibles se mantienen. Se usa la firma de depuración restaurada del
caché de GitHub Actions, todavía no una firma de producción.

## Validación de CI

Compilación, lint y pruebas instrumentadas en Android 10. Se publican capturas
reales de launcher, biblioteca y tema de consola completa como artifacts de CI.
Las pruebas cubren navegación de las nuevas barras, persistencia del modo de
color y tema, orden alfabético, catálogo inválido y separación de entradas.

## Recursos

Los logos mantienen la atribución y licencia de Carbon descrita en
`design/logos/CARBON-LICENSE.txt`. Carátulas bajo demanda de Libretro; no se
deforman ni se incluyen ROMs en el APK.
