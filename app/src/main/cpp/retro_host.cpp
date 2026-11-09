// SPDX-License-Identifier: GPL-3.0-or-later
// Tirsolador frontend for the public libretro C API. Cores remain separate DSOs.
#include "libretro.h"
#include <jni.h>
#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstdarg>
#include <cstring>
#include <algorithm>
#include <fstream>
#include <memory>
#include <mutex>
#include <map>
#include <vector>
#include <string>
#include <stdexcept>
#include <unistd.h>

namespace {
constexpr unsigned MAX_WIDTH=1024,MAX_HEIGHT=1024;
std::mutex sessionMutex;
struct Host;
Host* active=nullptr; // One lifetime lease, including callbacks on core-owned threads.
bool diagnosticLogging=false;
struct Host {
 std::unique_lock<std::mutex> lease;
 void* library=nullptr;
 std::string rom,save,system,core,logMessage;
 std::map<std::string,std::string> variables;
 std::vector<unsigned char> content;
 std::vector<uint32_t> pixels;
 std::vector<int16_t> audio;
 std::vector<unsigned char> previousSave;
 std::mutex audioMutex;
 retro_pixel_format format=RETRO_PIXEL_FORMAT_0RGB1555;
 retro_system_av_info av{};
 retro_system_info info{};
 retro_hw_render_callback hw{};
 EGLDisplay display=EGL_NO_DISPLAY;
 EGLSurface surface=EGL_NO_SURFACE;
 EGLContext context=EGL_NO_CONTEXT;
 GLuint fbo=0,texture=0,depth=0,stencil=0;
 uint16_t keys[4]{};
 int16_t axes[4][4]{};
 unsigned rotation=0;
 unsigned width=0,height=0,frames=0;
 double audioPhase=0;
 bool initialized=false,loaded=false,shutdown=false,hardware=false,contextReady=false,saveReady=false;
 void (*init)();void(*deinit)();void(*run)();void(*unload)();
 bool(*loadGame)(const retro_game_info*);size_t(*serializeSize)();bool(*serialize)(void*,size_t);bool(*unserialize)(const void*,size_t);
 void*(*memoryData)(unsigned);size_t(*memorySize)(unsigned);
 void(*getAV)(retro_system_av_info*);
 template<class T>T symbol(const char* n){auto p=dlsym(library,n);if(!p)throw std::runtime_error(std::string("Núcleo incompleto: ")+n);return reinterpret_cast<T>(p);}
 static bool environment(unsigned,void*);
 static void video(const void*,unsigned,unsigned,size_t);
 static size_t batch(const int16_t*,size_t);
 static void sample(int16_t l,int16_t r){int16_t data[2]={l,r};batch(data,1);}
 static void poll(){}
 static int16_t input(unsigned port,unsigned device,unsigned index,unsigned id){
  Host* h=active;if(!h||port>=4)return 0;device&=RETRO_DEVICE_MASK;
  if(device==RETRO_DEVICE_JOYPAD){if(id==RETRO_DEVICE_ID_JOYPAD_MASK)return h->keys[port];return id<16?((h->keys[port]>>id)&1):0;}
  if(device==RETRO_DEVICE_ANALOG&&index<2&&id<2)return h->axes[port][index*2+id];return 0;
 }
 static void log(enum retro_log_level level,const char* fmt,...){
  if((level<RETRO_LOG_WARN&&!diagnosticLogging)||!active)return;char text[1024];va_list a;va_start(a,fmt);vsnprintf(text,sizeof(text),fmt,a);va_end(a);
  if(level>=RETRO_LOG_WARN)active->logMessage=text;__android_log_print(level>=RETRO_LOG_WARN?ANDROID_LOG_WARN:ANDROID_LOG_INFO,"TirsoCore","%s",text);
 }
 static uintptr_t framebuffer(){return active?active->fbo:0;}
 static retro_proc_address_t proc(const char* name){auto p=eglGetProcAddress(name);return p?reinterpret_cast<retro_proc_address_t>(p):reinterpret_cast<retro_proc_address_t>(dlsym(RTLD_DEFAULT,name));}
 Host(const std::string& libraryPath,const std::string& path,const std::string& saveDir,const std::string& systemDir,const std::string& id):lease(sessionMutex),rom(path),save(saveDir),system(systemDir),core(id){
  active=this;pixels.resize(MAX_WIDTH*MAX_HEIGHT);
  variables={{"mupen64plus-pak1","memory"},{"mupen64plus-pak2","memory"},{"mupen64plus-pak3","memory"},{"mupen64plus-pak4","memory"},{"mupen64plus-43screensize","320x240"},{"mupen64plus-169screensize","640x360"},{"mupen64plus-rdp-plugin","gliden64"},{"mupen64plus-rsp-plugin","hle"},{"mupen64plus-txFilterMode","None"},{"mupen64plus-txEnhancementMode","None"},{"mupen64plus-ThreadedRenderer","False"},{"mupen64plus-EnableFBEmulation","True"},{"mupen64plus-FrameDuping","False"},{"mame2003-plus_skip_disclaimer","enabled"},{"mame2003-plus_skip_warnings","enabled"}};
  try{
   library=dlopen(libraryPath.c_str(),RTLD_NOW|RTLD_LOCAL);if(!library)throw std::runtime_error("No se pudo cargar el núcleo integrado para esta arquitectura");
   auto version=symbol<unsigned(*)()>("retro_api_version");if(version()!=RETRO_API_VERSION)throw std::runtime_error("Versión de núcleo incompatible");
   init=symbol<void(*)()>("retro_init");deinit=symbol<void(*)()>("retro_deinit");run=symbol<void(*)()>("retro_run");unload=symbol<void(*)()>("retro_unload_game");
   loadGame=symbol<bool(*)(const retro_game_info*)>("retro_load_game");getAV=symbol<void(*)(retro_system_av_info*)>("retro_get_system_av_info");
   serializeSize=symbol<size_t(*)()>("retro_serialize_size");serialize=symbol<bool(*)(void*,size_t)>("retro_serialize");unserialize=symbol<bool(*)(const void*,size_t)>("retro_unserialize");
   memoryData=symbol<void*(*)(unsigned)>("retro_get_memory_data");memorySize=symbol<size_t(*)(unsigned)>("retro_get_memory_size");
   symbol<void(*)(retro_environment_t)>("retro_set_environment")(environment);
   symbol<void(*)(retro_video_refresh_t)>("retro_set_video_refresh")(video);
   symbol<void(*)(retro_audio_sample_t)>("retro_set_audio_sample")(sample);
   symbol<void(*)(retro_audio_sample_batch_t)>("retro_set_audio_sample_batch")(batch);
   symbol<void(*)(retro_input_poll_t)>("retro_set_input_poll")(poll);
   symbol<void(*)(retro_input_state_t)>("retro_set_input_state")(input);
   init();initialized=true;symbol<void(*)(retro_system_info*)>("retro_get_system_info")(&info);
   retro_game_info game{rom.c_str(),nullptr,0,nullptr};
   if(!info.need_fullpath){
    std::ifstream f(rom,std::ios::binary|std::ios::ate);auto n=f.tellg();if(n<1||n>512LL*1024*1024)throw std::runtime_error("Archivo de juego inválido");
    content.resize(static_cast<size_t>(n));f.seekg(0);if(!f.read(reinterpret_cast<char*>(content.data()),n))throw std::runtime_error("Archivo de juego incompleto");game.data=content.data();game.size=content.size();
   }
   // Clean-room homebrew bootstrap can execute directly from cartridge ROM.
   // Mupen's dynarec rejects that address range; its cached interpreter supports it.
   if(core=="n64"&&content.size()>=64&&std::all_of(content.begin()+0x10,content.begin()+0x18,[](unsigned char b){return b==0;}))variables["mupen64plus-cpucore"]="cached_interpreter";
   if(!loadGame(&game))throw std::runtime_error(core=="mame"?"MAME no pudo abrir el juego. Verifica el nombre original del ZIP, el romset 2003-Plus y sus archivos BIOS/parent requeridos.":"El núcleo no pudo abrir el juego. Comprueba el archivo y los archivos de sistema requeridos.");
   loaded=true;getAV(&av);if(av.timing.fps<1||av.timing.fps>300||av.timing.sample_rate<1000||av.timing.sample_rate>192000)throw std::runtime_error("El núcleo devolvió un formato de vídeo/audio inválido");
   if(hardware&&hw.context_reset){hw.context_reset();contextReady=true;}
   auto setDevice=symbol<void(*)(unsigned,unsigned)>("retro_set_controller_port_device");
   unsigned ports=(core=="n64"||core=="mame")?4:(core=="gb"||core=="gba")?1:2;
   for(unsigned port=0;port<ports;++port)setDevice(port,RETRO_DEVICE_JOYPAD);
   // Hardware cores finish startup in their first coroutine frame. Cold state
   // restoration and controller binding must happen after that initialization.
   if(core=="n64"){run();for(unsigned port=0;port<ports;++port)setDevice(port,RETRO_DEVICE_JOYPAD);audio.clear();}
   restoreBattery();
  }catch(...){cleanup();throw;}
 }
 void initGL(){
  if(display!=EGL_NO_DISPLAY)return;
  display=eglGetDisplay(EGL_DEFAULT_DISPLAY);if(display==EGL_NO_DISPLAY||!eglInitialize(display,nullptr,nullptr))throw std::runtime_error("No se pudo iniciar OpenGL ES en esta TV");
  EGLint configAttrs[]={EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RENDERABLE_TYPE,hw.context_type==RETRO_HW_CONTEXT_OPENGLES3?0x0040:EGL_OPENGL_ES2_BIT,EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_NONE};
  EGLConfig config;EGLint count=0;if(!eglChooseConfig(display,configAttrs,&config,1,&count)||count<1)throw std::runtime_error("La TV no ofrece un contexto OpenGL ES compatible");
  EGLint size[]={EGL_WIDTH,static_cast<EGLint>(MAX_WIDTH),EGL_HEIGHT,static_cast<EGLint>(MAX_HEIGHT),EGL_NONE};surface=eglCreatePbufferSurface(display,config,size);
  EGLint ctx[]={EGL_CONTEXT_CLIENT_VERSION,hw.context_type==RETRO_HW_CONTEXT_OPENGLES3?3:2,EGL_NONE};context=eglCreateContext(display,config,EGL_NO_CONTEXT,ctx);
  if(surface==EGL_NO_SURFACE||context==EGL_NO_CONTEXT||!eglMakeCurrent(display,surface,surface,context))throw std::runtime_error("No se pudo crear la superficie de Nintendo 64");
  glGenTextures(1,&texture);glBindTexture(GL_TEXTURE_2D,texture);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,MAX_WIDTH,MAX_HEIGHT,0,GL_RGBA,GL_UNSIGNED_BYTE,nullptr);
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
  glGenFramebuffers(1,&fbo);glBindFramebuffer(GL_FRAMEBUFFER,fbo);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,texture,0);
  if(hw.depth){glGenRenderbuffers(1,&depth);glBindRenderbuffer(GL_RENDERBUFFER,depth);glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH_COMPONENT16,MAX_WIDTH,MAX_HEIGHT);glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,depth);}
  if(hw.stencil){glGenRenderbuffers(1,&stencil);glBindRenderbuffer(GL_RENDERBUFFER,stencil);glRenderbufferStorage(GL_RENDERBUFFER,GL_STENCIL_INDEX8,MAX_WIDTH,MAX_HEIGHT);glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_STENCIL_ATTACHMENT,GL_RENDERBUFFER,stencil);}
  if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw std::runtime_error("Framebuffer de emulación no compatible");
 }
 void restoreBattery(){
  auto size=memorySize(RETRO_MEMORY_SAVE_RAM);void* data=memoryData(RETRO_MEMORY_SAVE_RAM);
  if(!data||!size){saveReady=true;return;}if(size>32*1024*1024)throw std::runtime_error("Partida demasiado grande");
  std::ifstream in(save+"/battery.sav",std::ios::binary|std::ios::ate);
  if(in){auto stored=in.tellg();if(stored<1||stored>32*1024*1024)throw std::runtime_error("Partida guardada inválida; se conservó el archivo original");
   // SRAM can change size after autodetection (notably mGBA). Restore the
   // available prefix instead of silently discarding smaller valid saves.
   size_t count=std::min(size,static_cast<size_t>(stored));in.seekg(0);
   if(!in.read(static_cast<char*>(data),count))throw std::runtime_error("No se pudo leer la partida guardada");
   previousSave.assign(static_cast<unsigned char*>(data),static_cast<unsigned char*>(data)+size);
  }
  saveReady=true;
 }
 bool flush(){
  if(!loaded||!saveReady)return true;auto size=memorySize(RETRO_MEMORY_SAVE_RAM);void* data=memoryData(RETRO_MEMORY_SAVE_RAM);
  if(!data||!size)return true;if(size>32*1024*1024)return false;
  if(previousSave.size()==size&&memcmp(previousSave.data(),data,size)==0)return true;
  std::string target=save+"/battery.sav",temp=target+".tmp";FILE* f=fopen(temp.c_str(),"wb");if(!f)return false;
  bool ok=fwrite(data,1,size,f)==size&&fflush(f)==0&&fsync(fileno(f))==0;if(fclose(f)!=0)ok=false;
  if(ok)ok=rename(temp.c_str(),target.c_str())==0;if(!ok){unlink(temp.c_str());return false;}
  previousSave.assign(static_cast<unsigned char*>(data),static_cast<unsigned char*>(data)+size);return true;
 }
 std::vector<unsigned char> snapshot(){auto n=serializeSize();if(!n||n>64*1024*1024)throw std::runtime_error("Este juego no admite estados o el estado supera el límite de memoria");std::vector<unsigned char>b(n);if(!serialize(b.data(),n))throw std::runtime_error("No se pudo guardar el estado del juego");return b;}
 bool restore(const std::vector<unsigned char>& b){if(b.size()!=serializeSize())return false;auto old=snapshot();if(!unserialize(b.data(),b.size())){unserialize(old.data(),old.size());return false;}previousSave.clear();return flush();}
 void cleanup(){
  if(loaded){flush();unload();loaded=false;}
  if(contextReady&&hw.context_destroy){hw.context_destroy();hw.context_destroy=nullptr;contextReady=false;}
  if(initialized){deinit();initialized=false;}
  if(context!=EGL_NO_CONTEXT){glDeleteFramebuffers(1,&fbo);glDeleteTextures(1,&texture);glDeleteRenderbuffers(1,&depth);glDeleteRenderbuffers(1,&stencil);eglMakeCurrent(display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);eglDestroyContext(display,context);context=EGL_NO_CONTEXT;}
  if(surface!=EGL_NO_SURFACE)eglDestroySurface(display,surface);if(display!=EGL_NO_DISPLAY)eglTerminate(display);
  if(library){dlclose(library);library=nullptr;}if(active==this)active=nullptr;
 }
 ~Host(){cleanup();}
};
bool Host::environment(unsigned cmd,void* data){
 Host* h=active;if(!h)return false;
 switch(cmd){
 case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:*static_cast<const char**>(data)=h->system.c_str();return true;
 case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:*static_cast<const char**>(data)=h->save.c_str();return true;
 case RETRO_ENVIRONMENT_GET_CONTENT_DIRECTORY:*static_cast<const char**>(data)=h->system.c_str();return true;
 case RETRO_ENVIRONMENT_GET_CAN_DUPE:*static_cast<bool*>(data)=true;return true;
 case RETRO_ENVIRONMENT_GET_INPUT_DEVICE_CAPABILITIES:*static_cast<uint64_t*>(data)=(1ULL<<RETRO_DEVICE_JOYPAD)|(1ULL<<RETRO_DEVICE_ANALOG);return true;
 case RETRO_ENVIRONMENT_GET_INPUT_MAX_USERS:*static_cast<unsigned*>(data)=4;return true;
 case RETRO_ENVIRONMENT_SET_ROTATION:h->rotation=*static_cast<unsigned*>(data)&3;return true;
 case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:{auto f=*static_cast<retro_pixel_format*>(data);if(f!=RETRO_PIXEL_FORMAT_0RGB1555&&f!=RETRO_PIXEL_FORMAT_RGB565&&f!=RETRO_PIXEL_FORMAT_XRGB8888)return false;h->format=f;return true;}
 case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:static_cast<retro_log_callback*>(data)->log=log;return true;
 case RETRO_ENVIRONMENT_GET_VARIABLE:{auto* v=static_cast<retro_variable*>(data);auto found=h->variables.find(v->key);v->value=found==h->variables.end()?nullptr:found->second.c_str();return v->value!=nullptr;}
 case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:*static_cast<bool*>(data)=false;return true;
 case RETRO_ENVIRONMENT_SET_VARIABLES:{auto* v=static_cast<retro_variable*>(data);for(;v&&v->key;++v){if(h->variables.count(v->key)||!v->value)continue;std::string s=v->value;auto start=s.find(';');if(start==std::string::npos)continue;start=s.find_first_not_of(' ',start+1);if(start!=std::string::npos)h->variables[v->key]=s.substr(start,s.find('|',start)-start);}return true;}
 case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:*static_cast<unsigned*>(data)=2;return true;
 case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2:{auto* defs=static_cast<retro_core_options_v2*>(data);for(auto* d=defs->definitions;d&&d->key;++d)if(!h->variables.count(d->key))h->variables[d->key]=d->default_value?d->default_value:(d->values[0].value?d->values[0].value:"");return true;}
 case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2_INTL:{auto* defs=static_cast<retro_core_options_v2_intl*>(data);return environment(RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2,defs->us);}
 case RETRO_ENVIRONMENT_SET_CORE_OPTIONS:{auto* d=static_cast<retro_core_option_definition*>(data);for(;d&&d->key;++d)if(!h->variables.count(d->key))h->variables[d->key]=d->default_value?d->default_value:(d->values[0].value?d->values[0].value:"");return true;}
 case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_INTL:{auto* defs=static_cast<retro_core_options_intl*>(data);return environment(RETRO_ENVIRONMENT_SET_CORE_OPTIONS,defs->us);}
 case RETRO_ENVIRONMENT_SET_HW_RENDER:{auto* cb=static_cast<retro_hw_render_callback*>(data);if(cb->context_type!=RETRO_HW_CONTEXT_OPENGLES2&&cb->context_type!=RETRO_HW_CONTEXT_OPENGLES3)return false;h->hw=*cb;cb->get_current_framebuffer=framebuffer;cb->get_proc_address=proc;h->hardware=true;try{h->initGL();return true;}catch(const std::exception& e){h->logMessage=e.what();return false;}}
 case RETRO_ENVIRONMENT_GET_PREFERRED_HW_RENDER:*static_cast<unsigned*>(data)=RETRO_HW_CONTEXT_OPENGLES2;return true;
 case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO:h->av=*static_cast<retro_system_av_info*>(data);return true;
 case RETRO_ENVIRONMENT_SET_GEOMETRY:h->av.geometry=*static_cast<retro_game_geometry*>(data);return true;
 case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:return true;
 case RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE:*static_cast<int*>(data)=3;return true;
 case RETRO_ENVIRONMENT_GET_FASTFORWARDING:*static_cast<bool*>(data)=false;return true;
 case RETRO_ENVIRONMENT_GET_LANGUAGE:*static_cast<unsigned*>(data)=RETRO_LANGUAGE_SPANISH;return true;
 case RETRO_ENVIRONMENT_GET_OVERSCAN:*static_cast<bool*>(data)=false;return true;
 case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY:case RETRO_ENVIRONMENT_SET_MEMORY_MAPS:case RETRO_ENVIRONMENT_SET_SERIALIZATION_QUIRKS:case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_UPDATE_DISPLAY_CALLBACK:return true;
 case RETRO_ENVIRONMENT_SET_MESSAGE:__android_log_print(ANDROID_LOG_INFO,"TirsoCore","%s",static_cast<retro_message*>(data)->msg);return true;
 case RETRO_ENVIRONMENT_SHUTDOWN:h->shutdown=true;return true;
 default:return false;
 }
}
void Host::video(const void* data,unsigned w,unsigned height,size_t pitch){
 Host* h=active;if(!h||!data||!w||!height||w>MAX_WIDTH||height>MAX_HEIGHT)return;
 h->width=w;h->height=height;
 if(data==RETRO_HW_FRAME_BUFFER_VALID){
  glBindFramebuffer(GL_FRAMEBUFFER,h->fbo);std::vector<uint32_t> rgba(w*height);glReadPixels(0,0,w,height,GL_RGBA,GL_UNSIGNED_BYTE,rgba.data());
  for(unsigned y=0;y<height;++y)for(unsigned x=0;x<w;++x){uint32_t p=rgba[(h->hw.bottom_left_origin?height-1-y:y)*w+x];h->pixels[y*w+x]=0xFF000000|((p&255)<<16)|(p&0xFF00)|((p>>16)&255);}
 }else for(unsigned y=0;y<height;++y){auto* row=static_cast<const uint8_t*>(data)+y*pitch;
  for(unsigned x=0;x<w;++x){uint32_t p;
   if(h->format==RETRO_PIXEL_FORMAT_XRGB8888)p=reinterpret_cast<const uint32_t*>(row)[x]&0xFFFFFF;
   else{unsigned v=reinterpret_cast<const uint16_t*>(row)[x],r,g,b;if(h->format==RETRO_PIXEL_FORMAT_RGB565){r=(v>>11)&31;g=(v>>5)&63;b=v&31;g=(g<<2)|(g>>4);}else{r=(v>>10)&31;g=(v>>5)&31;b=v&31;g=(g<<3)|(g>>2);}r=(r<<3)|(r>>2);b=(b<<3)|(b>>2);p=(r<<16)|(g<<8)|b;}
   h->pixels[y*w+x]=0xFF000000|p;
  }
 }
 if(h->rotation){
  std::vector<uint32_t> old(h->pixels.begin(),h->pixels.begin()+w*height);unsigned nw=(h->rotation&1)?height:w,nh=(h->rotation&1)?w:height;
  for(unsigned y=0;y<height;++y)for(unsigned x=0;x<w;++x){unsigned nx,ny;if(h->rotation==1){nx=y;ny=w-1-x;}else if(h->rotation==2){nx=w-1-x;ny=height-1-y;}else{nx=height-1-y;ny=x;}h->pixels[ny*nw+nx]=old[y*w+x];}h->width=nw;h->height=nh;
 }
}
size_t Host::batch(const int16_t* data,size_t frames){
 Host* h=active;if(!h||!data)return frames;std::lock_guard<std::mutex> lock(h->audioMutex);
 double rate=h->av.timing.sample_rate>1000?h->av.timing.sample_rate:48000;
 for(size_t i=0;i<frames;++i){h->audioPhase+=48000.0/rate;while(h->audioPhase>=1){h->audioPhase-=1;if(h->audio.size()<16384){h->audio.push_back(data[i*2]);h->audio.push_back(data[i*2+1]);}}}return frames;
}
static void fail(JNIEnv* e,const char* message){e->ThrowNew(e->FindClass("java/io/IOException"),message);}
static std::string str(JNIEnv* e,jstring s){const char* p=e->GetStringUTFChars(s,nullptr);std::string r(p);e->ReleaseStringUTFChars(s,p);return r;}
}
extern "C" JNIEXPORT jlong JNICALL Java_com_tirso_tirsolador_RetroCore_open(JNIEnv* e,jclass,jstring lib,jstring rom,jstring save,jstring system,jstring id){try{return reinterpret_cast<jlong>(new Host(str(e,lib),str(e,rom),str(e,save),str(e,system),str(e,id)));}catch(const std::exception& x){fail(e,x.what());return 0;}}
extern "C" JNIEXPORT jint JNICALL Java_com_tirso_tirsolador_RetroCore_frame(JNIEnv* e,jclass,jlong handle,jintArray keys,jfloatArray axes,jintArray pixels,jshortArray sound){
 auto* h=reinterpret_cast<Host*>(handle);if(!h||e->GetArrayLength(pixels)<MAX_WIDTH*MAX_HEIGHT||e->GetArrayLength(sound)<16384||e->GetArrayLength(axes)<16||e->GetArrayLength(keys)<4){fail(e,"Buffer de emulación inválido");return -1;}
 try{
  int masks[4];e->GetIntArrayRegion(keys,0,4,masks);for(int i=0;i<4;++i)h->keys[i]=masks[i];
  float values[16];e->GetFloatArrayRegion(axes,0,16,values);for(int i=0;i<16;++i)h->axes[i/4][i%4]=static_cast<int16_t>(std::max(-1.f,std::min(1.f,values[i]))*32767);
  h->run();if(h->shutdown)throw std::runtime_error("El núcleo terminó la ejecución del juego");if(++h->frames%120==0&&!h->flush())throw std::runtime_error("No se pudo guardar la partida en la TV");
  if(h->width&&h->height)e->SetIntArrayRegion(pixels,0,h->width*h->height,reinterpret_cast<jint*>(h->pixels.data()));
  std::lock_guard<std::mutex> lock(h->audioMutex);int count=h->audio.size();if(count)e->SetShortArrayRegion(sound,0,count,h->audio.data());h->audio.clear();return count;
 }catch(const std::exception& x){fail(e,x.what());return -1;}
}
extern "C" JNIEXPORT jdoubleArray JNICALL Java_com_tirso_tirsolador_RetroCore_info(JNIEnv* e,jclass,jlong handle){auto* h=reinterpret_cast<Host*>(handle);double ratio=h->av.geometry.aspect_ratio;if((h->rotation&1)&&ratio>0)ratio=1/ratio;double data[]={static_cast<double>(h->width?h->width:h->av.geometry.base_width),static_cast<double>(h->height?h->height:h->av.geometry.base_height),ratio,h->av.timing.fps};auto a=e->NewDoubleArray(4);e->SetDoubleArrayRegion(a,0,4,data);return a;}
extern "C" JNIEXPORT jbyteArray JNICALL Java_com_tirso_tirsolador_RetroCore_snapshot(JNIEnv* e,jclass,jlong handle){try{auto data=reinterpret_cast<Host*>(handle)->snapshot();auto out=e->NewByteArray(data.size());e->SetByteArrayRegion(out,0,data.size(),reinterpret_cast<jbyte*>(data.data()));return out;}catch(const std::exception& x){fail(e,x.what());return nullptr;}}
extern "C" JNIEXPORT jboolean JNICALL Java_com_tirso_tirsolador_RetroCore_restore(JNIEnv* e,jclass,jlong handle,jbyteArray data){int n=e->GetArrayLength(data);if(n<1||n>64*1024*1024)return false;std::vector<unsigned char>b(n);e->GetByteArrayRegion(data,0,n,reinterpret_cast<jbyte*>(b.data()));try{return reinterpret_cast<Host*>(handle)->restore(b);}catch(const std::exception& x){fail(e,x.what());return false;}}
extern "C" JNIEXPORT jboolean JNICALL Java_com_tirso_tirsolador_RetroCore_flush(JNIEnv*,jclass,jlong handle){return reinterpret_cast<Host*>(handle)->flush();}
extern "C" JNIEXPORT void JNICALL Java_com_tirso_tirsolador_RetroCore_close(JNIEnv*,jclass,jlong handle){delete reinterpret_cast<Host*>(handle);}
extern "C" JNIEXPORT void JNICALL Java_com_tirso_tirsolador_RetroCore_diagnostics(JNIEnv*,jclass,jboolean enabled){diagnosticLogging=enabled;}

