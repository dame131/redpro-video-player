package com.mr131.redplayer;

import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;

public final class VaultActivity extends AppCompatActivity {
    private final ArrayList<File> files = new ArrayList<>();
    private final ArrayList<String> names = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private LinearLayout lockedView;

    private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) importVideo(uri); });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Private Vault");
        showLockedScreen();
    }

    private void showLockedScreen() {
        lockedView = new LinearLayout(this); lockedView.setOrientation(LinearLayout.VERTICAL);
        lockedView.setGravity(Gravity.CENTER); lockedView.setPadding(dp(28),dp(28),dp(28),dp(28));
        RedPlayerBackground.apply(lockedView);
        TextView title = text("PRIVATE MEDIA VAULT", 25, Color.WHITE); title.setContentDescription("Private Vault"); lockedView.addView(title);
        lockedView.addView(text("Protected by your fingerprint or private PIN",15,Color.LTGRAY));
        Button fingerprint=button("UNLOCK WITH FINGERPRINT"); fingerprint.setOnClickListener(v->unlockBiometric()); lockedView.addView(fingerprint);
        Button pin=button(hasPin()?"UNLOCK WITH PIN":"CREATE VAULT PIN"); pin.setOnClickListener(v->showPin()); lockedView.addView(pin);
        setContentView(lockedView);
    }

    private void unlockBiometric() {
        int authenticators=BiometricManager.Authenticators.BIOMETRIC_STRONG|BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        if(BiometricManager.from(this).canAuthenticate(authenticators)!=BiometricManager.BIOMETRIC_SUCCESS){toast("Fingerprint or phone lock is not ready");return;}
        new BiometricPrompt(this, ContextCompat.getMainExecutor(this), new BiometricPrompt.AuthenticationCallback(){
            @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result){showVault();}
        }).authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle("131 Private Vault").setSubtitle("Unlock private videos").setAllowedAuthenticators(authenticators).build());
    }

    private void showPin() {
        EditText input=new EditText(this); input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD); input.setHint("4–8 digit PIN");input.setTextColor(Color.WHITE);input.setHintTextColor(Color.LTGRAY);input.setBackgroundColor(getColor(R.color.carbon));input.setPadding(dp(16),dp(12),dp(16),dp(12));
        boolean creating=!hasPin();
        new AlertDialog.Builder(this).setTitle(creating?"Create vault PIN":"Enter vault PIN").setView(input).setNegativeButton("Cancel",null).setPositiveButton(creating?"Save":"Unlock",(d,w)->{
            String pin=input.getText().toString();
            if(creating){if(pin.length()<4||pin.length()>8){toast("PIN must be 4 to 8 digits");return;}getPreferences(MODE_PRIVATE).edit().putString("pin",hash(pin)).apply();showVault();}
            else if(hash(pin).equals(getPreferences(MODE_PRIVATE).getString("pin","")))showVault(); else toast("Wrong PIN");
        }).show();
    }

    private void showVault() {
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(12),dp(14),dp(12));RedPlayerBackground.apply(root);
        TextView title=text("PRIVATE MEDIA VAULT",24,Color.WHITE);title.setContentDescription("Vault Unlocked");root.addView(title);
        Button add=button("ADD PRIVATE VIDEO");add.setOnClickListener(v->picker.launch(new String[]{"video/*"}));root.addView(add);
        ListView list=new ListView(this);adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,names){@NonNull @Override public View getView(int p,View c,@NonNull android.view.ViewGroup g){TextView v=(TextView)super.getView(p,c,g);v.setTextColor(Color.WHITE);v.setMinHeight(dp(60));return v;}};list.setAdapter(adapter);
        list.setOnItemClickListener((p,v,i,id)->{Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",files.get(i));setResult(RESULT_OK,new Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));finish();});
        root.addView(list,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);reload();
    }

    private void importVideo(Uri uri) {
        File folder=new File(getFilesDir(),"vault"); if(!folder.isDirectory()&&!folder.mkdirs()){toast("Could not create vault");return;}
        String name=displayName(uri).replaceAll("[^a-zA-Z0-9._ -]","_");File target=new File(folder,System.currentTimeMillis()+"_"+name);
        try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(target)){if(in==null)throw new Exception("File unavailable");byte[] buf=new byte[65536];int read;while((read=in.read(buf))>0)out.write(buf,0,read);toast("Video secured in vault");reload();}catch(Exception e){target.delete();toast("Could not secure that video");}
    }

    private void reload(){files.clear();names.clear();File folder=new File(getFilesDir(),"vault");File[] found=folder.listFiles();if(found!=null)for(File f:found){files.add(f);String n=f.getName();names.add(n.contains("_")?n.substring(n.indexOf('_')+1):n);}if(adapter!=null)adapter.notifyDataSetChanged();}
    private boolean hasPin(){return !getPreferences(MODE_PRIVATE).getString("pin","").isEmpty();}
    private String hash(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest((getPackageName()+value).getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format("%02x",x));return s.toString();}catch(Exception e){return value;}}
    private String displayName(Uri uri){try(android.database.Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())return c.getString(0);}catch(Exception ignored){}return "private-video";}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setBackgroundColor(getColor(R.color.red_player));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(12),0,0);b.setLayoutParams(p);return b;}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(8),0,dp(8));return t;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
