package com.tirso.tirsolador;

import java.util.Locale;
import org.drinkless.tdlib.TdApi;

/** Exact source routing and conservative document parsing; no substring console guesses. */
final class TelegramCatalog {
    static String expectedTitle(Console c){
        switch(c){
            case NES:return "TD - ROMs - NES";
            case SNES:return "TD - ROMs - SNES";
            case MD:return "TD - ROMs - Genesis";
            case GB:return "TD - ROMs - Game Boy";
            case N64:return "TD - ROMs - N64";
            case NDS:return "TD - ROMs - DS";
            default:return "";
        }
    }
    static Console consoleFor(String title){
        if(title==null)return null;
        for(Console c:Console.values())if(!expectedTitle(c).isEmpty()&&expectedTitle(c).equalsIgnoreCase(title.trim()))return c;
        return null;
    }
    static boolean selectable(String title){return title!=null&&title.startsWith("TD - ROMs - ")&&!title.equalsIgnoreCase("TD - ROMs - GameCube");}
    static boolean supported(Console c,String file){
        if(file==null)return false;String lower=file.toLowerCase(Locale.ROOT);int dot=lower.lastIndexOf('.');
        if(dot<0)return false;String extension=lower.substring(dot+1);
        if(extension.equals("zip")||extension.equals("7z"))return true;
        String allowed;
        switch(c){
            case NES:allowed="nes,fds,unf,unif";break;
            case SNES:allowed="sfc,smc,swc,fig";break;
            case MD:allowed="md,gen,smd,bin";break;
            case GB:allowed="gb";break;
            case GBC:allowed="gbc,gb";break;
            case GBA:allowed="gba";break;
            case N64:allowed="n64,v64,z64";break;
            case NDS:allowed="nds";break;
            default:allowed="";
        }
        for(String e:allowed.split(","))if(!e.isEmpty()&&e.equals(extension))return true;
        return false;
    }
    static Game game(long account,Console console,TdApi.Message message){
        if(!(message.content instanceof TdApi.MessageDocument))return null;
        TdApi.Document document=((TdApi.MessageDocument)message.content).document;
        if(document==null||document.document==null||!supported(console,document.fileName))return null;
        String filename=document.fileName;
        String stem=filename.substring(0,filename.lastIndexOf('.')).trim();if(stem.isEmpty())return null;
        return new Game("tg:"+account+":"+message.chatId+":"+message.id,stem,console,
            "Biblioteca de "+console.title+" · "+filename,stem,account,message.chatId,message.id,
            document.document.id,filename,Math.max(document.document.size,document.document.expectedSize));
    }
}
