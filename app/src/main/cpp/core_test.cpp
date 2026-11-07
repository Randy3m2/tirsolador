// SPDX-License-Identifier: GPL-3.0-or-later
#include "engine.h"
#include <iostream>
#include <stdexcept>
int main(int argc,char** argv){
 if(argc!=2)return 2;
 try{
 Engine e(argv[1],std::string(argv[1])+".sav");
 std::vector<unsigned> p(256*384);short audio[4096];int n;
 for(int i=0;i<5;++i)if(!e.frame(0,0,0,false,p.data(),audio,n))throw std::runtime_error("stopped");
 auto red=p[128*256+128];auto saved=e.state();
 for(int i=0;i<5;++i)e.frame(1,0,0,false,p.data(),audio,n);
 auto green=p[128*256+128];std::cerr<<std::hex<<"red="<<red<<" green="<<green<<" other="<<p[256*192+128*256+128]<<std::dec<<"\n";
 if(((red>>16)&255)<240||(red&65535)!=0||((green>>8)&255)<240||(green&0xFF00FF)!=0)throw std::runtime_error("framebuffer/input mismatch");
 if(!e.load(saved))throw std::runtime_error("state restore failed");
 for(int i=0;i<3;++i)e.frame(0,0,0,false,p.data(),audio,n);
 if(p[128*256+128]!=red)throw std::runtime_error("state output mismatch");
 std::cout<<"PASS: direct boot, red frame, A input green, state roundtrip; audio samples="<<n<<" state="<<saved.size()<<"\n";
 }catch(const std::exception& x){std::cerr<<x.what()<<"\n";return 1;}
}
