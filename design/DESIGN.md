# Dirección visual

Las dos imágenes proporcionadas por Randy son las referencias principales del launcher.

## Pantalla de consolas

La primera referencia define:

- Fondo oscuro azul marino.
- Tarjetas grandes con esquinas redondeadas.
- Una tarjeta por consola, organizada en una cuadrícula clara.
- Color fuerte y diferenciado por consola.
- Logotipo o nombre de la consola centrado.
- Interfaz legible a distancia en una televisión.
- Selección visible con foco de mando, brillo o borde elegante.
- Cabecera compacta con hora/estado y navegación simple.

La implementación no debe copiar elementos que no correspondan a las consolas iniciales. Cada tarjeta se alimentará desde una configuración de consola para poder cambiar logo, color y orden sin modificar la pantalla.

## Biblioteca de juegos

La segunda referencia define:

- Encabezado con consola y colección activa.
- Lista ordenada alfabéticamente.
- Cuadrícula de carátulas/thumbnails.
- Carátula como elemento principal de cada tarjeta.
- Nombre del juego visible sin saturar la interfaz.
- Contador de juegos y estado de navegación.
- Foco de mando claramente visible.
- Diseño compatible con pantallas grandes y navegación D-pad.

Al abrir un juego se mostrará una ficha con carátula, título normalizado, descripción, año, género, región, tamaño del archivo, estado de descarga, favoritos y acciones de jugar/continuar.

## Fuente de carátulas

Fuente propuesta:

- https://thumbnails.libretro.com/

La app deberá construir URLs de forma segura, usar caché local, evitar descargar imágenes repetidamente y mostrar un placeholder si una carátula no existe. Antes de incluir imágenes dentro de una distribución pública se revisarán las condiciones de uso, atribución y límites del servicio.

## Aplicación a las consolas iniciales

La misma dirección visual se aplicará a:

- NES
- SNES
- Sega Mega Drive
- Game Boy
- Game Boy Color
- Game Boy Advance
- Nintendo 64
- MAME
- Nintendo DS

La interfaz será propia de Tirsolador y utilizará las imágenes proporcionadas como guía visual, sin convertirlas automáticamente en recursos finales del APK.
