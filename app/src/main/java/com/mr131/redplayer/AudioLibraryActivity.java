package com.mr131.redplayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Locale;

public final class AudioLibraryActivity extends AppCompatActivity {
 private final ArrayList<Track> all=new ArrayList<>(),shown=new ArrayList<>();private final ArrayList<String> labels=new ArrayList<>();private ArrayAdapter<String> adapter;private TextView count;private String query="";
 private final ActivityResultLauncher<String> permission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),ok->{if(ok)load();else count.setText("Music permission is off");});
 @Override protected void onCreate(Bundle b){super.onCreate(b);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(10),dp(14),dp(10));RedPlayerBackground.apply(root);root.addView(LuxuryIconView.create(this,R.drawable.icon_playlist_thick,"Music Library emblem"));TextView title=text("MUSIC LIBRARY",25,Color.WHITE);title.setContentDescription("Music Library Screen");root.addView(title);count=text("Scanning music…",13,Color.LTGRAY);root.addView(count);SearchView search=new SearchView(this);search.setQueryHint("Search songs, artists or albums");search.setIconifiedByDefault(false);search.setOnQueryTextListener(new SearchView.OnQueryTextListener(){public boolean onQueryTextSubmit(String q){filter(q);return true;}public boolean onQueryTextChange(String q){filter(q);return true;}});root.addView(search,new LinearLayout.LayoutParams(-1,dp(56)));ListView list=new ListView(this);adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels){@NonNull public android.view.View getView(int p,android.view.View c,@NonNull android.view.ViewGroup g){TextView v=(TextView)super.getView(p,c,g);v.setTextColor(Color.WHITE);v.setMinHeight(dp(70));return v;}};list.setAdapter(adapter);list.setOnItemClickListener((p,v,i,id)->choose(shown.get(i).uri));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);request();}
 private void request(){String p=Build.VERSION.SDK_INT>=33?Manifest.permission.READ_MEDIA_AUDIO:Manifest.permission.READ_EXTERNAL_STORAGE;if(ContextCompat.checkSelfPermission(this,p)==PackageManager.PERMISSION_GRANTED)load();else permission.launch(p);}
 private void load(){new Thread(()->{ArrayList<Track> found=new ArrayList<>();String[] cols={MediaStore.Audio.Media._ID,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.DURATION};try(Cursor c=getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,cols,MediaStore.Audio.Media.IS_MUSIC+"!=0",null,MediaStore.Audio.Media.DATE_ADDED+" DESC")){if(c!=null)while(c.moveToNext())found.add(new Track(Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,String.valueOf(c.getLong(0))),safe(c.getString(1),"Unknown track"),safe(c.getString(2),"Unknown artist"),safe(c.getString(3),"Unknown album"),c.getLong(4)));}runOnUiThread(()->{all.clear();all.addAll(found);filter(query);});},"music-scan").start();}
 private void filter(String q){query=q==null?"":q.trim().toLowerCase(Locale.US);shown.clear();labels.clear();for(Track t:all)if(query.isEmpty()||(t.title+" "+t.artist+" "+t.album).toLowerCase(Locale.US).contains(query)){shown.add(t);labels.add(t.title+"\n"+t.artist+" • "+t.album+" • "+time(t.duration));}adapter.notifyDataSetChanged();count.setText(shown.size()+" songs");}
 private void choose(Uri uri){setResult(RESULT_OK,new Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));finish();}private String safe(String s,String d){return s==null||s.trim().isEmpty()?d:s;}private String time(long ms){long s=ms/1000;return String.format(Locale.US,"%d:%02d",s/60,s%60);}private TextView text(String s,int z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(7),0,dp(7));return t;}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private static final class Track{final Uri uri;final String title,artist,album;final long duration;Track(Uri u,String t,String a,String l,long d){uri=u;title=t;artist=a;album=l;duration=d;}}
}