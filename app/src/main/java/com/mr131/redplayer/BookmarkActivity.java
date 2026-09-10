package com.mr131.redplayer;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Locale;

public final class BookmarkActivity extends AppCompatActivity {
 private final ArrayList<JSONObject> items=new ArrayList<>();private final ArrayList<String> labels=new ArrayList<>();private ArrayAdapter<String> adapter;private TextView status;
 @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(screen());load();}
 private LinearLayout screen(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(12));RedPlayerBackground.apply(root);root.setContentDescription("Video Bookmarks Screen");root.addView(LuxuryIconView.create(this,R.drawable.icon_history_thick,"Video bookmarks emblem"));root.addView(text("VIDEO BOOKMARKS",26,Color.WHITE));status=text("Saved moments from every video",14,Color.LTGRAY);root.addView(status);ListView list=new ListView(this);adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels){public android.view.View getView(int p,android.view.View c,android.view.ViewGroup g){TextView row=(TextView)super.getView(p,c,g);row.setTextColor(Color.WHITE);row.setMinHeight(dp(68));return row;}};list.setAdapter(adapter);list.setOnItemClickListener((p,v,i,id)->open(i));list.setOnItemLongClickListener((p,v,i,id)->remove(i));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));Button clear=button("CLEAR ALL BOOKMARKS");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Clear every bookmark?").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{getSharedPreferences("red_player",MODE_PRIVATE).edit().remove("video_bookmarks").apply();load();}).show());root.addView(clear);return root;}
 private void load(){items.clear();labels.clear();try{JSONArray a=new JSONArray(getSharedPreferences("red_player",MODE_PRIVATE).getString("video_bookmarks","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);items.add(o);labels.add(o.optString("label","Bookmark")+"\n"+o.optString("name","Video")+"  •  "+time(o.optLong("position")));}}catch(Exception ignored){}if(adapter!=null)adapter.notifyDataSetChanged();if(status!=null)status.setText(items.isEmpty()?"No bookmarks yet":items.size()+" saved video moments");}
 private void open(int i){JSONObject o=items.get(i);setResult(RESULT_OK,new Intent().setData(Uri.parse(o.optString("uri"))).putExtra("position",o.optLong("position")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));finish();}
 private void remove(int i){new AlertDialog.Builder(this).setTitle("Remove bookmark?").setMessage(labels.get(i)).setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{items.remove(i);save();load();}).show();}
 private void save(){JSONArray a=new JSONArray();for(JSONObject o:items)a.put(o);getSharedPreferences("red_player",MODE_PRIVATE).edit().putString("video_bookmarks",a.toString()).apply();}
 private String time(long ms){long s=Math.max(0,ms/1000);return String.format(Locale.US,"%d:%02d:%02d",s/3600,(s/60)%60,s%60);}private TextView text(String s,int z,int c){TextView t=new ChromePulseTextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(7),0,dp(7));return t;}private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setBackgroundColor(getColor(R.color.red_player));return b;}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
