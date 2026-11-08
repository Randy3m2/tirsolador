# Compilación nativa

JDK 17, Gradle 8.11.1, SDK 35, build-tools 35.0.0, NDK 27.2.12479018 y CMake 3.22.1. Android mínimo 8.0.

```sh
bash tools/prepare_core.sh
python tools/prepare_runtime_cores.py
bash tools/prepare_preview_key.sh
gradle :app:assembleRelease :app:lintRelease
```

`prepare_runtime_cores.py` requiere GitHub CLI autenticado para obtener los núcleos compilados desde las revisiones de `config/cores.json` en native-1. Para reconstruirlos: `python tools/fetch_cores.py`, seguido de `python tools/build_cores.py <ABI>` con el NDK instalado. Revisar `.github/workflows/cores.yml` para las opciones exactas de cada ABI. El código fuente de las entregas incluye `vendor/` y el upstream original de melonDS, con licencias y dependencias fijadas.

El frontend JNI carga los núcleos incluidos en el APK; no abre emuladores externos. melonDS utiliza el motor original mediante `engine.cpp`. Los demás núcleos usan la ABI libretro en `retro_host.cpp`, con audio estéreo, entrada independiente y OpenGL ES para N64. La salida cierra y guarda antes de eliminar ROMs temporales.

## Pruebas Android

```sh
python tools/make_test_rom.py app/src/androidTest/assets/diagnostic.nds
python tools/make_console_fixtures.py app/src/androidTest/assets
python tools/prepare_runtime_cores.py x86_64
bash tools/test_android.sh
```

Requiere un emulador/dispositivo x86_64. Los diagnósticos se incluyen exclusivamente en el APK de pruebas; Gridlee se obtiene de su distribución oficial y no se redistribuye. La prueba N64 propia incluye arranque MIPS, framebuffer VI y DMA AI, sin IPL propietario.

## Firma y actualizaciones

Se conserva el paquete `com.tirso.tirsolador.preview` de 0.4.0. La clave de esa instalación tiene una copia de recuperación privada. CI restaura la caché existente o el secreto opcional `TIRSO_SIGNING_KEY_B64`, verifica el certificado público de `config/signing-cert.sha256` y rechaza publicar si falta la clave o cambia la firma. Las pruebas usan claves efímeras explícitas. Nunca guardar claves privadas ni base64 de claves en Git.

La publicación requiere instalar 1.0.0 sobre preview-13 con `adb install -r`, verificar que conserva datos privados y abrir la aplicación. Los APK de producción tienen `debuggable=false`.
