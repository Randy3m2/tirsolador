// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include <memory>
#include <string>
#include <vector>
#include "NDS.h"
struct Engine {
  std::unique_ptr<melonDS::NDS> nds;
  std::string savePath;
  std::vector<unsigned char> dirtySave;
  bool stopped=false;
  Engine(const std::string& rom,const std::string& save);
  ~Engine();
  bool frame(int mask,int x,int y,bool touch,unsigned* pixels,short* audio,int& count);
  std::vector<unsigned char> state();
  bool load(const std::vector<unsigned char>& bytes);
  bool flush();
};
