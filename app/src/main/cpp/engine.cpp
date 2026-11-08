// SPDX-License-Identifier: GPL-3.0-or-later
#include "engine.h"
#include "Args.h"
#include "Savestate.h"
#include "Platform.h"
#include <cstdio>
#include <cstdarg>
#include <cstring>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <chrono>
#include <stdexcept>
#include <fstream>
#include <unistd.h>
#include <dlfcn.h>
#ifdef __ANDROID__
#include <android/log.h>
#endif
using namespace melonDS;
static std::vector<u8> readBytes(const std::string& path,size_t max) {
 std::ifstream f(path,std::ios::binary|std::ios::ate);
 if(!f)return {}; auto n=f.tellg(); if(n<=0||n>static_cast<std::streamoff>(max))return {};
 std::vector<u8> b(static_cast<size_t>(n)); f.seekg(0); if(!f.read(reinterpret_cast<char*>(b.data()),n))return {}; return b;
}
static bool atomicWrite(const std::string& path,const u8* p,size_t n) {
 std::string temp=path+".tmp"; FILE* f=fopen(temp.c_str(),"wb"); if(!f)return false;
 bool ok=fwrite(p,1,n,f)==n && fflush(f)==0 && fsync(fileno(f))==0;
 if(fclose(f)!=0)ok=false; if(ok)ok=rename(temp.c_str(),path.c_str())==0;
 if(!ok)unlink(temp.c_str()); return ok;
}
Engine::Engine(const std::string& rom,const std::string& save):savePath(save) {
 std::ifstream file(rom,std::ios::binary|std::ios::ate);
 if(!file)throw std::runtime_error("No se pudo abrir la ROM");
 auto size=file.tellg();if(size<4096||size>512LL*1024*1024)throw std::runtime_error("ROM DS inválida o demasiado grande");
 auto bytes=std::make_unique<u8[]>(static_cast<size_t>(size));file.seekg(0);
 if(!file.read(reinterpret_cast<char*>(bytes.get()),size))throw std::runtime_error("ROM incompleta");
 if(bytes[0x12]==3)throw std::runtime_error("Esta versión admite Nintendo DS; los juegos exclusivos de DSi aún no están habilitados");
 auto cart=NDSCart::ParseROM(std::move(bytes),static_cast<u32>(size),this); if(!cart)throw std::runtime_error("melonDS no pudo abrir la ROM");
 NDSArgs args; args.JIT=std::nullopt; args.OutputSampleRate=48000;
 nds=std::make_unique<NDS>(std::move(args),this); nds->SetNDSCart(std::move(cart));
 nds->Reset(); auto battery=readBytes(save,32*1024*1024);
 if(!battery.empty())nds->SetNDSSave(battery.data(),battery.size());
 nds->SetupDirectBoot(rom); nds->Start();
}
Engine::~Engine(){flush();if(nds)nds->Stop(Platform::External);}
bool Engine::frame(int mask,int x,int y,bool touch,unsigned* pixels,short* audio,int& count) {
 if(stopped)return false;
 nds->SetKeyMask((~mask)&0xFFF); if(touch)nds->TouchScreen(x,y);else nds->ReleaseScreen();
 nds->RunFrame(); void *a=nullptr,*b=nullptr;
 if(nds->GPU.GetFramebuffers(&a,&b)){memcpy(pixels,a,256*192*4);memcpy(pixels+256*192,b,256*192*4);}
 count=nds->SPU.ReadOutput(audio,2048); if(!dirtySave.empty()&&!flush())throw std::runtime_error("No se pudo guardar la partida");
 return !stopped;
}
bool Engine::flush(){if(dirtySave.empty())return true;bool ok=atomicWrite(savePath,dirtySave.data(),dirtySave.size());if(ok)dirtySave.clear();return ok;}
std::vector<u8> Engine::state(){Savestate s;if(!nds->DoSavestate(&s)||s.Error)throw std::runtime_error("No se pudo crear el estado");s.Finish();return {(u8*)s.Buffer(),(u8*)s.Buffer()+s.Length()};}
bool Engine::load(const std::vector<u8>& bytes){
 if(bytes.size()<16||bytes.size()>64*1024*1024)return false;
 auto backup=state(); Savestate s(const_cast<u8*>(bytes.data()),bytes.size(),false);
 bool ok=!s.Error&&nds->DoSavestate(&s)&&!s.Error;
 if(!ok){Savestate rollback(backup.data(),backup.size(),false);nds->DoSavestate(&rollback);}
 return ok;
}
namespace melonDS::Platform {
struct FileHandle{FILE* f;};
std::string GetLocalFilePath(const std::string&){return {};}
FileHandle* OpenFile(const std::string& p,FileMode m){
 bool exists=access(p.c_str(),F_OK)==0;if((m&NoCreate)&&!exists)return nullptr;
 const char* mode=(m&Write)?((m&Append)?"ab+":((m&Preserve)&&exists)?"rb+":"wb+"):"rb";
 FILE* f=fopen(p.c_str(),mode);return f?new FileHandle{f}:nullptr;
}
FileHandle* OpenLocalFile(const std::string&,FileMode){return nullptr;}
bool FileExists(const std::string& p){return access(p.c_str(),F_OK)==0;}
bool LocalFileExists(const std::string&){return false;}
bool CheckFileWritable(const std::string& p){auto f=OpenFile(p,FileMode(Write|Preserve));return f&&CloseFile(f);}
bool CheckLocalFileWritable(const std::string&){return false;}
bool CloseFile(FileHandle* f){bool ok=fclose(f->f)==0;delete f;return ok;}
bool IsEndOfFile(FileHandle* f){return feof(f->f)!=0;}
bool FileReadLine(char* s,int n,FileHandle* f){return fgets(s,n,f->f)!=nullptr;}
u64 FilePosition(FileHandle* f){return ftello(f->f);}
bool FileSeek(FileHandle* f,s64 n,FileSeekOrigin o){return fseeko(f->f,n,o==FileSeekOrigin::Start?SEEK_SET:o==FileSeekOrigin::End?SEEK_END:SEEK_CUR)==0;}
void FileRewind(FileHandle* f){rewind(f->f);}
u64 FileRead(void* p,u64 s,u64 n,FileHandle* f){return fread(p,s,n,f->f);}
u64 FileWrite(const void* p,u64 s,u64 n,FileHandle* f){return fwrite(p,s,n,f->f);}
bool FileFlush(FileHandle* f){return fflush(f->f)==0;}
u64 FileWriteFormatted(FileHandle* f,const char* fmt,...){va_list a;va_start(a,fmt);int n=vfprintf(f->f,fmt,a);va_end(a);return n<0?0:n;}
u64 FileLength(FileHandle* f){auto pos=ftello(f->f);fseeko(f->f,0,SEEK_END);auto n=ftello(f->f);fseeko(f->f,pos,SEEK_SET);return n;}
void Log(LogLevel l,const char* fmt,...){
 if(l<Warn)return;va_list a;va_start(a,fmt);
#ifdef __ANDROID__
 __android_log_vprint(ANDROID_LOG_WARN,"TirsoDS",fmt,a);
#else
 vfprintf(stderr,fmt,a);
#endif
 va_end(a);
}
struct Thread{std::thread t;explicit Thread(std::function<void()> f):t(std::move(f)){};};
Thread* Thread_Create(std::function<void()> f){return new Thread(std::move(f));}
void Thread_Wait(Thread* t){if(t->t.joinable())t->t.join();}
void Thread_Free(Thread* t){Thread_Wait(t);delete t;}
struct Semaphore{std::mutex m;std::condition_variable cv;unsigned n=0;};
Semaphore* Semaphore_Create(){return new Semaphore;}
void Semaphore_Free(Semaphore* s){delete s;}
void Semaphore_Reset(Semaphore* s){std::lock_guard<std::mutex> l(s->m);s->n=0;}
void Semaphore_Wait(Semaphore* s){std::unique_lock<std::mutex> l(s->m);s->cv.wait(l,[&]{return s->n>0;});--s->n;}
bool Semaphore_TryWait(Semaphore* s,int ms){std::unique_lock<std::mutex> l(s->m);if(!s->cv.wait_for(l,std::chrono::milliseconds(ms),[&]{return s->n>0;}))return false;--s->n;return true;}
void Semaphore_Post(Semaphore* s,int n){std::lock_guard<std::mutex> l(s->m);s->n+=n;s->cv.notify_all();}
struct Mutex{std::mutex m;};
Mutex* Mutex_Create(){return new Mutex;}
void Mutex_Free(Mutex* m){delete m;}
void Mutex_Lock(Mutex* m){m->m.lock();}
void Mutex_Unlock(Mutex* m){m->m.unlock();}
bool Mutex_TryLock(Mutex* m){return m->m.try_lock();}
void Sleep(u64 n){std::this_thread::sleep_for(std::chrono::microseconds(n));}
u64 GetUSCount(){return std::chrono::duration_cast<std::chrono::microseconds>(std::chrono::steady_clock::now().time_since_epoch()).count();}
u64 GetMSCount(){return GetUSCount()/1000;}
void SignalStop(StopReason,void* p){if(p)static_cast<Engine*>(p)->stopped=true;}
void WriteNDSSave(const u8* p,u32 n,u32,u32,void* e){if(e&&n<=32*1024*1024)static_cast<Engine*>(e)->dirtySave.assign(p,p+n);}
void WriteGBASave(const u8*,u32,u32,u32,void*){}
void WriteFirmware(const Firmware&,u32,u32,void*){}
void WriteDateTime(int,int,int,int,int,int,void*){}
void MP_Begin(void*){} void MP_End(void*){}
int MP_SendPacket(u8*,int,u64,void*){return 0;} int MP_RecvPacket(u8*,u64*,void*){return 0;}
int MP_SendCmd(u8*,int,u64,void*){return 0;} int MP_SendReply(u8*,int,u64,u16,void*){return 0;}
int MP_SendAck(u8*,int,u64,void*){return 0;} int MP_RecvHostPacket(u8*,u64*,void*){return 0;}
u16 MP_RecvReplies(u8*,u64,u16,void*){return 0;}
int Net_SendPacket(u8*,int,void*){return 0;} int Net_RecvPacket(u8*,void*){return 0;}
void Camera_Start(int,void*){} void Camera_Stop(int,void*){}
void Camera_CaptureFrame(int,u32* p,int w,int h,bool,void*){memset(p,0,w*h*4);}
void Mic_Start(void*){} void Mic_Stop(void*){} int Mic_ReadInput(s16* p,int n,void*){memset(p,0,n*2);return n;}
AACDecoder* AAC_Init(){return nullptr;} void AAC_DeInit(AACDecoder*){}
bool AAC_Configure(AACDecoder*,int,int){return false;} bool AAC_DecodeFrame(AACDecoder*,const void*,int,void*,int){return false;}
bool Addon_KeyDown(KeyType,void*){return false;} void Addon_RumbleStart(u32,void*){} void Addon_RumbleStop(void*){}
float Addon_MotionQuery(MotionQueryType,void*){return 0;}
DynamicLibrary* DynamicLibrary_Load(const char* p){return reinterpret_cast<DynamicLibrary*>(dlopen(p,RTLD_NOW));}
void DynamicLibrary_Unload(DynamicLibrary* p){dlclose(p);}
void* DynamicLibrary_LoadFunction(DynamicLibrary* p,const char* n){return dlsym(p,n);}
}
