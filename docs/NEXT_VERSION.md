# Siguiente versión: requisitos confirmados

Estos cambios son para la siguiente versión. No modificar la APK ya publicada,
interrumpir trabajos en curso ni lanzar otro build como parte de esta anotación.
Este documento actualiza las especificaciones anteriores cuando entren en conflicto.

## Launcher de consolas

- Eliminar de esta pantalla la marca textual «TIRSOLADOR», «Tus consolas»,
  el aviso «Vista previa del catálogo» y todas las instrucciones del mando.
- Eliminar el botón «Consolas».
- Sustituir «Favoritos» por un botón de estrella y «Ajustes» por un engranaje.
- Los botones deben conservar nombres de accesibilidad y foco visible aunque no
  muestren etiquetas textuales.
- Eliminar los nombres bajo los logos: conservar únicamente el contador de juegos.
- Contar juegos reales del catálogo de cada consola; no mezclar la cuenta del
  catálogo de muestra con la biblioteca de Telegram.
- Aprovechar el espacio liberado para agrandar las tarjetas y los logos.
- Modernizar las superficies, espaciado, bordes y transiciones; conservar
  proporciones de los PNG, contraste y respuesta rápida en televisores modestos.
- Conservar los modos claro/oscuro y las consolas visibles.
- Eliminar la opción de importar catálogo JSON de la interfaz de usuario,
  tanto de biblioteca como de ajustes. La biblioteca se alimentará de las
  fuentes de TirsoDrive asignadas.

## Controles prioritarios

- Sony PS5 DualSense
- Sony DualSense Edge
- Sony DualShock 4
- 8BitDo Ultimate 2C
- Nintendo Switch Pro Controller

Objetivo: soporte amplio de mandos reconocidos por Android, con validación
específica de los modelos anteriores. No considerar garantizadas todas sus
funciones sin pruebas de Bluetooth/USB y de las versiones Android objetivo.

### Perfiles e independencia de entradas

- Perfil propio por modelo, identificado por vendor/product ID y capacidades
  reales; evitar depender exclusivamente del nombre visible del dispositivo.
- Separar asignación persistente por dispositivo y asignación temporal a jugador.
- Preservar remapeos por consola y juego sin mezclarlos entre modelos.
- Distinguir D-pad de stick izquierdo/derecho: teclas direccionales y HAT_X/HAT_Y
  frente a AXIS_X/AXIS_Y y ejes del stick derecho.
- Configurar zonas muertas y umbrales por eje/dispositivo.
- Evitar eventos duplicados cuando Android reporta el D-pad como teclas y HAT.
- El movimiento del stick no debe sobrescribir el mapeo del D-pad ni viceversa.
- La navegación del launcher puede aceptar ambos, manteniendo su origen separado.
- Detectar conexión/desconexión sin reasignar accidentalmente el jugador.
- Mantener perfiles distintos si un mismo mando presenta IDs/capacidades
  diferentes en Bluetooth y USB o en modos de compatibilidad.
- Incluir diagnóstico de botones/ejes y remapeo manual para mandos desconocidos.
- No asumir funciones especiales, vibración o botones adicionales expuestos
  por igual en todos los modos y dispositivos Android.

## Menú durante el juego

El botón Atrás del remoto de TV abre un menú de Tirsolador sobre la partida.
Debe pausar la emulación y el audio, conservar el estado de entrada y permitir
cerrarlo para continuar. Fuera de una partida, Atrás conserva su navegación normal.

Acciones requeridas:

1. Ver información del juego.
2. Mostrar FPS (activar/desactivar).
3. Save game state (selección de ranura).
4. Load game state (selección de ranura).
5. Back up saves.
6. Restore saves.
7. Restart game.
8. Exit game.

- Evitar que la pulsación que abre/cierra el menú también llegue al juego.
- Diferenciar guardados normales/tarjetas de memoria de estados rápidos.
- Guardar estados con ID del juego, núcleo y versión para validar compatibilidad.
- Los backups deben llevar un manifiesto; restaurar sólo tras validar destino y
  compatibilidad y preservar una copia previa antes de reemplazar partidas.
- Pedir confirmación en restauración que sobrescriba partidas y en reinicio/salida
  si puede perderse progreso.
- Si una función no existe en un núcleo, mostrarla como no disponible con motivo;
  no simular éxito.
- El FPS debe medir el flujo de emulación; no confundirlo con FPS del launcher.
- Las operaciones de guardado deben ejecutarse con el núcleo en un punto seguro.
- Tras salir, regresar al juego seleccionado en la biblioteca, conservando foco
  y desplazamiento.

## Núcleos dentro de Tirsolador

El APK debe integrar las compilaciones de los proyectos de emulación seleccionados,
mediante bibliotecas nativas y adaptadores dentro del proceso/aplicación según sus
requisitos. El usuario no debe instalar ni abrir APKs externas de emuladores.

Tirsolador será propietario de la experiencia visible: arranque de la ROM, pantalla,
entrada del mando, menú de pausa, configuración, FPS, guardados y retorno a biblioteca.
Esto no cambia la autoría ni las obligaciones de licencia de los núcleos.

- Nintendo 64: familia Mupen64Plus; identificar los repositorios concretos del core
  y plugins de vídeo/audio/entrada necesarios dentro de la organización suministrada.
- Nintendo DS: melonDS, https://github.com/melonDS-emu/melonDS .
- Los demás núcleos quedan pendientes de elección de repositorios.
- Compilar para las ABI Android pertinentes; evaluar 32 y 64 bits por núcleo.
- Mantener dependencias, versiones y commits fijados, con builds reproducibles.
- Conservar licencias, atribuciones y código fuente exigido por cada componente.
- Establecer un contrato común de iniciar/pausar/reanudar/parar, vídeo/audio,
  entradas, guardados normales y estados; adaptar cada emulador a ese contrato.
- Evitar que cambios de UI o de foco bloqueen el hilo de emulación.
- No dar una consola por integrada hasta probar el recorrido real:
  descargar → arrancar → mando → menú → guardar/cargar → salir → volver a abrir.

## Estado

Requisitos guardados para implementación. Este documento no afirma que los núcleos,
los nuevos perfiles de mandos o el menú durante el juego estén implementados en
la APK 0.1.0 Preview.
