package com.mr131.redplayer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public final class CoreFeatureTest {
    @Test public void encryptionAndLocalCastRangeWork() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        byte[] plain=("131 private video payload ".repeat(5000)).getBytes(StandardCharsets.UTF_8);
        File encrypted=new File(context.getCacheDir(),"vault-test.vlt"),restored=new File(context.getCacheDir(),"vault-restored.bin");
        VaultCrypto.encrypt(new ByteArrayInputStream(plain),encrypted);
        byte[] cipher=read(encrypted);
        assertFalse("Vault stored readable video bytes",contains(cipher,"private video payload".getBytes(StandardCharsets.UTF_8)));
        VaultCrypto.decrypt(encrypted,restored);
        assertArrayEquals("Vault round trip changed the media",plain,read(restored));

        File video=new File(context.getCacheDir(),"cast-test.mp4");try(FileOutputStream out=new FileOutputStream(video)){out.write(plain);}
        Uri uri=FileProvider.getUriForFile(context,context.getPackageName()+".files",video);
        LocalCastServer server=new LocalCastServer(context,uri);String publicUrl=server.start();URL parsed=new URL(publicUrl);
        URL local=new URL("http","127.0.0.1",parsed.getPort(),parsed.getPath());
        HttpURLConnection connection=(HttpURLConnection)local.openConnection();connection.setRequestProperty("Range","bytes=10-29");
        assertEquals(206,connection.getResponseCode());assertArrayEquals(Arrays.copyOfRange(plain,10,30),read(connection.getInputStream()));server.stop();
        encrypted.delete();restored.delete();video.delete();
    }
    private byte[] read(File file)throws Exception{try(FileInputStream in=new FileInputStream(file)){return read(in);}}
    private byte[] read(InputStream in)throws Exception{try(ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);return out.toByteArray();}}
    private boolean contains(byte[] data,byte[] part){outer:for(int i=0;i<=data.length-part.length;i++){for(int j=0;j<part.length;j++)if(data[i+j]!=part[j])continue outer;return true;}return false;}
}