#ifdef TIRSO_DIAGNOSTICS
extern "C" JNIEXPORT jint JNICALL Java_com_tirso_tirsolador_RetroCore_systemWordForTest(JNIEnv* e,jclass,jlong handle,jint offset){
 auto* h=reinterpret_cast<Host*>(handle);auto size=h->memorySize(RETRO_MEMORY_SYSTEM_RAM);auto* data=static_cast<unsigned char*>(h->memoryData(RETRO_MEMORY_SYSTEM_RAM));
 if(!data||offset<0||(offset&3)||static_cast<size_t>(offset)+4>size){fail(e,"Invalid RAM probe");return 0;}uint32_t value;memcpy(&value,data+offset,4);return value;
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_com_tirso_tirsolador_RetroCore_saveMemoryForTest(JNIEnv* e,jclass,jlong handle,jint value){
 auto* h=reinterpret_cast<Host*>(handle);auto size=h->memorySize(RETRO_MEMORY_SAVE_RAM);auto* data=static_cast<unsigned char*>(h->memoryData(RETRO_MEMORY_SAVE_RAM));
 if(!data||size<1||size>32*1024*1024){fail(e,"No save RAM");return nullptr;}
 if(value>=0)data[0]=static_cast<unsigned char>(value);
 auto out=e->NewByteArray(size);e->SetByteArrayRegion(out,0,size,reinterpret_cast<jbyte*>(data));return out;
}
#endif
