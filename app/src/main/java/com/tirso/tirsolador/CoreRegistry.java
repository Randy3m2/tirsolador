package com.tirso.tirsolador;
import android.content.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
final class CoreRegistry {
    static String key(Console c){switch(c){case GBC:case GB:return "gb";case NDS:return "nds";default:return c.name().toLowerCase(java.util.Locale.ROOT);}}
    static JSONObject config(Context c)throws Exception{try(java.io.InputStream in=c.getAssets().open("cores.json")){return new JSONObject(new String(GameStorage.read(in,32768),StandardCharsets.UTF_8)).getJSONObject("cores");}}
    static String revision(Context context,Console console)throws Exception{return console==Console.NDS?DsCore.REVISION:config(context).getJSONObject(key(console)).getString("rev");}
    static String name(Context context,Console console)throws Exception{return console==Console.NDS?"melonDS":config(context).getJSONObject(key(console)).getString("name");}
    static File library(Context context,Console console){return new File(context.getApplicationInfo().nativeLibraryDir,"libtirso_"+key(console)+".so");}
}
