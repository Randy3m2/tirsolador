# Compilar el núcleo integrado

JDK 17, Gradle 8.11.1, Android SDK 35, build-tools 35.0.0,
NDK 27.2.12479018, CMake 3.22.1. Android mínimo 8.0 (API 26).

```sh
bash tools/prepare_core.sh
python tools/make_test_rom.py app/src/androidTest/assets/diagnostic.nds
gradle :app:assembleDebug :app:lintDebug
# En emulador x86_64:
gradle -PtirsoAbis=x86_64 :app:connectedDebugAndroidTest
```

El archivo fuente de cada release ya contiene `app/src/main/cpp/upstream`.
En ese caso no es necesario ejecutar `prepare_core.sh`. El script descarga
exclusivamente la revisión fijada del repositorio original melonDS-emu/melonDS.

Prueba nativa independiente de Android (GCC/Clang, CMake >= 3.22):

```sh
cmake -S app/src/main/cpp -B build/native -DCMAKE_BUILD_TYPE=Release
cmake --build build/native -j2
python tools/make_test_rom.py build/diagnostic.nds
build/native/core_test build/diagnostic.nds
```

La ROM de diagnóstico se genera de código ARM propio, sin logo o código Nintendo;
se incluye solo en el APK de pruebas, no en el APK de usuario.

`engine.cpp` implementa la interfaz Platform de upstream, sin Qt/SDL, usando
archivos privados, hilos de C++ y audio/vídeo del frontend Android. JNI se llama
desde un único hilo por sesión. El menú pausa entre frames; salir destruye el
núcleo después de procesar las operaciones encoladas. Los guardados se escriben
con archivo temporal, fsync y rename.

Créditos y licencias: `app/src/main/assets/licenses/`, `LICENSE`,
`design/logos/CARBON-LICENSE.txt`, y avisos de los archivos upstream. Los logos
de consolas son recursos separados con las condiciones originales de Carbon;
los nombres y marcas pertenecen a sus titulares.
