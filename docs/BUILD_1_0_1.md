# Tirsolador 1.0.1 · Nintendo 64 y guardados

- Selección de configuración EGL compatible con el contexto OpenGL ES solicitado por N64, más resolución de funciones del controlador gráfico.
- Arranque completo del núcleo N64 antes de restaurar estados o respaldos de una sesión anterior.
- Partidas SRAM de tamaño variable restauradas por su contenido disponible. Antes se ignoraban silenciosamente si su tamaño difería del detectado al abrir el núcleo; esto podía afectar a GBA.
- Guardado de partidas al pausar, antes de guardar un estado y al salir. Las ROMs temporales siguen eliminándose; las partidas permanecen en almacenamiento privado.
- La sesión de emulación no se recrea al cambiar la configuración de controles, teclado, navegación o pantalla. Así se evita cerrar el núcleo y eliminar la ROM temporal durante esos cambios.
- Mandos registrados por dispositivo, con puertos estables al pausar y continuar. El remoto de TV ya no ocupa un puerto de jugador. N64 mantiene cuatro puertos y cuatro Controller Paks de memoria independientes.

Pruebas Android ampliadas: lectura de cuatro mandos mediante SI/PIF dentro del núcleo N64, SRAM y estados tras cerrar/reabrir N64 y GBA, restauración de SRAM más pequeña y estado DS tras eliminar/recrear la ROM temporal. Las herramientas de inspección de memoria se compilan exclusivamente en el APK de pruebas, nunca en el APK de publicación.

Instalar sobre **1.0.0**, sin desinstalar: misma firma y paquete. La actualización se prueba conservando UID y datos privados. No se incluyen ROMs comerciales ni BIOS. El comportamiento de cada juego y mando físico requiere validación en el televisor correspondiente.
