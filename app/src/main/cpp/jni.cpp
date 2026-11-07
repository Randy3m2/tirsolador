// SPDX-License-Identifier: GPL-3.0-or-later
#include "engine.h"
#include <jni.h>
static void fail(JNIEnv* e,const char* s){e->ThrowNew(e->FindClass("java/io/IOException"),s);}
static std::string str(JNIEnv* e,jstring s){const char* p=e->GetStringUTFChars(s,nullptr);std::string r(p);e->ReleaseStringUTFChars(s,p);return r;}
extern "C" JNIEXPORT jlong JNICALL Java_com_tirso_tirsolador_DsCore_open(JNIEnv* e,jclass,jstring rom,jstring save){try{return reinterpret_cast<jlong>(new Engine(str(e,rom),str(e,save)));}catch(const std::exception& x){fail(e,x.what());return 0;}}
extern "C" JNIEXPORT jint JNICALL Java_com_tirso_tirsolador_DsCore_frame(JNIEnv* e,jclass,jlong h,jint keys,jint x,jint y,jboolean touch,jintArray pixels,jshortArray audio){
 if(!h||e->GetArrayLength(pixels)<256*384||e->GetArrayLength(audio)<4096){fail(e,"Buffer inválido");return -1;}
 unsigned image[256*384];short samples[4096];int n=0;try{if(!reinterpret_cast<Engine*>(h)->frame(keys,x,y,touch,image,samples,n))return -1;}catch(const std::exception& z){fail(e,z.what());return -1;}
 e->SetIntArrayRegion(pixels,0,256*384,reinterpret_cast<jint*>(image));e->SetShortArrayRegion(audio,0,n*2,samples);return n*2;
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_com_tirso_tirsolador_DsCore_snapshot(JNIEnv* e,jclass,jlong h){try{auto b=reinterpret_cast<Engine*>(h)->state();auto a=e->NewByteArray(b.size());e->SetByteArrayRegion(a,0,b.size(),reinterpret_cast<jbyte*>(b.data()));return a;}catch(const std::exception& z){fail(e,z.what());return nullptr;}}
extern "C" JNIEXPORT jboolean JNICALL Java_com_tirso_tirsolador_DsCore_restore(JNIEnv* e,jclass,jlong h,jbyteArray a){int n=e->GetArrayLength(a);if(n<16||n>64*1024*1024)return false;std::vector<unsigned char>b(n);e->GetByteArrayRegion(a,0,n,reinterpret_cast<jbyte*>(b.data()));try{return reinterpret_cast<Engine*>(h)->load(b);}catch(const std::exception& z){fail(e,z.what());return false;}}
extern "C" JNIEXPORT void JNICALL Java_com_tirso_tirsolador_DsCore_close(JNIEnv*,jclass,jlong h){delete reinterpret_cast<Engine*>(h);}
