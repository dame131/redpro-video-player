package com.mr131.redplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/** Drive-compatible backup and restore for settings, history, favorites and playlists. */
public final class BackupActivity extends AppCompatActivity {
    private TextView status;
    private final ActivityResultLauncher<String> exportFile=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),this::exportTo);
    private final ActivityResultLauncher<String[]> importFile=registerForActivityResult(new ActivityResultContracts.OpenDocument(),this::importFrom);
    @Override protected void onCreate(Bundle state){super.onCreate(state);setTitle("Backup & Restore");LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(24),dp(24),dp(24));RedPlayerBackground.apply(root);root.addView(LuxuryIconView.create(this,R.drawable.icon_cloud_thick,"Backup emblem"));TextView title=text("BACKUP & RESTORE",25,Color.WHITE);title.setContentDescription("Backup Restore Screen");root.addView(title);root.addView(text("Save settings, history, favorites, playlists and network addresses to your phone or Google Drive.",15,Color.LTGRAY));Button backup=button("CREATE BACKUP");backup.setOnClickListener(v->exportFile.launch("131-red-player-backup.json"));root.addView(backup);Button restore=button("RESTORE BACKUP");restore.setOnClickListener(v->importFile.launch(new String[]{"application/json","text/*"}));root.addView(restore);status=text("Ready",14,Color.LTGRAY);root.addView(status);setContentView(root);}
    private void exportTo(Uri uri){if(uri==null)return;new Thread(()->{try{JSONObject root=new JSONObject();JSONObject values=new JSONObject();for(Map.Entry<String,?> e:prefs().getAll().entrySet()){Object v=e.getValue();if(v instanceof Set)values.put(e.getKey(),new JSONArray((Set<?>)v));else values.put(e.getKey(),v);}root.put("version",1);root.put("values",values);try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new Exception("File unavailable");out.write(root.toString(2).getBytes(StandardCharsets.UTF_8));}show("Backup saved");}catch(Exception e){show("Backup failed");}},"backup-export").start();}
    private void importFrom(Uri uri){if(uri==null)return;new Thread(()->{try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new Exception("File unavailable");JSONObject values=new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8)).getJSONObject("values");SharedPreferences.Editor edit=prefs().edit().clear();for(String key:values.keySet()){Object v=values.get(key);if(v instanceof Boolean)edit.putBoolean(key,(Boolean)v);else if(v instanceof Integer)edit.putInt(key,(Integer)v);else if(v instanceof Long)edit.putLong(key,(Long)v);else if(v instanceof Number)edit.putFloat(key,((Number)v).floatValue());else if(v instanceof JSONArray){java.util.HashSet<String> set=new java.util.HashSet<>();JSONArray a=(JSONArray)v;for(int i=0;i<a.length();i++)set.add(a.getString(i));edit.putStringSet(key,set);}else edit.putString(key,String.valueOf(v));}edit.apply();show("Backup restored");}catch(Exception e){show("Restore failed: invalid backup");}},"backup-import").start();}
    private SharedPreferences prefs(){return getSharedPreferences("red_player",MODE_PRIVATE);}
    private void show(String value){runOnUiThread(()->{status.setText(value);Toast.makeText(this,value,Toast.LENGTH_SHORT).show();});}
    private Button button(String value){Button b=new Button(this);b.setText(value);b.setTextColor(Color.WHITE);b.setBackgroundColor(getColor(R.color.red_player));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(54));p.setMargins(0,dp(14),0,0);b.setLayoutParams(p);return b;}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(8),0,dp(8));return t;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
