#!/usr/bin/env python3
"""Own ARM9 framebuffer/input diagnostic, no Nintendo code/logo/ROM assets.
SPDX-License-Identifier: CC0-1.0
ARM source equivalent: initialize VRAM A; show red; A held -> green; loop.
"""
import pathlib, struct, sys
words=[]; literals=[]
def op(word): words.append(word)
def ldr(reg,value,cond=0xE):
    literals.append((len(words),reg,value,cond)); op(0)
ldr(0,0x04000000); ldr(1,0x00020000); op(0xE5801000)
ldr(0,0x04000240); op(0xE3A01080); op(0xE5C01000)
loop=len(words)
ldr(0,0x04000130); op(0xE1D010B0); op(0xE3110001)
ldr(2,0x801F); ldr(2,0x83E0,0)
ldr(0,0x06800000); ldr(1,49152)
fill=len(words); op(0xE0C020B2); op(0xE2511001)
op(0x1A000000|((fill-len(words)-2)&0xFFFFFF))
op(0xEA000000|((loop-len(words)-2)&0xFFFFFF))
for index,reg,value,cond in literals:
    offset=len(words)*4-(index*4+8)
    words[index]=(cond<<28)|0x059F0000|(reg<<12)|offset
    op(value)
arm9=struct.pack('<%dI'%len(words),*words)
arm7=struct.pack('<I',0xEAFFFFFE)
header=bytearray(512)
header[:12]=b'TIRSOTEST   '; header[12:16]=b'####';header[16:18]=b'00'
struct.pack_into('<4I',header,0x20,0x200,0x02000000,0x02000000,len(arm9))
struct.pack_into('<4I',header,0x30,0x400,0x03800000,0x03800000,len(arm7))
struct.pack_into('<2I',header,0x80,0x600,0x200)
def crc16(b):
    crc=0xFFFF
    for v in b:
        crc^=v
        for _ in range(8):crc=(crc>>1)^(0xA001 if crc&1 else 0)
    return crc
struct.pack_into('<H',header,0x15C,crc16(header[0xC0:0x15C]))
struct.pack_into('<H',header,0x15E,crc16(header[:0x15E]))
out=pathlib.Path(sys.argv[1]);out.parent.mkdir(parents=True,exist_ok=True)
out.write_bytes((header+arm9+bytes(512-len(arm9))+arm7+bytes(512-len(arm7))).ljust(0x20000,b'\x00'))
