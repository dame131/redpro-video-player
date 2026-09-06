package com.mr131.redplayer;

import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {
    private PlayerView playerView;
    private ExoPlayer player;
    private TextView titleText;
    private LinearLayout bottomBar;
    private final ArrayList<Uri> videos = new ArrayList<>();
    private final ArrayList<String> names = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private int current = -1;
    private int fitMode = 0;
    private float speed = 1f;
    private long pointA = C.TIME_UNSET, pointB = C.TIME_UNSET;
    private boolean controlsLocked = false;
    private boolean gesturesEnabled = true;
    private boolean resumeEnabled = true;
    private TextView gestureOverlay;

    private final ActivityResultLauncher<String[]> videoPicker = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(), this::addVideos);
    private final ActivityResultLauncher<String[]> subtitlePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::addSubtitle);
    private final ActivityResultLauncher<Intent> libraryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    addVideos(Collections.singletonList(result.getData().getData()));
                }
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("red_player", MODE_PRIVATE);
        playerView = findViewById(R.id.playerView);
        titleText = findViewById(R.id.titleText);
        bottomBar = findViewById(R.id.bottomBar);
        gestureOverlay = findViewById(R.id.gestureOverlay);
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        restoreSettings();
        wireButtons();
        wirePlayer();
        wireGestures();
        handleIncomingVideo(getIntent());
    }

    private void wireButtons() {
        findViewById(R.id.openButton).setOnClickListener(v -> libraryLauncher.launch(new Intent(this, LibraryActivity.class)));
        findViewById(R.id.subtitleButton).setOnClickListener(v -> {
            if (current < 0) toast("Open a video first");
            else subtitlePicker.launch(new String[]{"text/*", "application/x-subrip", "text/vtt"});
        });
        findViewById(R.id.speedButton).setOnClickListener(this::showSpeed);
        findViewById(R.id.fitButton).setOnClickListener(v -> changeFit());
        findViewById(R.id.abButton).setOnClickListener(this::setAB);
        findViewById(R.id.pipButton).setOnClickListener(v -> enterPip());
        findViewById(R.id.lockButton).setOnClickListener(v -> toggleLock());
        findViewById(R.id.infoButton).setOnClickListener(v -> showInfo());
        findViewById(R.id.previousButton).setOnClickListener(v -> playIndex(current - 1, true));
        findViewById(R.id.nextButton).setOnClickListener(v -> playIndex(current + 1, true));
        findViewById(R.id.playlistButton).setOnClickListener(v -> showPlaylist(""));
        findViewById(R.id.settingsButton).setOnClickListener(this::showSettings);
        findViewById(R.id.searchButton).setOnClickListener(v -> showSearch());
        findViewById(R.id.moreButton).setOnClickListener(this::showMore);
    }

    private void wirePlayer() {
        player.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                if (!isPlaying) savePosition();
            }
            @Override public void onMediaItemTransition(MediaItem item, int reason) {
                current = player.getCurrentMediaItemIndex();
                if (current >= 0 && current < names.size()) titleText.setText(names.get(current));
            }
            @Override public void onPlayerError(@NonNull PlaybackException error) {
                new AlertDialog.Builder(MainActivity.this).setTitle("Unsupported video")
                        .setMessage("This file could not be played. Try another file or open it in VLC.")
                        .setPositiveButton("OK", null).show();
            }
        });
        handler.post(abLoop);
    }

    private final Runnable abLoop = new Runnable() {
        @Override public void run() {
            if (pointA != C.TIME_UNSET && pointB != C.TIME_UNSET && player.getCurrentPosition() >= pointB) {
                player.seekTo(pointA);
            }
            handler.postDelayed(this, 150);
        }
    };

    private void addVideos(List<Uri> result) {
        if (result == null || result.isEmpty()) return;
        for (Uri uri : result) {
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
            if (!videos.contains(uri)) { videos.add(uri); names.add(displayName(uri)); }
        }
        rebuildPlaylist();
        if (current < 0) playIndex(0, false);
        toast(result.size() + " video" + (result.size() == 1 ? "" : "s") + " added");
    }

    private void rebuildPlaylist() {
        long oldPosition = player.getCurrentPosition();
        int oldIndex = current;
        ArrayList<MediaItem> media = new ArrayList<>();
        for (int i = 0; i < videos.size(); i++) {
            media.add(new MediaItem.Builder().setUri(videos.get(i)).setMediaId(videos.get(i).toString()).setTag(names.get(i)).build());
        }
        player.setMediaItems(media, Math.max(0, oldIndex), Math.max(0, oldPosition));
        player.prepare();
    }

    private void playIndex(int index, boolean play) {
        if (videos.isEmpty()) { toast("Add videos first"); return; }
        if (index < 0) index = videos.size() - 1;
        if (index >= videos.size()) index = 0;
        current = index;
        player.seekTo(index, resumeEnabled ? savedPosition(videos.get(index)) : 0);
        player.prepare();
        if (play) player.play();
        titleText.setText(names.get(index));
    }

    private void addSubtitle(Uri uri) {
        if (uri == null || current < 0) return;
        String name = displayName(uri).toLowerCase(Locale.US);
        String mime = name.endsWith(".vtt") ? MimeTypes.TEXT_VTT : MimeTypes.APPLICATION_SUBRIP;
        MediaItem.SubtitleConfiguration sub = new MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(mime).setLanguage("en").setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build();
        MediaItem item = new MediaItem.Builder().setUri(videos.get(current))
                .setSubtitleConfigurations(Collections.singletonList(sub)).setTag(names.get(current)).build();
        long pos = player.getCurrentPosition();
        player.replaceMediaItem(current, item);
        player.seekTo(current, pos);
        player.play();
        toast("Subtitles added");
    }

    private void showSpeed(View anchor) {
        final String[] labels = {"0.25×","0.5×","0.75×","1×","1.25×","1.5×","1.75×","2×","3×"};
        final float[] values = {.25f,.5f,.75f,1f,1.25f,1.5f,1.75f,2f,3f};
        new AlertDialog.Builder(this).setTitle("Playback speed").setSingleChoiceItems(labels, indexOf(values, speed), (d, which) -> {
            speed = values[which]; player.setPlaybackSpeed(speed); prefs.edit().putFloat("speed", speed).apply(); ((Button) anchor).setText("SPEED " + labels[which]); d.dismiss();
        }).show();
    }

    private int indexOf(float[] values, float value) { for (int i=0;i<values.length;i++) if(values[i]==value)return i; return 3; }

    private void changeFit() {
        int[] modes = {AspectRatioFrameLayout.RESIZE_MODE_FIT, AspectRatioFrameLayout.RESIZE_MODE_ZOOM, AspectRatioFrameLayout.RESIZE_MODE_FILL};
        String[] labels = {"Fit inside", "Fill screen", "Stretch"};
        fitMode = (fitMode + 1) % modes.length;
        playerView.setResizeMode(modes[fitMode]);
        toast(labels[fitMode]);
    }

    private void setAB(View view) {
        Button button = (Button) view;
        if (current < 0) { toast("Open a video first"); return; }
        if (pointA == C.TIME_UNSET) { pointA = player.getCurrentPosition(); button.setText("SET B"); toast("Point A saved"); }
        else if (pointB == C.TIME_UNSET) { pointB = Math.max(pointA + 250, player.getCurrentPosition()); button.setText("A–B ON"); toast("A–B repeat on"); }
        else { pointA = pointB = C.TIME_UNSET; button.setText("A–B"); toast("A–B repeat off"); }
    }

    private void enterPip() {
        if (current < 0) { toast("Open a video first"); return; }
        Rect rect = new Rect(); playerView.getGlobalVisibleRect(rect);
        PictureInPictureParams params = new PictureInPictureParams.Builder().setAspectRatio(new Rational(16,9)).setSourceRectHint(rect).build();
        enterPictureInPictureMode(params);
    }

    private void toggleLock() {
        controlsLocked = !controlsLocked;
        playerView.setUseController(!controlsLocked);
        findViewById(R.id.lockButton).setAlpha(controlsLocked ? .45f : 1f);
        toast(controlsLocked ? "Controls locked. Hold video to unlock." : "Controls unlocked");
    }

    private void wireGestures() {
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        GestureDetector detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(@NonNull MotionEvent e) { return true; }
            @Override public void onLongPress(@NonNull MotionEvent e) { if (controlsLocked) toggleLock(); }
            @Override public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (!controlsLocked) playerView.showController();
                return true;
            }
            @Override public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (!gesturesEnabled || controlsLocked) return false;
                float third = playerView.getWidth() / 3f;
                if (e.getX() < third) {
                    player.seekTo(Math.max(0, player.getCurrentPosition() - 10_000));
                    showGesture("↶ 10 seconds");
                } else if (e.getX() > third * 2) {
                    player.seekTo(Math.min(player.getDuration(), player.getCurrentPosition() + 10_000));
                    showGesture("10 seconds ↷");
                } else {
                    if (player.isPlaying()) player.pause(); else player.play();
                    showGesture(player.isPlaying() ? "▶ Play" : "Ⅱ Pause");
                }
                return true;
            }
            @Override public boolean onScroll(MotionEvent first, @NonNull MotionEvent now, float dx, float dy) {
                if (first == null || controlsLocked || !gesturesEnabled) return false;
                float adx = now.getX() - first.getX(), ady = now.getY() - first.getY();
                if (Math.abs(adx) > Math.abs(ady)) {
                    if (Math.abs(dx) > 2) {
                        long next = Math.max(0, Math.min(player.getDuration(), player.getCurrentPosition() - (long) dx * 35));
                        player.seekTo(next); showGesture(formatTime(next));
                    }
                } else if (first.getX() < playerView.getWidth()/2f) {
                    WindowManager.LayoutParams p = getWindow().getAttributes();
                    float base = p.screenBrightness < 0 ? .5f : p.screenBrightness;
                    p.screenBrightness = Math.max(.05f, Math.min(1f, base - dy/playerView.getHeight()));
                    getWindow().setAttributes(p);
                    showGesture("Brightness " + Math.round(p.screenBrightness * 100) + "%");
                } else {
                    int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int step = dy < 0 ? 1 : -1;
                    int level = Math.max(0, Math.min(max, audio.getStreamVolume(AudioManager.STREAM_MUSIC)+step));
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0);
                    showGesture("Volume " + Math.round(level * 100f / max) + "%");
                }
                return true;
            }
        });
        playerView.setOnTouchListener((view, event) -> detector.onTouchEvent(event));
    }

    private void showPlaylist(String filter) {
        ArrayList<Integer> indexes = new ArrayList<>(); ArrayList<String> shown = new ArrayList<>();
        for (int i=0;i<names.size();i++) if(names.get(i).toLowerCase(Locale.US).contains(filter.toLowerCase(Locale.US))){indexes.add(i);shown.add((i==current?"▶ ":"")+names.get(i));}
        if (shown.isEmpty()) { toast("No matching videos"); return; }
        new AlertDialog.Builder(this).setTitle(filter.isEmpty()?"Playlist":"Search results").setItems(shown.toArray(new String[0]),(d,w)->playIndex(indexes.get(w),true)).setNegativeButton("Close",null).show();
    }

    private void showSearch() {
        EditText input = new EditText(this); input.setHint("Video name");
        new AlertDialog.Builder(this).setTitle("Search videos").setView(input).setPositiveButton("Search",(d,w)->showPlaylist(input.getText().toString())).setNegativeButton("Cancel",null).show();
    }

    private void showMore(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Audio tracks"); menu.getMenu().add("Private vault"); menu.getMenu().add("Rotate screen"); menu.getMenu().add("Sleep timer");
        menu.setOnMenuItemClickListener(item -> {
            String title=item.getTitle().toString();
            if(title.equals("Audio tracks"))showAudioTracks();
            if(title.equals("Private vault"))unlockVault();
            if(title.equals("Rotate screen"))setRequestedOrientation(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            if(title.equals("Sleep timer"))showSleepTimer();
            return true;
        }); menu.show();
    }

    private void showAudioTracks() {
        Tracks tracks=player.getCurrentTracks(); ArrayList<Tracks.Group> groups=new ArrayList<>(); ArrayList<String> labels=new ArrayList<>();
        for(Tracks.Group g:tracks.getGroups())if(g.getType()==C.TRACK_TYPE_AUDIO)for(int i=0;i<g.length;i++){groups.add(g);String lang=g.getTrackFormat(i).language;labels.add("Audio "+(labels.size()+1)+(lang==null?"":" • "+lang));}
        if(labels.isEmpty()){toast("No extra audio tracks found");return;}
        new AlertDialog.Builder(this).setTitle("Audio tracks").setItems(labels.toArray(new String[0]),(d,w)->{Tracks.Group g=groups.get(w);player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setOverrideForType(new TrackSelectionOverride(g.getMediaTrackGroup(),0)).build());}).show();
    }

    private void unlockVault() {
        BiometricManager manager=BiometricManager.from(this);
        if(manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG|BiometricManager.Authenticators.DEVICE_CREDENTIAL)!=BiometricManager.BIOMETRIC_SUCCESS){toast("Set up fingerprint or phone lock first");return;}
        Executor executor=ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt=new BiometricPrompt(this,executor,new BiometricPrompt.AuthenticationCallback(){@Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult r){toast("Private vault unlocked");videoPicker.launch(new String[]{"video/*"});}});
        prompt.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle("131 Private Vault").setSubtitle("Unlock your private videos").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG|BiometricManager.Authenticators.DEVICE_CREDENTIAL).build());
    }

    private void showSleepTimer() {
        String[] choices={"15 minutes","30 minutes","1 hour","2 hours"};
        int[] mins={15,30,60,120};
        new AlertDialog.Builder(this).setTitle("Sleep timer").setItems(choices,(d,w)->{handler.postDelayed(()->{player.pause();toast("Sleep timer stopped playback");},mins[w]*60000L);toast("Sleep timer set");}).show();
    }

    private void showSettings(View view) {
        String[] options={"Repeat playlist","Shuffle videos","Keep screen awake","Resume where I stopped","Swipe and double-tap controls"};
        boolean[] checked={player.getRepeatMode()!=Player.REPEAT_MODE_OFF,player.getShuffleModeEnabled(),prefs.getBoolean("keep_awake",false),resumeEnabled,gesturesEnabled};
        new AlertDialog.Builder(this).setTitle("Settings").setMultiChoiceItems(options,checked,(d,w,on)->{
            if(w==0){player.setRepeatMode(on?Player.REPEAT_MODE_ALL:Player.REPEAT_MODE_OFF);prefs.edit().putBoolean("repeat",on).apply();}
            if(w==1){player.setShuffleModeEnabled(on);prefs.edit().putBoolean("shuffle",on).apply();}
            if(w==2){prefs.edit().putBoolean("keep_awake",on).apply();applyKeepAwake(on);}
            if(w==3){resumeEnabled=on;prefs.edit().putBoolean("resume",on).apply();}
            if(w==4){gesturesEnabled=on;prefs.edit().putBoolean("gestures",on).apply();}
        }).setNeutralButton("Clear history",(d,w)->{clearHistoryOnly();toast("Playback history cleared");}).setPositiveButton("Done",null).show();
    }

    private void restoreSettings() {
        speed = prefs.getFloat("speed", 1f);
        resumeEnabled = prefs.getBoolean("resume", true);
        gesturesEnabled = prefs.getBoolean("gestures", true);
        player.setPlaybackSpeed(speed);
        player.setRepeatMode(prefs.getBoolean("repeat", false) ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF);
        player.setShuffleModeEnabled(prefs.getBoolean("shuffle", false));
        applyKeepAwake(prefs.getBoolean("keep_awake", false));
        ((Button)findViewById(R.id.speedButton)).setText("SPEED " + speed + "×");
    }

    private void applyKeepAwake(boolean on) {
        if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void clearHistoryOnly() {
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) if (key.startsWith("pos:")) editor.remove(key);
        editor.apply();
    }

    private void showGesture(String text) {
        gestureOverlay.setText(text); gestureOverlay.setVisibility(View.VISIBLE); gestureOverlay.setAlpha(1f);
        gestureOverlay.animate().alpha(0f).setStartDelay(650).setDuration(300).withEndAction(() -> gestureOverlay.setVisibility(View.GONE)).start();
    }

    private void showInfo() {
        if(current<0){toast("Open a video first");return;}
        androidx.media3.common.Format f=player.getVideoFormat();
        String details=names.get(current)+"\n\nDuration: "+formatTime(player.getDuration())+"\nResolution: "+(f==null?"Unknown":f.width+" × "+f.height)+"\nVideo codec: "+(f==null||f.codecs==null?"Unknown":f.codecs)+"\nLocation: "+videos.get(current);
        new AlertDialog.Builder(this).setTitle("Video information").setMessage(details).setPositiveButton("OK",null).show();
    }

    private String displayName(Uri uri) {
        try (android.database.Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())return c.getString(0);}catch(Exception ignored){}
        return uri.getLastPathSegment()==null?"Video":uri.getLastPathSegment();
    }
    private String formatTime(long ms){if(ms<0)return"Unknown";long s=ms/1000;return String.format(Locale.US,"%d:%02d:%02d",s/3600,(s/60)%60,s%60);}
    private long savedPosition(Uri uri){return prefs.getLong("pos:"+uri,0);}
    private void savePosition(){if(current>=0&&current<videos.size())prefs.edit().putLong("pos:"+videos.get(current),player.getCurrentPosition()).apply();}
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_SHORT).show();}
    private void handleIncomingVideo(Intent intent){if(intent!=null&&Intent.ACTION_VIEW.equals(intent.getAction())&&intent.getData()!=null)addVideos(Collections.singletonList(intent.getData()));}

    @Override public void onPictureInPictureModeChanged(boolean inPip,@NonNull Configuration config){super.onPictureInPictureModeChanged(inPip,config);bottomBar.setVisibility(inPip?View.GONE:View.VISIBLE);}
    @Override protected void onStop(){super.onStop();if(!isInPictureInPictureMode())savePosition();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);player.release();super.onDestroy();}
}
