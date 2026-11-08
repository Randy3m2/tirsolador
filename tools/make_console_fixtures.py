#!/usr/bin/env python3
"""Own, copyright-free framebuffer/input diagnostics. No commercial ROMs in the app.
SPDX-License-Identifier: CC0-1.0
"""
from pathlib import Path
import sys,struct,zipfile,hashlib,urllib.request
root=Path(__file__).resolve().parents[1];out=Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
class Code:
    def __init__(self,base=0):self.b=bytearray();self.labels={};self.fix=[];self.base=base
    def emit(self,*b):self.b.extend(b)
    def label(self,n):self.labels[n]=len(self.b)
    def rel(self,op,label):self.emit(op,0);self.fix.append((len(self.b)-1,label,False))
    def absolute(self,op,label):self.emit(op,0,0);self.fix.append((len(self.b)-2,label,True))
    def finish(self):
        for p,n,absolute in self.fix:
            target=self.labels[n]
            if absolute:struct.pack_into('<H',self.b,p,target+self.base)
            else:
                delta=target-p-1;assert -128<=delta<=127;self.b[p]=delta&255
        return self.b
# NES / mapper zero, universal background colour changes while A is held.
c=Code(0x8000);c.emit(0x78,0xD8,0xA2,0xFF,0x9A,0xA9,0,0x8D,0,0x20,0x8D,1,0x20)
c.label('loop');c.emit(0x2C,2,0x20);c.rel(0x10,'loop')
c.emit(0xA9,1,0x8D,0x16,0x40,0xA9,0,0x8D,0x16,0x40,0xAD,0x16,0x40,0x29,1)
c.rel(0xF0,'red');c.emit(0xA2,0x1A);c.absolute(0x4C,'write');c.label('red');c.emit(0xA2,0x16)
c.label('write');c.emit(0xA9,0x3F,0x8D,6,0x20,0xA9,0,0x8D,6,0x20,0x8A,0x8D,7,0x20,0xA9,8,0x8D,1,0x20);c.absolute(0x4C,'loop')
prg=bytearray(16384);b=c.finish();prg[:len(b)]=b;struct.pack_into('<3H',prg,len(prg)-6,0x8000,0x8000,0x8000)
(out/'diagnostic.nes').write_bytes(b'NES\x1a'+bytes([1,1])+bytes(10)+prg+bytes(8192))
# SNES / LoROM / native 65816 / backdrop colour and auto-joypad input.
c=Code(0x8000);c.emit(0x78,0xD8,0x18,0xFB,0xC2,0x10,0xA2,0xFF,0x1F,0x9A,0xA9,0x80,0x8D,0,0x21,0xA9,0x0F,0x8D,0,0x21,0xA9,1,0x8D,0,0x42)
c.label('loop');c.emit(0xAD,0x12,0x42,0x29,0x80);c.rel(0xF0,'loop');c.emit(0xAD,0x18,0x42,0x29,0x80);c.rel(0xF0,'red')
c.emit(0xA9,0,0x8D,0x21,0x21,0xA9,0xE0,0x8D,0x22,0x21,0xA9,3,0x8D,0x22,0x21);c.absolute(0x4C,'loop')
c.label('red');c.emit(0xA9,0,0x8D,0x21,0x21,0xA9,0x1F,0x8D,0x22,0x21,0xA9,0,0x8D,0x22,0x21);c.absolute(0x4C,'loop')
rom=bytearray(32768);b=c.finish();rom[:len(b)]=b;rom[0x7FC0:0x7FD5]=b'TIRSOLADOR TEST'.ljust(21,b' ');rom[0x7FD5:0x7FDC]=bytes([0x20,0,5,0,1,0,0])
for p in range(0x7FE0,0x8000,2):struct.pack_into('<H',rom,p,0x8000)
struct.pack_into('<HH',rom,0x7FDC,0xFFFF,0);checksum=sum(rom)&0xFFFF;struct.pack_into('<HH',rom,0x7FDC,checksum^0xFFFF,checksum)
(out/'diagnostic.sfc').write_bytes(rom)
# GB and GBC: own LR35902 code. Built-in core bootstrap; no vendor logo/BIOS.
for colour,ext in [(False,'gb'),(True,'gbc')]:
    c=Code(0x150);c.emit(0xF3,0xAF,0xE0,0x40,0x21,0,0x80,0x01,0,0x20)
    c.label('zero');c.emit(0xAF,0x22,0x0B,0x78,0xB1);c.rel(0x20,'zero')
    c.emit(0xAF,0xE0,0x42,0xE0,0x43,0x3E,0xE4,0xE0,0x47,0x3E,0x91,0xE0,0x40)
    c.label('loop');c.emit(0x3E,0x10,0xE0,0,0xF0,0,0xE6,1);c.rel(0x28,'pressed')
    if colour:
        c.emit(0x06,0x1F,0x0E,0);c.rel(0x18,'apply');c.label('pressed');c.emit(0x06,0xE0,0x0E,3)
        c.label('apply');c.emit(0x3E,0x80,0xE0,0x68,0x78,0xE0,0x69,0x79,0xE0,0x69)
    else:
        c.emit(0x3E,0xE4);c.rel(0x18,'apply');c.label('pressed');c.emit(0x3E,0x1B);c.label('apply');c.emit(0xE0,0x47)
    c.absolute(0xC3,'loop');rom=bytearray(32768);rom[0x100:0x104]=bytes([0xC3,0x50,1,0]);rom[0x134:0x143]=b'TIRSOLADOR TEST'.ljust(15,b' ');rom[0x143]=0x80 if colour else 0
    b=c.finish();rom[0x150:0x150+len(b)]=b;rom[0x14D]=(-sum(rom[0x134:0x14D])-25)&255;struct.pack_into('>H',rom,0x14E,sum(rom)&0xFFFF)
    (out/('diagnostic.'+ext)).write_bytes(rom)
