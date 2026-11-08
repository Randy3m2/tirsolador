package com.tirso.tirsolador;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import org.json.JSONObject;

/** Private game data keyed by ROM digest. Atomic states and verified portable backups. */
final class GameStorage {
    static final int MAX_STATE=64*1024*1024,MAX_SAVE=32*1024*1024;
    final File rom,dir;
    final String digest;
    GameStorage(Context context,File input) throws Exception {
        this(context,input,input.getName());
    }
    GameStorage(Context context,File input,String name) throws Exception {
        File cache=new File(context.getCacheDir(),"roms");if(!cache.isDirectory()&&!cache.mkdirs())throw new IOException("Sin espacio para preparar la ROM");
        if(name.toLowerCase(Locale.ROOT).endsWith(".zip")){
            File temp=File.createTempFile("extract-",".nds",cache);boolean ok=false;
            try(ZipFile zip=new ZipFile(input)){
                ZipEntry chosen=null;Enumeration<? extends ZipEntry> entries=zip.entries();int count=0;
                while(entries.hasMoreElements()){
                    ZipEntry e=entries.nextElement();if(++count>10000)throw new IOException("ZIP con demasiados archivos");
                    if(!e.isDirectory()&&e.getName().toLowerCase(Locale.ROOT).endsWith(".nds")){
                        if(chosen!=null)throw new IOException("El ZIP contiene varias ROMs DS; usa un archivo por juego");chosen=e;
                    }
                }
                if(chosen==null)throw new IOException("No hay una ROM .nds en el ZIP");
                try(InputStream in=zip.getInputStream(chosen);OutputStream out=new FileOutputStream(temp)){copy(in,out,512L*1024*1024);}
                String hash=hash(temp);File target=new File(cache,hash+".nds");
                if(target.exists()){if(!hash(target).equals(hash))throw new IOException("Caché dañada");}
                else if(!temp.renameTo(target))throw new IOException("No se pudo preparar el juego");
                rom=target;ok=true;
            }finally{if(!ok||temp.exists())temp.delete();}
        }else{
            if(!name.toLowerCase(Locale.ROOT).endsWith(".nds"))throw new IOException("Nintendo DS admite .nds o .zip con una sola ROM. Extrae los .7z antes de subirlos.");
            rom=input;
        }
        if(rom.length()<512||rom.length()>512L*1024*1024)throw new IOException("Tamaño de ROM no válido");
        digest=hash(rom);dir=new File(context.getFilesDir(),"saves/nds/"+digest);
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("No se puede crear la carpeta de guardado");
    }
    File battery(){return new File(dir,"battery.sav");}
    File stateFile(){return new File(dir,"slot1.tss");}
    static void copy(InputStream in,OutputStream out,long limit) throws IOException {
        byte[] b=new byte[65536];long total=0;int n;
        while((n=in.read(b))!=-1){total+=n;if(total>limit)throw new IOException("Archivo demasiado grande");out.write(b,0,n);}
    }
    static byte[] read(File f,int limit)throws IOException{try(InputStream in=new FileInputStream(f)){return read(in,limit);}}
    static byte[] read(InputStream in,int limit)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();copy(in,out,limit);return out.toByteArray();}
    static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
    static String hash(byte[] b)throws Exception{return hex(MessageDigest.getInstance("SHA-256").digest(b));}
    static String hash(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[]b=new byte[65536];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}return hex(d.digest());}
    static void write(File target,byte[] b)throws IOException{
        AtomicFile a=new AtomicFile(target);FileOutputStream out=null;
        try{out=a.startWrite();out.write(b);a.finishWrite(out);}catch(IOException e){if(out!=null)a.failWrite(out);throw e;}
    }
    static void copyAtomic(File source,File target)throws IOException{
        AtomicFile a=new AtomicFile(target);FileOutputStream out=null;
        try(InputStream in=new FileInputStream(source)){out=a.startWrite();copy(in,out,MAX_STATE+1024);a.finishWrite(out);}
        catch(IOException e){if(out!=null)a.failWrite(out);throw e;}
    }
    byte[] wrapState(byte[] state)throws Exception{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeUTF("TIRSO-DS-STATE-1");out.writeUTF(digest);out.writeUTF(DsCore.REVISION);out.writeUTF(hash(state));out.writeInt(state.length);out.write(state);return bytes.toByteArray();
    }
    byte[] unwrapState(byte[] bytes)throws Exception{
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(!in.readUTF().equals("TIRSO-DS-STATE-1")||!in.readUTF().equals(digest)||!in.readUTF().equals(DsCore.REVISION))throw new IOException("El estado pertenece a otro juego o versión del núcleo");
        String checksum=in.readUTF();int n=in.readInt();if(n<16||n>MAX_STATE||n!=in.available())throw new IOException("Estado incompleto");
        byte[] result=new byte[n];in.readFully(result);if(!hash(result).equals(checksum))throw new IOException("Estado dañado");return result;
    }
    void saveState(byte[] bytes)throws Exception{
        AtomicFile a=new AtomicFile(stateFile());FileOutputStream file=null;
        try{file=a.startWrite();DataOutputStream out=new DataOutputStream(file);
            out.writeUTF("TIRSO-DS-STATE-1");out.writeUTF(digest);out.writeUTF(DsCore.REVISION);out.writeUTF(hash(bytes));out.writeInt(bytes.length);out.write(bytes);out.flush();a.finishWrite(file);
        }catch(Exception e){if(file!=null)a.failWrite(file);throw e;}
    }
    byte[] loadState()throws Exception{
        try(DataInputStream in=new DataInputStream(new FileInputStream(stateFile()))){
            if(!in.readUTF().equals("TIRSO-DS-STATE-1")||!in.readUTF().equals(digest)||!in.readUTF().equals(DsCore.REVISION))throw new IOException("El estado pertenece a otro juego o versión del núcleo");
            String checksum=in.readUTF();int n=in.readInt();if(n<16||n>MAX_STATE||n!=in.available())throw new IOException("Estado incompleto");
            byte[] bytes=new byte[n];in.readFully(bytes);if(!hash(bytes).equals(checksum))throw new IOException("Estado dañado");return bytes;
        }
    }
    void backup(OutputStream stream)throws Exception{
        Map<String,File> files=new LinkedHashMap<>();
        if(battery().exists())files.put("battery.sav",battery());
        if(stateFile().exists())files.put("slot1.tss",stateFile());
        if(files.isEmpty())throw new IOException("Aún no hay partidas ni estados para respaldar");
        JSONObject hashes=new JSONObject();for(Map.Entry<String,File>e:files.entrySet())hashes.put(e.getKey(),hash(e.getValue()));
        JSONObject manifest=new JSONObject().put("schema",1).put("rom",digest).put("core",DsCore.REVISION).put("files",hashes);
        try(ZipOutputStream out=new ZipOutputStream(stream)){
            out.putNextEntry(new ZipEntry("manifest.json"));out.write(manifest.toString().getBytes(StandardCharsets.UTF_8));out.closeEntry();
            for(Map.Entry<String,File>e:files.entrySet()){
                out.putNextEntry(new ZipEntry(e.getKey()));try(InputStream in=new FileInputStream(e.getValue())){copy(in,out,MAX_STATE+1024);}out.closeEntry();
            }
        }
    }
    void restore(InputStream stream)throws Exception{
        Map<String,byte[]> files=new LinkedHashMap<>();byte[] meta=null;int count=0;
        try(ZipInputStream in=new ZipInputStream(stream)){
            ZipEntry e;while((e=in.getNextEntry())!=null){
                String name=e.getName();if(++count>3||e.isDirectory()||(!name.equals("manifest.json")&&!name.equals("battery.sav")&&!name.equals("slot1.tss")))throw new IOException("Formato de respaldo no válido");
                if(files.containsKey(name)||(name.equals("manifest.json")&&meta!=null))throw new IOException("Archivo duplicado en respaldo");
                byte[] b=read(in,name.equals("manifest.json")?8192:name.equals("battery.sav")?MAX_SAVE:MAX_STATE+1024);
                if(name.equals("manifest.json"))meta=b;else files.put(name,b);
            }
        }
        if(meta==null||files.isEmpty())throw new IOException("Respaldo incompleto");
        JSONObject manifest=new JSONObject(new String(meta,StandardCharsets.UTF_8));JSONObject hashes=manifest.getJSONObject("files");
        if(manifest.getInt("schema")!=1||!digest.equals(manifest.getString("rom"))||hashes.length()!=files.size())throw new IOException("El respaldo no pertenece a este juego");
        for(Map.Entry<String,byte[]>e:files.entrySet()){
            if(!hash(e.getValue()).equals(hashes.getString(e.getKey())))throw new IOException("Respaldo dañado");
            if(e.getKey().equals("slot1.tss"))unwrapState(e.getValue());
        }
        Map<String,File> old=new LinkedHashMap<>();boolean preserve=false;
        try{
            for(String name:files.keySet()){
                File f=new File(dir,name),saved=null;
                if(f.exists()){saved=File.createTempFile("restore-old-"+name+"-",".bak",dir);old.put(name,saved);copyAtomic(f,saved);}else old.put(name,null);
            }
            try{for(Map.Entry<String,byte[]>e:files.entrySet())write(new File(dir,e.getKey()),e.getValue());}
            catch(Exception x){
                for(Map.Entry<String,File>e:old.entrySet())try{File f=new File(dir,e.getKey());if(e.getValue()==null){if(f.exists()&&!f.delete())throw new IOException("No se pudo revertir la restauración");}else copyAtomic(e.getValue(),f);}
                catch(IOException rollback){preserve=true;x.addSuppressed(rollback);}throw x;
            }
        }finally{if(!preserve)for(File f:old.values())if(f!=null)f.delete();}
    }
}
