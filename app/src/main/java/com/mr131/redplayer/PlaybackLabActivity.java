package com.mr131.redplayer;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public final class PlaybackLabActivity extends AppCompatActivity {
 private SharedPreferences prefs;
 private TextView boostValue,audioDelayValue,subtitleDelayValue,cacheValue,deinterlaceValue,encodingValue;
 private static final String[] DEINTERLACE={"Off","Auto","Yadif","Blend"};
 private static final String[] ENCODING={"Auto","UTF-8","Windows-1252","ISO-8859-1","UTF-16"};
 @Override protected void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("red_player",MODE_PRIVATE);setContentView(screen());}
 private LinearLayout screen(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(12),dp(18),dp(12));RedPlayerBackground.apply(root);root.setContentDescription("Playback Lab Screen");
  root.addView(LuxuryIconView.create(this,R.drawable.icon_equalizer_thick,"Playback Lab emblem"));
  root.addView(text("PLAYBACK LAB",26,Color.WHITE));root.addView(text("Fine control for difficult video, sound and subtitles",14,Color.LTGRAY));
  CheckBox normalize=new CheckBox(this);normalize.setText("LEVEL LOUD AND QUIET AUDIO");normalize.setTextColor(Color.WHITE);normalize.setButtonTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.red_player)));normalize.setChecked(prefs.getBoolean("vlc_normalize",false));normalize.setOnCheckedChangeListener((b,on)->prefs.edit().putBoolean("vlc_normalize",on).apply());root.addView(normalize);
  boostValue=text("",15,Color.WHITE);root.addView(boostValue);root.addView(seek("AUDIO BOOST",100,200,prefs.getInt("vlc_volume",100),(value)->{prefs.edit().putInt("vlc_volume",value).apply();boostValue.setText("AUDIO BOOST  "+value+"%");}));boostValue.setText("AUDIO BOOST  "+prefs.getInt("vlc_volume",100)+"%");
  audioDelayValue=text("",15,Color.WHITE);root.addView(audioDelayValue);root.addView(seek("AUDIO TIMING",0,10000,prefs.getInt("vlc_audio_delay",0)+5000,(value)->{int delay=value-5000;prefs.edit().putInt("vlc_audio_delay",delay).apply();audioDelayValue.setText("AUDIO TIMING  "+signed(delay)+" ms");}));audioDelayValue.setText("AUDIO TIMING  "+signed(prefs.getInt("vlc_audio_delay",0))+" ms");
  subtitleDelayValue=text("",15,Color.WHITE);root.addView(subtitleDelayValue);root.addView(seek("SUBTITLE TIMING",0,10000,prefs.getInt("vlc_subtitle_delay",0)+5000,(value)->{int delay=value-5000;prefs.edit().putInt("vlc_subtitle_delay",delay).apply();subtitleDelayValue.setText("SUBTITLE TIMING  "+signed(delay)+" ms");}));subtitleDelayValue.setText("SUBTITLE TIMING  "+signed(prefs.getInt("vlc_subtitle_delay",0))+" ms");
  cacheValue=text("",15,Color.WHITE);root.addView(cacheValue);root.addView(seek("NETWORK BUFFER",0,60000,prefs.getInt("vlc_network_cache",1500),(value)->{prefs.edit().putInt("vlc_network_cache",value).apply();cacheValue.setText("NETWORK BUFFER  "+value+" ms");}));cacheValue.setText("NETWORK BUFFER  "+prefs.getInt("vlc_network_cache",1500)+" ms");
  deinterlaceValue=text("",15,Color.WHITE);root.addView(deinterlaceValue);Button deinterlace=button("CHANGE VIDEO DEINTERLACING");deinterlace.setOnClickListener(v->{int next=(prefs.getInt("vlc_deinterlace",0)+1)%DEINTERLACE.length;prefs.edit().putInt("vlc_deinterlace",next).apply();deinterlaceValue.setText("VIDEO DEINTERLACING  "+DEINTERLACE[next]);});root.addView(deinterlace);deinterlaceValue.setText("VIDEO DEINTERLACING  "+DEINTERLACE[prefs.getInt("vlc_deinterlace",0)]);
  encodingValue=text("",15,Color.WHITE);root.addView(encodingValue);Button encoding=button("CHANGE SUBTITLE TEXT ENCODING");encoding.setOnClickListener(v->{int next=(prefs.getInt("subtitle_encoding",0)+1)%ENCODING.length;prefs.edit().putInt("subtitle_encoding",next).apply();encodingValue.setText("SUBTITLE TEXT  "+ENCODING[next]);});root.addView(encoding);encodingValue.setText("SUBTITLE TEXT  "+ENCODING[prefs.getInt("subtitle_encoding",0)]);
  Button reset=button("RESET PLAYBACK LAB");reset.setOnClickListener(v->{prefs.edit().remove("vlc_normalize").remove("vlc_volume").remove("vlc_audio_delay").remove("vlc_subtitle_delay").remove("vlc_network_cache").remove("vlc_deinterlace").remove("subtitle_encoding").apply();Toast.makeText(this,"Playback Lab reset",Toast.LENGTH_SHORT).show();recreate();});root.addView(reset);return root;
 }
 private SeekBar seek(String name,int min,int max,int value,ValueChange change){SeekBar s=new SeekBar(this);s.setMin(min);s.setMax(max);s.setProgress(Math.max(min,Math.min(max,value)));android.content.res.ColorStateList red=android.content.res.ColorStateList.valueOf(getColor(R.color.red_player));s.setProgressTintList(red);s.setThumbTintList(red);s.setContentDescription(name);s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){if(user)change.onChange(p);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});return s;}
 private Button button(String label){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setBackgroundColor(getColor(R.color.carbon));b.setMinHeight(dp(48));return b;}
 private TextView text(String value,int size,int color){TextView t=new ChromePulseTextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(5),0,dp(5));return t;}
 private String signed(int value){return value>0?"+"+value:String.valueOf(value);}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}private interface ValueChange{void onChange(int value);}
}
