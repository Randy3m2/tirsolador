#!/usr/bin/env python3
import json,os,pathlib,subprocess,sys,shutil
ROOT=pathlib.Path(__file__).resolve().parents[1]
abi=sys.argv[1];assert abi in ['arm64-v8a','armeabi-v7a','x86','x86_64']
sdk=pathlib.Path(os.environ['ANDROID_HOME']);ndk=sdk/'ndk/27.2.12479018'
lock=json.loads((ROOT/'config/cores.json').read_text())
out=ROOT/'native-dist'/abi;out.mkdir(parents=True,exist_ok=True)
env=dict(os.environ,NDK_ROOT=str(ndk))
for name,core in lock['cores'].items():
    src=ROOT/'vendor'/name;build=ROOT/'native-build'/name/abi;build.mkdir(parents=True,exist_ok=True)
    print('BUILD',name,abi,flush=True)
    if name=='gba':
        cmake=sdk/'cmake/3.22.1/bin/cmake'
        args=[str(cmake),'-S',str(src),'-B',str(build),'-G','Ninja','-DCMAKE_MAKE_PROGRAM='+str(cmake.parent/'ninja'),'-DCMAKE_TOOLCHAIN_FILE='+str(ndk/'build/cmake/android.toolchain.cmake'),'-DANDROID_ABI='+abi,'-DANDROID_PLATFORM=android-26','-DCMAKE_BUILD_TYPE=Release','-DBUILD_LIBRETRO=ON','-DBUILD_QT=OFF','-DBUILD_SDL=OFF','-DBUILD_SHARED=OFF','-DBUILD_STATIC=OFF','-DBUILD_SUITE=OFF','-DBUILD_TEST=OFF','-DBUILD_DOCGEN=OFF','-DBUILD_MAINTAINER_TOOLS=OFF','-DENABLE_DEBUGGERS=OFF','-DENABLE_GDB_STUB=OFF','-DENABLE_SCRIPTING=OFF','-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384']
        args+=['-DUSE_'+f+'=OFF' for f in ['FFMPEG','MINIZIP','PNG','LIBZIP','SQLITE3','ELF','LUA','JSON_C','FREETYPE','LZMA','DISCORD_RPC','EPOXY']]
        subprocess.run(args,check=True,env=env)
        subprocess.run([str(cmake),'--build',str(build),'--target','mgba_libretro','-j','2'],check=True,env=env)
        binary=build/'mgba_libretro.so'
    else:
        args=[str(ndk/'ndk-build'),'-C',str(src/core['jni']),'-j2','NDK_PROJECT_PATH=.', 'APP_BUILD_SCRIPT=Android.mk','APP_ABI='+abi,'APP_PLATFORM=android-26','APP_STL=c++_static','APP_CPPFLAGS=-std=c++17 -fexceptions -frtti','APP_CFLAGS=-O2 -fcommon -Wno-error=implicit-function-declaration -Wno-error=incompatible-function-pointer-types','APP_LDFLAGS=-Wl,-z,max-page-size=16384','NDK_OUT='+str(build/'obj'),'NDK_LIBS_OUT='+str(build/'libs')]
        if name=='n64':args+=['HAVE_PARALLEL_RSP=0','HAVE_PARALLEL_RDP=0','HAVE_THR_AL=0','LLE=0','GLES3=0']
        subprocess.run(args,check=True,env=env)
        binary=build/'libs'/abi/'libretro.so'
    if not binary.is_file():raise RuntimeError('Missing core: '+str(binary))
    shutil.copy2(binary,out/('libtirso_'+name+'.so'))
shutil.copy2(ROOT/'config/cores.json',out/'cores.json')