# GBA: ARM7TDMI, mode 3 native framebuffer, A toggles red/green.
words=[];literals=[]
def op(w):words.append(w)
def ldr(reg,value,condition=14):literals.append((len(words),reg,value,condition));op(0)
ldr(0,0x04000000);ldr(1,0x403);op(0xE5801000);loop=len(words)
ldr(0,0x04000130);op(0xE1D010B0);op(0xE3110001);ldr(2,0x001F);ldr(2,0x03E0,0);ldr(0,0x06000000);ldr(1,240*160)
fill=len(words);op(0xE0C020B2);op(0xE2511001);op(0x1A000000|((fill-len(words)-2)&0xFFFFFF));op(0xEA000000|((loop-len(words)-2)&0xFFFFFF))
for i,reg,value,cond in literals:words[i]=(cond<<28)|0x059F0000|(reg<<12)|(len(words)*4-i*4-8);op(value)
rom=bytearray(0xC0);struct.pack_into('<I',rom,0,0xEA00002E);rom[0xA0:0xAC]=b'TIRSOLADOR  ';rom[0xAC:0xB0]=b'TIRS';rom[0xB2]=0x96;rom[0xBD]=(-sum(rom[0xA0:0xBD])-0x19)&255
rom+=struct.pack('<%dI'%len(words),*words);(out/'diagnostic.gba').write_bytes(rom.ljust(32768,b'\0'))
# Mega Drive: own 68000 reset, TMSS unlock, VDP backdrop, pad A toggles green.
c=Code(0x200)
def w(v):c.emit((v>>8)&255,v&255)
def lng(v):w(v>>16);w(v&65535)
def move_word(v,address):w(0x33FC);w(v);lng(address)
def move_long(v,address):w(0x23FC);lng(v);lng(address)
def move_byte(v,address):w(0x13FC);w(v);lng(address)
move_long(0x53454741,0xA14000)
for v in [0x8004,0x8104,0x8700,0x8C81,0x8F02]:move_word(v,0xC00004)
move_long(0xC0000000,0xC00004);move_word(0x000E,0xC00000);move_word(0x8144,0xC00004);move_byte(0x40,0xA10009)
c.label('loop');move_byte(0,0xA10003);w(0x1039);lng(0xA10003);w(0x0200);w(0x10);c.rel(0x67,'pressed')
move_long(0xC0000000,0xC00004);move_word(0x000E,0xC00000);c.rel(0x60,'loop')
c.label('pressed');move_long(0xC0000000,0xC00004);move_word(0x00E0,0xC00000);c.rel(0x60,'loop')
rom=bytearray(65536);struct.pack_into('>2I',rom,0,0xFFFFF0,0x200);rom[0x100:0x110]=b'SEGA MEGA DRIVE ';rom[0x120:0x150]=b'TIRSOLADOR TEST'.ljust(48,b' ')
struct.pack_into('>4I',rom,0x1A0,0,len(rom)-1,0xFF0000,0xFFFFFF);rom[0x1F0:0x200]=b'JUE'.ljust(16,b' ');b=c.finish();rom[0x200:0x200+len(b)]=b
struct.pack_into('>H',rom,0x18E,sum(struct.unpack('>%dH'%((len(rom)-512)//2),rom[512:]))&65535);(out/'diagnostic.md').write_bytes(rom)
# MIT homebrew N64 diagnostic from the author's pinned CI build, including open IPL3.
with zipfile.ZipFile(root/'tests/fixtures/n64-systemtest.zip') as archive:
    name=next(n for n in archive.namelist() if n.endswith('.z64'));data=archive.read(name)
    assert hashlib.sha256(data).hexdigest()=='6ca63a67baec3195b99160f3c3260b68bcc41d1a0a7bb5626c49c28a4eb09cc0';(out/'n64-systemtest.z64').write_bytes(data)
# Gridlee is downloaded only from its approved official distribution site, never mirrored in the repo/release.
url='https://www.mamedev.org/roms/gridlee/gridlee.zip'
with urllib.request.urlopen(url,timeout=60) as response:data=response.read(1024*1024)
assert hashlib.sha256(data).hexdigest()=='df977ceba0ae1c8d0ecf489ae8423390ff5c7c76ce95f5ee6ba9bc892b18056e';(out/'gridlee.zip').write_bytes(data)
print('Prepared console execution diagnostics (test APK only).')
