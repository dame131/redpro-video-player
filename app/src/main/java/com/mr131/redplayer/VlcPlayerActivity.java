package com.mr131.redplayer;

import android.app.PictureInPictureParams;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.content.SharedPreferences;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.Locale;

/** VLC-powered fallback player for formats not supported by the phone codec. */
public final class VlcPlayerActivity extends AppCompatActivity {
    private LibVLC vlc;
    private MediaPlayer player;
    private ParcelFileDescriptor sourceFd;
    private VLCVideoLayout video;
    private SeekBar seek;
    private TextView time;
    private LinearLayout controls;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Uri uri=getIntent().getData();
        if(uri==null){finish();return;}
        buildScreen(getIntent().getStringExtra("title"));
        SharedPreferences prefs=getSharedPreferences("red_player",MODE_PRIVATE);
        ArrayList<String> options=new ArrayList<>();
        options.add("--network-caching="+prefs.getInt("vlc_network_cache",1500));options.add("--clock-jitter=0");options.add("--clock-synchro=0");
        if(prefs.getBoolean("vlc_normalize",false))options.add("--audio-filter=normvol");
        vlc=new LibVLC(this,options);player=new MediaPlayer(vlc);player.attachViews(video,null,true,false);
        Media media;
        try{
            String scheme=uri.getScheme();
            if("content".equalsIgnoreCase(scheme)){sourceFd=getContentResolver().openFileDescriptor(uri,"r");if(sourceFd==null)throw new Exception("File is unavailable");media=new Media(vlc,sourceFd.getFileDescriptor());}
            else media=new Media(vlc,uri);
        }catch(Exception error){Toast.makeText(this,"VLC could not open this file",Toast.LENGTH_LONG).show();finish();return;}
        media.setHWDecoderEnabled(false,false);media.addOption(":codec=all");
        String[] encodings={"","UTF-8","Windows-1252","ISO-8859-1","UTF-16"};int encoding=prefs.getInt("subtitle_encoding",0);if(encoding>0&&encoding<encodings.length)media.addOption(":subsdec-encoding="+encodings[encoding]);
        player.setMedia(media);media.release();
        player.setEventListener(event->{if(event.type==MediaPlayer.Event.EncounteredError)runOnUiThread(()->Toast.makeText(this,"VLC could not decode this file",Toast.LENGTH_LONG).show());if(event.type==MediaPlayer.Event.EndReached)runOnUiThread(this::finish);});
        player.play();player.setVolume(prefs.getInt("vlc_volume",100));player.setAudioDelay(prefs.getInt("vlc_audio_delay",0)*1000L);player.setSpuDelay(prefs.getInt("vlc_subtitle_delay",0)*1000L);String[] deinterlace={"","auto","yadif","blend"};int di=prefs.getInt("vlc_deinterlace",0);if(di>0&&di<deinterlace.length)player.setDeinterlace(deinterlace[di]);long start=getIntent().getLongExtra("position",0);if(start>0)handler.postDelayed(()->player.setTime(start),500);handler.post(progress);
    }

    private void buildScreen(String title){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.BLACK);root.setContentDescription("VLC Codec Player Ready");
        TextView heading=new ChromePulseTextView(this);heading.setText((title==null?"VIDEO":title)+"  •  VLC CODEC");heading.setTextSize(17);heading.setTextColor(Color.WHITE);heading.setGravity(Gravity.CENTER_VERTICAL);heading.setPadding(dp(16),0,dp(16),0);root.addView(heading,new LinearLayout.LayoutParams(-1,dp(54)));
        FrameLayout stage=new FrameLayout(this);video=new VLCVideoLayout(this);stage.addView(video,new FrameLayout.LayoutParams(-1,-1));controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);controls.setOrientation(LinearLayout.HORIZONTAL);
        ChromeIconButton back=icon(R.drawable.icon_rewind_10_thick,"Rewind 10 seconds");back.setOnClickListener(v->player.setTime(Math.max(0,player.getTime()-10000)));
        ChromeIconButton play=icon(R.drawable.icon_play_pause_thick,"Play or pause");play.setOnClickListener(v->{if(player.isPlaying())player.pause();else player.play();});
        ChromeIconButton next=icon(R.drawable.icon_forward_10_thick,"Forward 10 seconds");next.setOnClickListener(v->player.setTime(Math.min(player.getLength(),player.getTime()+10000)));
        ChromeIconButton pip=icon(R.drawable.icon_pip_thick,"Picture in picture");pip.setOnClickListener(v->enterPictureInPictureMode(new PictureInPictureParams.Builder().setAspectRatio(new Rational(16,9)).build()));
        controls.addView(back);controls.addView(play);controls.addView(next);controls.addView(pip);stage.addView(controls,new FrameLayout.LayoutParams(-1,dp(86),Gravity.CENTER));root.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout timeline=new LinearLayout(this);timeline.setGravity(Gravity.CENTER_VERTICAL);timeline.setPadding(dp(12),0,dp(12),0);seek=new SeekBar(this);seek.setMax(1000);seek.getProgressDrawable().setTint(getColor(R.color.red_player));seek.getThumb().setTint(getColor(R.color.red_player));timeline.addView(seek,new LinearLayout.LayoutParams(0,dp(48),1));time=new ChromePulseTextView(this);time.setText("0:00 / 0:00");time.setTextColor(Color.WHITE);timeline.addView(time,new LinearLayout.LayoutParams(dp(118),dp(48)));root.addView(timeline);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){if(user&&player!=null&&player.getLength()>0)player.setTime(player.getLength()*p/1000);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        setContentView(root);
    }

    private ChromeIconButton icon(int image,String description){ChromeIconButton b=new ChromeIconButton(this);b.setImageResource(image);b.setTag("raw_asset");b.setContentDescription(description);b.setBackgroundColor(Color.TRANSPARENT);b.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);b.setPadding(dp(6),dp(6),dp(6),dp(6));b.setLayoutParams(new LinearLayout.LayoutParams(dp(76),dp(76)));return b;}
    private final Runnable progress=new Runnable(){public void run(){if(player!=null){long length=Math.max(0,player.getLength()),now=Math.max(0,player.getTime());seek.setProgress(length>0?(int)(now*1000/length):0);time.setText(format(now)+" / "+format(length));handler.postDelayed(this,500);}}};
    private String format(long ms){long s=ms/1000;return String.format(Locale.US,"%d:%02d",s/60,s%60);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override public void onPictureInPictureModeChanged(boolean pip,android.content.res.Configuration c){super.onPictureInPictureModeChanged(pip,c);controls.setVisibility(pip?View.GONE:View.VISIBLE);}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(player!=null){player.stop();player.detachViews();player.release();}if(vlc!=null)vlc.release();if(sourceFd!=null)try{sourceFd.close();}catch(Exception ignored){}super.onDestroy();}
}
