# Registro de emuladores

Este archivo registra los proyectos externos que se evaluarán para integración en Tirsolador.

## Nintendo 64

- Repositorio: https://github.com/mupen64plus
- Estado: asignado para evaluación
- Pendientes: identificar componentes Android, licencia de cada componente, método de compilación y adaptación de controles para Android TV.

## Nintendo DS

- Repositorio: https://github.com/melonDS-emu/melonDS
- Estado: asignado para evaluación
- Pendientes: identificar soporte Android/NDK, renderizado, audio, controles táctiles opcionales y compatibilidad con Android TV.

## Criterios de integración

Cada emulador deberá evaluarse por:

1. Licencia y obligaciones de distribución.
2. Soporte de Android y arquitecturas arm64-v8a/armeabi-v7a.
3. Integración como módulo, biblioteca nativa o proceso interno.
4. Guardados normales, estados rápidos y memoria de usuario.
5. Mapeo de controles, múltiples jugadores y desconexión/reconexión.
6. Rendimiento en televisores Android de gama baja.
7. Posibilidad de iniciar el juego desde la ficha del launcher sin exponer una interfaz externa.
