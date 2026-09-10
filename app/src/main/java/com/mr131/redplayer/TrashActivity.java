package com.mr131.redplayer;

import android.Manifest;
import android.app.PendingIntent;
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
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Collections;

public final class TrashActivity extends AppCompatActivity {
 private final ArrayList<Uri> uris=new ArrayList<>();private final ArrayList<String> names=new ArrayList<>();private ArrayAdapter<String> adapter;private TextView status;private Uri pending;private boolean permanent;
 private final ActivityResultLauncher<IntentSenderRequest> approval=registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),r->{if(r.getResultCode()==RESULT_OK){toast(permanent?"Video permanently deleted":"Video recovered");load();}});
 @Override protected void onCreate(Bundle b){super.onCreate(b);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(12));RedPlayerBackground.apply(root);root.addView(LuxuryIconView.create(this,R.drawable.icon_library_thick,"Trash emblem"));TextView title=text("VIDEO TRASH",25,Color.WHITE);title.setContentDescription("Video Trash Screen");root.addView(title);status=text("Recover deleted videos for 30 days",14,Color.LTGRAY);root.addView(status);ListView list=new ListView(this);adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,names){public android.view.View getView(int p,android.view.View c,android.view.ViewGroup g){TextView v=(TextView)super.getView(p,c,g);v.setTextColor(Color.WHITE);v.setMinHeight(dp(64));return v;}};list.setAdapter(adapter);list.setOnItemClickListener((p,v,i,id)->recover(i));list.setOnItemLongClickListener((p,v,i,id)->{removeForever(i);return true;});root.addView(list,new LinearLayout.LayoutParams(-1,0,1));Button close=new Button(this);close.setText("CLOSE");close.setTextColor(Color.WHITE);close.setBackgroundColor(getColor(R.color.red_player));close.setOnClickListener(v->finish());root.addView(close);setContentView(root);load();}
 private void load(){uris.clear();names.clear();if(Build.VERSION.SDK_INT<30){status.setText("Recoverable Trash requires Android 11 or newer");adapter.notifyDataSetChanged();return;}new Thread(()->{Bundle q=new Bundle();q.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED,MediaStore.MATCH_ONLY);String[] cols={MediaStore.Video.Media._ID,MediaStore.Video.Media.DISPLAY_NAME,MediaStore.MediaColumns.DATE_EXPIRES};try(Cursor c=getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,cols,q,null)){if(c!=null)while(c.moveToNext()){uris.add(Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,String.valueOf(c.getLong(0))));names.add((c.getString(1)==null?"Video":c.getString(1))+"\nTap to recover • Hold to delete forever");}}runOnUiThread(()->{adapter.notifyDataSetChanged();status.setText(uris.isEmpty()?"Trash is empty":uris.size()+" recoverable videos");});},"trash-scan").start();}
 private void recover(int i){new AlertDialog.Builder(this).setTitle("Recover video?").setMessage(names.get(i)).setNegativeButton("Cancel",null).setPositiveButton("Recover",(d,w)->request(uris.get(i),false)).show();}
 private void removeForever(int i){new AlertDialog.Builder(this).setTitle("Delete forever?").setMessage("This cannot be undone.").setNegativeButton("Cancel",null).setPositiveButton("Delete forever",(d,w)->request(uris.get(i),true)).show();}
 private void request(Uri uri,boolean forever){pending=uri;permanent=forever;try{PendingIntent p=forever?MediaStore.createDeleteRequest(getContentResolver(),Collections.singletonList(uri)):MediaStore.createTrashRequest(getContentResolver(),Collections.singletonList(uri),false);approval.launch(new IntentSenderRequest.Builder(p.getIntentSender()).build());}catch(Exception e){toast("Phone permission request failed");}}
 private TextView text(String s,int z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(8),0,dp(8));return t;}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}