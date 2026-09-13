package com.mr131.redplayer;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

/** Version, privacy and safe temporary-file management. */
public final class AboutActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("About 131 Red Player");
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(dp(22),dp(20),dp(22),dp(28));RedPlayerBackground.apply(root);
        root.addView(LuxuryIconView.create(this,R.drawable.icon_info_thick,"About 131 Red Player emblem"));
        TextView title=text("131 RED PLAYER",28,Color.WHITE);title.setContentDescription("About and Privacy Screen");root.addView(title);
        root.addView(text("Version "+BuildConfig.VERSION_NAME,17,Color.LTGRAY));
        root.addView(text("Private, ad-free video and music playback built from selected VLC and XPlayer-style controls.",16,Color.WHITE));
        root.addView(text("PRIVACY",20,getColor(R.color.red_player)));
        root.addView(text("Your local videos stay on your device unless you choose Cast, a network address, cloud storage, sharing, or subtitle download. History, bookmarks, settings, and saved addresses remain in this app. Cloud providers control their own sign-in and access.",15,Color.LTGRAY));
        root.addView(text("4K MODE",20,getColor(R.color.red_player)));
        root.addView(text("4K Ultra HD mode selects the highest real video track available. It does not create missing picture detail or claim AI upscaling.",15,Color.LTGRAY));
        Button clear=button("CLEAR TEMPORARY CACHE");clear.setOnClickListener(v->confirmClear());root.addView(clear);
        root.addView(text("Clearing temporary cache does not delete your personal videos, vault files, playlists, bookmarks, or settings.",13,Color.LTGRAY));
        scroll.addView(root);setContentView(scroll);
    }

    private void confirmClear(){new AlertDialog.Builder(this).setTitle("Clear temporary cache?").setMessage("Personal videos and saved settings will not be deleted.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{long bytes=size(getCacheDir());boolean ok=clear(getCacheDir());Toast.makeText(this,ok?"Temporary cache cleared ("+human(bytes)+")":"Some temporary files are still in use",Toast.LENGTH_LONG).show();}).show();}
    private boolean clear(File folder){File[] files=folder.listFiles();if(files==null)return true;boolean ok=true;for(File file:files){if(file.isDirectory())ok&=clear(file);if(!file.delete()&&file.exists())ok=false;}return ok;}
    private long size(File file){if(file==null||!file.exists())return 0;if(file.isFile())return file.length();long total=0;File[] files=file.listFiles();if(files!=null)for(File child:files)total+=size(child);return total;}
    private String human(long bytes){if(bytes<1024)return bytes+" B";if(bytes<1024*1024)return (bytes/1024)+" KB";return (bytes/(1024*1024))+" MB";}
    private TextView text(String value,int size,int color){TextView t=new ChromePulseTextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(9),0,dp(9));return t;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setBackgroundColor(getColor(R.color.red_player));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(20),0,dp(8));b.setLayoutParams(p);return b;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
