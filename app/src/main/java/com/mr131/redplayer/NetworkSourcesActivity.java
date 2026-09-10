package com.mr131.redplayer;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;

import java.util.ArrayList;

/** Saved HTTP, RTSP, FTP and SMB locations for VLC-style network playback. */
public final class NetworkSourcesActivity extends AppCompatActivity {
    private final ArrayList<String> sources=new ArrayList<>();
    private ArrayAdapter<String> adapter;
    @Override protected void onCreate(Bundle state){super.onCreate(state);setTitle("Network Sources");load();LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(14),dp(16),dp(14));RedPlayerBackground.apply(root);root.addView(LuxuryIconView.create(this,R.drawable.icon_network_thick,"Network Sources emblem"));TextView title=text("NETWORK & HOME SERVER",24,Color.WHITE);title.setContentDescription("Network Sources Screen");root.addView(title);root.addView(text("HTTP • HLS • RTSP • SMB • FTP • SFTP • NFS • UDP • RTP • UPnP",14,Color.LTGRAY));Button add=button("ADD NETWORK ADDRESS");add.setOnClickListener(v->addSource());root.addView(add);ListView list=new ListView(this);adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,sources){@Override public android.view.View getView(int p,android.view.View c,android.view.ViewGroup g){TextView row=(TextView)super.getView(p,c,g);row.setTextColor(Color.WHITE);row.setMinHeight(dp(64));return row;}};list.setAdapter(adapter);list.setOnItemClickListener((p,v,i,id)->open(sources.get(i)));list.setOnItemLongClickListener((p,v,i,id)->{new AlertDialog.Builder(this).setTitle("Remove saved address?").setMessage(sources.get(i)).setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{sources.remove(i);save();adapter.notifyDataSetChanged();}).show();return true;});root.addView(list,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private void addSource(){EditText input=new EditText(this);input.setHint("smb://, sftp://, nfs://, udp:// or https://…");input.setSingleLine(true);input.setTextColor(Color.WHITE);input.setHintTextColor(Color.LTGRAY);input.setBackgroundColor(getColor(R.color.carbon));input.setPadding(dp(12),dp(12),dp(12),dp(12));new AlertDialog.Builder(this).setTitle("Add network address").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save & Play",(d,w)->{String value=input.getText().toString().trim();if(!valid(value)){toast("Use a supported stream or server protocol");return;}if(!sources.contains(value)){sources.add(value);save();adapter.notifyDataSetChanged();}open(value);}).show();}
    private boolean valid(String value){String s=Uri.parse(value).getScheme();return s!=null&&(s.equalsIgnoreCase("http")||s.equalsIgnoreCase("https")||s.equalsIgnoreCase("rtsp")||s.equalsIgnoreCase("ftp")||s.equalsIgnoreCase("ftps")||s.equalsIgnoreCase("sftp")||s.equalsIgnoreCase("smb")||s.equalsIgnoreCase("nfs")||s.equalsIgnoreCase("udp")||s.equalsIgnoreCase("rtp")||s.equalsIgnoreCase("mms")||s.equalsIgnoreCase("upnp"));}
    private void open(String value){setResult(RESULT_OK,new Intent().setData(Uri.parse(value)));finish();}
    private void load(){try{JSONArray a=new JSONArray(getSharedPreferences("red_player",MODE_PRIVATE).getString("network_sources","[]"));for(int i=0;i<a.length();i++)sources.add(a.getString(i));}catch(Exception ignored){}}
    private void save(){getSharedPreferences("red_player",MODE_PRIVATE).edit().putString("network_sources",new JSONArray(sources).toString()).apply();}
    private Button button(String value){Button b=new Button(this);b.setText(value);b.setTextColor(Color.WHITE);b.setBackgroundColor(getColor(R.color.red_player));b.setMinHeight(dp(52));return b;}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(8),0,dp(8));return t;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void toast(String v){Toast.makeText(this,v,Toast.LENGTH_SHORT).show();}
}
