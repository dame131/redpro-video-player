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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public final class SubtitleDownloadActivity extends AppCompatActivity {
    private final ArrayList<Integer> fileIds=new ArrayList<>();private final ArrayList<String> labels=new ArrayList<>();private ArrayAdapter<String> adapter;private EditText query,key;private TextView status;
    @Override protected void onCreate(Bundle state){super.onCreate(state);setTitle("Subtitle Downloader");String video=getIntent().getStringExtra("video_name");if(video==null)video="";video=video.replaceFirst("(?i)\\.[a-z0-9]{2,5}$","").replace('.',' ');
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(14),dp(16),dp(14));RedPlayerBackground.apply(root);
        TextView title=text("SUBTITLE DOWNLOADER",24,Color.WHITE);title.setContentDescription("Subtitle Downloader Screen 8");root.addView(title);
        query=input("Movie or episode name");query.setText(video);root.addView(query);
        key=input("OpenSubtitles API key");key.setInputType(129);key.setText(getSharedPreferences("red_player",MODE_PRIVATE).getString("subtitle_api_key",""));root.addView(key);
        Button search=button("SEARCH ENGLISH SUBTITLES");search.setOnClickListener(v->search());root.addView(search);status=text("Enter your free OpenSubtitles API key once. The app saves it only on this phone.",13,Color.LTGRAY);root.addView(status);
        ListView list=new ListView(this);adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels){@NonNull @Override public android.view.View getView(int p,android.view.View c,@NonNull android.view.ViewGroup g){TextView t=(TextView)super.getView(p,c,g);t.setTextColor(Color.WHITE);t.setMinHeight(dp(58));return t;}};list.setAdapter(adapter);list.setOnItemClickListener((p,v,i,id)->download(fileIds.get(i)));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    private void search(){String api=key.getText().toString().trim(),name=query.getText().toString().trim();if(api.isEmpty()||name.isEmpty()){toast("Enter the video name and API key");return;}getSharedPreferences("red_player",MODE_PRIVATE).edit().putString("subtitle_api_key",api).apply();status.setText("Searching…");new Thread(()->{try{String url="https://api.opensubtitles.com/api/v1/subtitles?languages=en&query="+URLEncoder.encode(name,"UTF-8")+"&order_by=download_count";JSONObject json=new JSONObject(request(url,"GET",api,null));JSONArray data=json.getJSONArray("data");ArrayList<Integer> ids=new ArrayList<>();ArrayList<String> rows=new ArrayList<>();for(int i=0;i<Math.min(20,data.length());i++){JSONObject a=data.getJSONObject(i).getJSONObject("attributes");JSONArray files=a.optJSONArray("files");if(files==null||files.length()==0)continue;ids.add(files.getJSONObject(0).getInt("file_id"));rows.add(a.optString("release","Matched subtitle")+"\n"+a.optString("language","en")+" • "+a.optInt("download_count",0)+" downloads");}runOnUiThread(()->{fileIds.clear();fileIds.addAll(ids);labels.clear();labels.addAll(rows);adapter.notifyDataSetChanged();status.setText(rows.isEmpty()?"No matching subtitles found":rows.size()+" matches • tap one to download");});}catch(Exception e){runOnUiThread(()->status.setText("Search failed: "+e.getMessage()));}}).start();}
    private void download(int id){String api=key.getText().toString().trim();status.setText("Downloading and syncing…");new Thread(()->{try{String body=new JSONObject().put("file_id",id).toString();JSONObject result=new JSONObject(request("https://api.opensubtitles.com/api/v1/download","POST",api,body));String link=result.getString("link");HttpURLConnection c=(HttpURLConnection)new URL(link).openConnection();c.setRequestProperty("User-Agent","131RedPlayer v1.4");File folder=new File(getFilesDir(),"subtitles");folder.mkdirs();File file=new File(folder,"matched-"+id+".srt");try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(file)){byte[] b=new byte[32768];int n;while((n=in.read(b))>0)out.write(b,0,n);}Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",file);runOnUiThread(()->{setResult(RESULT_OK,new Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));finish();});}catch(Exception e){runOnUiThread(()->status.setText("Download failed: "+e.getMessage()));}}).start();}
    private String request(String address,String method,String api,String body)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();c.setRequestMethod(method);c.setRequestProperty("Api-Key",api);c.setRequestProperty("User-Agent","131RedPlayer v1.4");c.setRequestProperty("Accept","application/json");if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));}int code=c.getResponseCode();InputStream stream=code<400?c.getInputStream():c.getErrorStream();BufferedReader r=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();String line;while((line=r.readLine())!=null)s.append(line);if(code>=400)throw new Exception("service returned "+code);return s.toString();}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.LTGRAY);e.setBackgroundColor(Color.rgb(34,34,34));e.setPadding(dp(12),dp(8),dp(12),dp(8));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(8),0,0);e.setLayoutParams(p);return e;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setBackgroundColor(Color.rgb(214,10,0));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(10),0,0);b.setLayoutParams(p);return b;}
    private TextView text(String s,int z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(8),0,dp(8));return t;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
