package com.mr131.redplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public final class PlaybackService extends MediaSessionService {
    private static ExoPlayer player;
    private MediaSession session;
    private static Equalizer equalizer;
    private static BassBoost bassBoost;
    private static PlaybackService instance;
    private boolean softwareDecoder;
    private SharedPreferences resumeStore;
    private final Handler resumeHandler = new Handler(Looper.getMainLooper());
    private final Runnable saveProgress = new Runnable() {
        @Override public void run() {
            saveQueue();
            resumeHandler.postDelayed(this, 10_000L);
        }
    };
    private final Player.Listener resumeListener = new Player.Listener() {
        @Override public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) { saveQueue(); }
        @Override public void onIsPlayingChanged(boolean isPlaying) { saveQueue(); }
        @Override public void onPlaybackStateChanged(int playbackState) { saveQueue(); }
    };

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        resumeStore = getSharedPreferences("playback_service_resume", MODE_PRIVATE);
        player = buildPlayer(false);
        configurePlayer(player);
        restoreQueue();
        session = new MediaSession.Builder(this, player).build();
        resumeHandler.postDelayed(saveProgress, 10_000L);
    }

    public static ExoPlayer player() { return player; }

    private ExoPlayer buildPlayer(boolean software) {
        DefaultRenderersFactory renderers=new DefaultRenderersFactory(this).setEnableDecoderFallback(true);
        if(software) renderers.setMediaCodecSelector((mime,secure,tunneling)->{
            List<MediaCodecInfo> all=MediaCodecSelector.DEFAULT.getDecoderInfos(mime,secure,tunneling);ArrayList<MediaCodecInfo> preferred=new ArrayList<>();
            for(MediaCodecInfo info:all){String n=info.name.toLowerCase();if(n.contains("google")||n.contains("android")||n.contains("ffmpeg"))preferred.add(info);}
            return preferred.isEmpty()?all:preferred;
        });
        return new ExoPlayer.Builder(this,renderers)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build();
    }

    private void configurePlayer(ExoPlayer target) {
        target.addListener(resumeListener);
    }

    public static void setSoftwareDecoder(boolean software) { if(instance!=null&&instance.softwareDecoder!=software)instance.rebuildPlayer(software); }

    private void rebuildPlayer(boolean software) {
        ArrayList<androidx.media3.common.MediaItem> items=new ArrayList<>();for(int i=0;i<player.getMediaItemCount();i++)items.add(player.getMediaItemAt(i));
        int index=player.getCurrentMediaItemIndex();long position=player.getCurrentPosition();boolean play=player.getPlayWhenReady();
        if(equalizer!=null){equalizer.release();equalizer=null;}if(bassBoost!=null){bassBoost.release();bassBoost=null;}if(session!=null)session.release();player.release();
        softwareDecoder=software;player=buildPlayer(software);configurePlayer(player);
        if(!items.isEmpty()){player.setMediaItems(items,Math.max(0,index),Math.max(0,position));player.prepare();player.setPlayWhenReady(play);}session=new MediaSession.Builder(this,player).build();
    }

    /** Saves a bounded queue so a killed playback service can resume without auto-playing. */
    private void saveQueue() {
        if (player == null || resumeStore == null || player.getMediaItemCount() == 0) return;
        try {
            JSONArray queue = new JSONArray();
            int count = Math.min(player.getMediaItemCount(), 500);
            for (int i = 0; i < count; i++) {
                MediaItem item = player.getMediaItemAt(i);
                if (item.localConfiguration == null || item.localConfiguration.uri == null) continue;
                JSONObject saved = new JSONObject();
                saved.put("uri", item.localConfiguration.uri.toString());
                put(saved, "title", item.mediaMetadata.title);
                put(saved, "artist", item.mediaMetadata.artist);
                put(saved, "album", item.mediaMetadata.albumTitle);
                if (item.mediaMetadata.artworkUri != null) saved.put("art", item.mediaMetadata.artworkUri.toString());
                queue.put(saved);
            }
            if (queue.length() == 0) return;
            resumeStore.edit()
                    .putString("queue", queue.toString())
                    .putInt("index", Math.max(0, player.getCurrentMediaItemIndex()))
                    .putLong("position", Math.max(0, player.getCurrentPosition()))
                    .apply();
        } catch (Exception ignored) { }
    }

    private void put(JSONObject object, String key, @Nullable CharSequence value) {
        if (value == null) return;
        try { object.put(key, value.toString()); } catch (Exception ignored) { }
    }

    private void restoreQueue() {
        String stored = resumeStore.getString("queue", "");
        if (stored == null || stored.isEmpty()) return;
        try {
            JSONArray queue = new JSONArray(stored);
            ArrayList<MediaItem> items = new ArrayList<>();
            for (int i = 0; i < queue.length(); i++) {
                JSONObject saved = queue.getJSONObject(i);
                String value = saved.optString("uri", "");
                if (value.isEmpty()) continue;
                Uri uri = Uri.parse(value);
                MediaMetadata.Builder metadata = new MediaMetadata.Builder();
                if (saved.has("title")) metadata.setTitle(saved.optString("title"));
                if (saved.has("artist")) metadata.setArtist(saved.optString("artist"));
                if (saved.has("album")) metadata.setAlbumTitle(saved.optString("album"));
                String art = saved.optString("art", "");
                if (!art.isEmpty()) metadata.setArtworkUri(Uri.parse(art));
                items.add(new MediaItem.Builder().setUri(uri).setMediaId(value)
                        .setMediaMetadata(metadata.build()).build());
            }
            if (items.isEmpty()) return;
            int index = Math.min(Math.max(0, resumeStore.getInt("index", 0)), items.size() - 1);
            long position = Math.max(0, resumeStore.getLong("position", 0));
            player.setMediaItems(items, index, position);
            player.prepare();
            player.setPlayWhenReady(false);
        } catch (Exception ignored) {
            resumeStore.edit().clear().apply();
        }
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

    @Override public void onTaskRemoved(@Nullable Intent rootIntent) {
        saveQueue();
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy() {
        resumeHandler.removeCallbacks(saveProgress);
        saveQueue();
        if (equalizer != null) equalizer.release();
        if (bassBoost != null) bassBoost.release();
        if (session != null) session.release();
        if (player != null) player.release();
        equalizer = null; bassBoost = null; session = null; player = null;
        instance = null;
        super.onDestroy();
    }
}
