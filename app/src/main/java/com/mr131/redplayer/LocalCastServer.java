package com.mr131.redplayer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small Wi-Fi HTTP range server that lets a Chromecast read a local phone video. */
final class LocalCastServer {
    private final Context context;private final Uri uri;private final String token=UUID.randomUUID().toString();private final ExecutorService workers=Executors.newCachedThreadPool();private ServerSocket server;private volatile boolean running;
    LocalCastServer(Context c,Uri u){context=c.getApplicationContext();uri=u;}
    String start() throws Exception{stop();server=new ServerSocket(0);running=true;new Thread(this::accept,"local-cast-server").start();String ip=wifiAddress();if(ip==null)throw new Exception("Connect phone and TV to the same Wi-Fi");return "http://"+ip+":"+server.getLocalPort()+"/video/"+token;}
    private void accept(){while(running)try{Socket socket=server.accept();workers.execute(()->serve(socket));}catch(Exception e){if(running)running=false;}}
    private void serve(Socket socket){try(socket){socket.setSoTimeout(10000);BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));String request=reader.readLine();if(request==null||!request.contains("/video/"+token)){reply(socket.getOutputStream(),"404 Not Found",0,null,0,-1);return;}long start=0;String line;while((line=reader.readLine())!=null&&!line.isEmpty())if(line.toLowerCase(Locale.US).startsWith("range: bytes=")){String value=line.substring(line.indexOf('=')+1).split("-")[0].trim();if(!value.isEmpty())start=Long.parseLong(value);}long length=length();if(length<=0)throw new Exception("Unknown file length");start=Math.min(start,length-1);long remaining=length-start;String mime=context.getContentResolver().getType(uri);if(mime==null)mime="video/mp4";OutputStream out=socket.getOutputStream();reply(out,start>0?"206 Partial Content":"200 OK",remaining,mime,start,length);try(InputStream in=context.getContentResolver().openInputStream(uri)){if(in==null)throw new Exception("File unavailable");skip(in,start);byte[] buffer=new byte[128*1024];long left=remaining;while(left>0){int read=in.read(buffer,0,(int)Math.min(buffer.length,left));if(read<0)break;out.write(buffer,0,read);left-=read;}out.flush();}}catch(Exception ignored){}}
    private void reply(OutputStream out,String status,long size,String mime,long start,long total)throws Exception{StringBuilder h=new StringBuilder("HTTP/1.1 ").append(status).append("\r\nAccept-Ranges: bytes\r\nConnection: close\r\n");if(mime!=null)h.append("Content-Type: ").append(mime).append("\r\n");if(size>0)h.append("Content-Length: ").append(size).append("\r\n");if(total>0&&status.startsWith("206"))h.append("Content-Range: bytes ").append(start).append('-').append(total-1).append('/').append(total).append("\r\n");h.append("\r\n");out.write(h.toString().getBytes(StandardCharsets.US_ASCII));}
    private long length(){try(AssetFileDescriptor f=context.getContentResolver().openAssetFileDescriptor(uri,"r")){return f==null?-1:f.getLength();}catch(Exception e){return-1;}}
    private void skip(InputStream in,long count)throws Exception{long done=0;while(done<count){long n=in.skip(count-done);if(n<=0){if(in.read()<0)break;n=1;}done+=n;}}
    private String wifiAddress()throws Exception{for(NetworkInterface n:Collections.list(NetworkInterface.getNetworkInterfaces()))if(n.isUp()&&!n.isLoopback())for(InetAddress a:Collections.list(n.getInetAddresses()))if(a instanceof Inet4Address&&!a.isLoopbackAddress()&&a.isSiteLocalAddress())return a.getHostAddress();return null;}
    void stop(){running=false;if(server!=null)try{server.close();}catch(Exception ignored){}server=null;}
}
