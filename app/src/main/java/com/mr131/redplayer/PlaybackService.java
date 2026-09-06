package com.mr131.redplayer;

import android.content.Intent;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import java.util.ArrayList;
import java.util.List;

public final class PlaybackService extends MediaSessionService {
    private static ExoPlayer player;
    private MediaSession session;
    private static Equalizer equalizer;
    private static BassBoost bassBoost;
    private static PlaybackService instance;
    private boolean softwareDecoder;

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        player = buildPlayer(false);
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true);
        session = new MediaSession.Builder(this, player).build();
    }

    public static ExoPlayer player() { return player; }

    private ExoPlayer buildPlayer(boolean software) {
        DefaultRenderersFactory renderers=new DefaultRenderersFactory(this).setEnableDecoderFallback(true);
        if(software) renderers.setMediaCodecSelector((mime,secure,tunneling)->{
            List<MediaCodecInfo> all=MediaCodecSelector.DEFAULT.getDecoderInfos(mime,secure,tunneling);ArrayList<MediaCodecInfo> preferred=new ArrayList<>();
            for(MediaCodecInfo info:all){String n=info.name.toLowerCase();if(n.contains("google")||n.contains("android")||n.contains("ffmpeg"))preferred.add(info);}
            return preferred.isEmpty()?all:preferred;
        });
        return new ExoPlayer.Builder(this,renderers).build();
    }

    public static void setSoftwareDecoder(boolean software) { if(instance!=null&&instance.softwareDecoder!=software)instance.rebuildPlayer(software); }

    private void rebuildPlayer(boolean software) {
        ArrayList<androidx.media3.common.MediaItem> items=new ArrayList<>();for(int i=0;i<player.getMediaItemCount();i++)items.add(player.getMediaItemAt(i));
        int index=player.getCurrentMediaItemIndex();long position=player.getCurrentPosition();boolean play=player.getPlayWhenReady();
        if(equalizer!=null){equalizer.release();equalizer=null;}if(bassBoost!=null){bassBoost.release();bassBoost=null;}if(session!=null)session.release();player.release();
        softwareDecoder=software;player=buildPlayer(software);player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
        if(!items.isEmpty()){player.setMediaItems(items,Math.max(0,index),Math.max(0,position));player.prepare();player.setPlayWhenReady(play);}session=new MediaSession.Builder(this,player).build();
    }

    public static void setEqualizer(boolean enabled, int levelPercent, int bassPercent) {
        if (player == null) return;
        int id = player.getAudioSessionId();
        if (id == C.AUDIO_SESSION_ID_UNSET) return;
        try {
            if (equalizer == null) equalizer = new Equalizer(0, id);
            if (bassBoost == null) bassBoost = new BassBoost(0, id);
            short[] range = equalizer.getBandLevelRange();
            short level = (short)(range[0] + (range[1] - range[0]) * Math.max(0, Math.min(100, levelPercent)) / 100);
            for (short band = 0; band < equalizer.getNumberOfBands(); band++) equalizer.setBandLevel(band, level);
            bassBoost.setStrength((short)(Math.max(0, Math.min(100, bassPercent)) * 10));
            equalizer.setEnabled(enabled); bassBoost.setEnabled(enabled);
        } catch (RuntimeException ignored) { }
    }

    @Nullable @Override public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) { return session; }

    @Override public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (equalizer != null) equalizer.release();
        if (bassBoost != null) bassBoost.release();
        if (session != null) session.release();
        if (player != null) player.release();
        equalizer = null; bassBoost = null; session = null; player = null;
        instance = null;
        super.onDestroy();
    }
}
