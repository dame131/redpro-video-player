package com.mr131.redplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public final class HistoryActivity extends AppCompatActivity {
    private final ArrayList<JSONObject> entries=new ArrayList<>();private final ArrayList<String> labels=new ArrayList<>();private ArrayAdapter<String> adapter;private SharedPreferences prefs;
    @Override protected void onCreate(Bundle state){super.onCreate(state);setTitle("History & Recovery");prefs=getSharedPreferences("red_player",MODE_PRIVATE);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(12),dp(14),dp(12));root.setBackgroundColor(Color.BLACK);
        TextView title=text("HISTORY & RECOVERY",24,Color.WHITE);title.setContentDescription("History Screen 18");root.addView(title);String error=prefs.getString("last_error","");root.addView(text(error.isEmpty()?"No recent playback errors":"Last recovery: "+error,13,error.isEmpty()?Color.LTGRAY:Color.rgb(214,10,0)));
        Button clear=new Button(this);clear.setText("CLEAR HISTORY");clear.setTextColor(Color.WHITE);clear.setBackgroundColor(Color.rgb(34,34,34));clear.setOnClickListener(v->{prefs.edit().remove("history").remove("last_error").apply();entries.clear();labels.clear();adapter.notifyDataSetChanged();});root.addView(clear,new LinearLayout.LayoutParams(-1,dp(48)));
        ListView list=new ListView(this);adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels){@NonNull @Override public android.view.View getView(int p,android.view.View c,@NonNull android.view.ViewGroup g){TextView t=(TextView)super.getView(p,c,g);t.setTextColor(Color.WHITE);t.setMinHeight(dp(68));return t;}};list.setAdapter(adapter);list.setOnItemClickListener((p,v,i,id)->{if(i>=entries.size())return;Uri uri=Uri.parse(entries.get(i).optString("uri"));setResult(RESULT_OK,new Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));finish();});root.addView(list,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);load();}
    private void load(){entries.clear();labels.clear();try{JSONArray a=new JSONArray(prefs.getString("history","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);entries.add(o);long ms=o.optLong("position");String when=DateFormat.getMediumDateFormat(this).format(new Date(o.optLong("watched")))+" "+DateFormat.getTimeFormat(this).format(new Date(o.optLong("watched")));labels.add(o.optString("name","Video")+"\nResume at "+String.format(Locale.US,"%d:%02d",ms/60000,(ms/1000)%60)+" • "+when);}}catch(Exception ignored){}if(labels.isEmpty())labels.add("No viewing history yet");adapter.notifyDataSetChanged();}
    private TextView text(String s,int z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(8),0,dp(8));return t;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
