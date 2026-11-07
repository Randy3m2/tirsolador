# Primera apertura: TirsoDrive

Requisito confirmado: cuando la integración real de Telegram esté disponible,
la primera apertura de Tirsolador debe mostrar la bienvenida de conexión antes
del catálogo. La captura suministrada identifica las fuentes; no es una pantalla
de chats que deba copiarse como diseño del launcher.

## Diseño moderno para Android TV

- Composición horizontal con mucho espacio, fondo azul marino profundo en Oscuro
  y blanco cálido en Claro; misma estructura en ambos modos.
- Marca Tirsolador discreta, título «Tu biblioteca retro, conectada» y texto breve.
- Columna izquierda con explicación y estado; panel derecho con QR real, de alto
  contraste, cuando TDLib entregue un token válido.
- Tipografía sans serif moderna y legible a distancia. Reservar la estética retro
  para los logos; evitar tipografía pixel en descripciones y controles.
- Acento turquesa, superficies suaves, bordes finos y foco visible con D-pad.
- Progreso de tres etapas: Conectar → Encontrar consolas → Biblioteca.
- El QR debe renovar su token al expirar. No mostrar QR decorativos ni simulados.
- Permitir cambiar Claro/Oscuro en la bienvenida.
- Instrucción: «Abre Telegram en tu teléfono → Ajustes → Dispositivos → Vincular
  dispositivo». Si Telegram solicita contraseña adicional, completar ese estado
  de autenticación antes de continuar.
- Mostrar estados comprensibles: esperando conexión, QR caducado, conectado,
  buscando grupos y conexión interrumpida, con reintento.

## Descubrimiento y asignación

Tras autenticar, buscar fuentes en chats principales y archivados de la cuenta.
Usar los nombres exactos de `config/telegram-sources.json` para la primera
identificación y guardar la asociación por ID real de chat, consola y cuenta.

- Coincidencia única: asociar automáticamente a la consola definida.
- Nombre duplicado: mostrar las coincidencias y permitir elegir el grupo correcto.
- Fuente ausente: marcar «Sin grupo asignado»; no buscar ROMs en otra consola.
- No depender del nombre después de asociar: un cambio de título no debe romper
  una asociación existente por ID.
- Mostrar resumen con logo, consola, grupo asociado y progreso de indexación.
- Nintendo 64 debe usar exclusivamente su fuente N64 y Nintendo DS la fuente DS;
  Genesis corresponde a Sega Mega Drive.
- Mantener visibles los estados de consolas sin fuente y ofrecer configurarlas
  después; no confundir «sin grupo» con «consola oculta».
- Incluir «Ir a mi biblioteca» cuando termine la configuración, conservando los
  estados de indexación pendientes sin bloquear toda la interfaz.
- Marcar la bienvenida completada sólo después de autenticar y persistir las
  asociaciones. Al volver a abrir con sesión válida, mostrar las consolas.
- Si se cierra la sesión o cambia la cuenta, volver al flujo de conexión y no
  reutilizar IDs de la cuenta anterior sin validarlos.

## Ajustes posteriores

En Ajustes > TirsoDrive: estado de sesión, volver a conectar, cerrar sesión,
grupos por consola, corregir una asignación y actualizar catálogo.

## Pulido del launcher

- Mantener logos PNG transparentes con su proporción original.
- Dar más espacio entre cabecera, navegación y tarjetas; evitar aspecto de tabla.
- Tarjetas de consolas con superficies discretas y acentos propios, en vez de
  bloques de color estridente. Foco con contorno y elevación moderados.
- Biblioteca con carátulas sin recortes, título del juego seleccionado y ficha
  lateral. Mantener búsqueda y favoritos en posiciones predecibles.
- Animaciones breves y ligeras, sin sacrificar respuesta del mando en TVs modestas.
- Conservar foco y posición al regresar desde la ficha o cambiar el modo de color.

## Estado

Especificación para la próxima integración. La APK 0.1.0 Preview conserva su
estado actual: no autentica Telegram ni indexa esos grupos todavía.
