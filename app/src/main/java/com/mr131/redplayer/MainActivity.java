package com.mr131.redplayer;

import androidx.appcompat.app.AlertDialog;
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
import android.view.ContextThemeWrapper;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.splashscreen.SplashScreen;
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
import androidx.media3.cast.CastPlayer;
import androidx.media3.cast.SessionAvailabilityListener;
import androidx.mediarouter.app.MediaRouteButton;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private PlayerView playerView;
    private Player player;
    private ExoPlayer localPlayer;
    private CastPlayer castPlayer;
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
    private Uri subtitleUri;
    private long subtitleOffsetMs = 0;
    private boolean softwareDecoder = false;

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
    private final ActivityResultLauncher<Intent> vaultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null)
                    addVideos(Collections.singletonList(result.getData().getData()));
            });
    private final ActivityResultLauncher<Intent> cloudLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null)
                    addVideos(Collections.singletonList(result.getData().getData()));
            });
    private final ActivityResultLauncher<Intent> downloadedSubtitleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {if(result.getResultCode()==RESULT_OK&&result.getData()!=null&&result.getData().getData()!=null)addSubtitle(result.getData().getData());});
    private final ActivityResultLauncher<Intent> historyLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {if(result.getResultCode()==RESULT_OK&&result.getData()!=null&&result.getData().getData()!=null)addVideos(Collections.singletonList(result.getData().getData()));});

    @Override protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.root).setContentDescription("131 Red Player Home Ready");
        prefs = getSharedPreferences("red_player", MODE_PRIVATE);
        playerView = findViewById(R.id.playerView);
        titleText = findViewById(R.id.titleText);
        bottomBar = findViewById(R.id.bottomBar);
        gestureOverlay = findViewById(R.id.gestureOverlay);
        wireButtons();
        startIconEntrance();
        wireGestures();
        startService(new Intent(this, PlaybackService.class));
        connectPlayer(0);
    }

    private void connectPlayer(int attempt) {
        localPlayer = PlaybackService.player();
        if (localPlayer == null) {
            if (attempt < 60) handler.postDelayed(() -> connectPlayer(attempt + 1), 50);
            else toast("Playback service could not start");
            return;
        }
        player = localPlayer; playerView.setPlayer(player); restoreSettings(); wirePlayer(); playerView.post(() -> setupCast()); handleIncomingVideo(getIntent());
    }

    private void wireButtons() {
        bindAnimated(R.id.openButton,v -> libraryLauncher.launch(new Intent(this, LibraryActivity.class)));
        bindAnimated(R.id.networkButton,v -> showNetworkStream());
        bindAnimated(R.id.cloudButton,v -> cloudLauncher.launch(new Intent(this, CloudImportActivity.class)));
        bindAnimated(R.id.decoderButton,this::toggleDecoder);
        bindAnimated(R.id.subtitleButton,v -> {if(current<0)toast("Open a video first");else showSubtitleChoices();});
        bindAnimated(R.id.speedButton,this::showSpeed);
        bindAnimated(R.id.fitButton,v -> changeFit());
        bindAnimated(R.id.abButton,this::setAB);
        bindAnimated(R.id.pipButton,v -> enterPip());
        bindAnimated(R.id.lockButton,v -> toggleLock());
        bindAnimated(R.id.infoButton,v -> showInfo());
        bindAnimated(R.id.previousButton,v -> playIndex(current - 1, true));
        bindAnimated(R.id.nextButton,v -> playIndex(current + 1, true));
        bindAnimated(R.id.playlistButton,v -> showPlaylist(""));
        bindAnimated(R.id.settingsButton,this::showSettings);
        bindAnimated(R.id.searchButton,v -> showSearch());
        bindAnimated(R.id.moreButton,this::showMore);
    }

    private void bindAnimated(int id, View.OnClickListener action) {
        View icon=findViewById(id);icon.setOnClickListener(v->{animateIcon(v);action.onClick(v);});
    }

    private void animateIcon(View icon) {
        icon.animate().cancel();icon.animate().scaleX(.72f).scaleY(.72f).rotationBy(18f).alpha(.6f).setDuration(90).withEndAction(()->icon.animate().scaleX(1.12f).scaleY(1.12f).rotation(0f).alpha(1f).setDuration(120).withEndAction(()->icon.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start()).start();
    }

    private void startIconEntrance() {
        int[] ids={R.id.searchButton,R.id.moreButton,R.id.networkButton,R.id.cloudButton,R.id.decoderButton,R.id.subtitleButton,R.id.speedButton,R.id.fitButton,R.id.abButton,R.id.pipButton,R.id.lockButton,R.id.infoButton,R.id.openButton,R.id.previousButton,R.id.playlistButton,R.id.nextButton,R.id.settingsButton};
        for(int i=0;i<ids.length;i++){View icon=findViewById(ids[i]);icon.setAlpha(0f);icon.setScaleX(.55f);icon.setScaleY(.55f);icon.setTranslationY(14f);icon.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setStartDelay(i*22L).setDuration(260).start();}
        findViewById(R.id.openButton).setActivated(true);
    }

    private void showSubtitleChoices(){new AlertDialog.Builder(this).setTitle("Subtitles").setItems(new String[]{"Download matching subtitles","Load SRT or VTT file","Adjust subtitle timing"},(d,w)->{if(w==0)downloadedSubtitleLauncher.launch(new Intent(this,SubtitleDownloadActivity.class).putExtra("video_name",names.get(current)));if(w==1)subtitlePicker.launch(new String[]{"text/*","application/x-subrip","text/vtt"});if(w==2)showSubtitleTiming();}).show();}

    private void showNetworkStream() {
        EditText input=new EditText(this);input.setHint("https://, http://, rtsp://, .m3u8, .mp4, .mkv");input.setSingleLine(true);input.setTextColor(android.graphics.Color.WHITE);input.setHintTextColor(android.graphics.Color.LTGRAY);input.setBackgroundColor(android.graphics.Color.rgb(34,34,34));input.setPadding(24,20,24,20);
        new AlertDialog.Builder(this).setTitle("Open Network Stream").setMessage("Paste a direct video or live-stream address.").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Play",(d,w)->{
            String value=input.getText().toString().trim();Uri uri=Uri.parse(value);String scheme=uri.getScheme();
            if(value.isEmpty()||scheme==null||!(scheme.equalsIgnoreCase("http")||scheme.equalsIgnoreCase("https")||scheme.equalsIgnoreCase("rtsp"))){toast("Use a valid HTTP, HTTPS, or RTSP address");return;}
            addVideos(Collections.singletonList(uri));playIndex(videos.size()-1,true);
        }).show();
    }

    private void toggleDecoder(View view) {
        softwareDecoder=!softwareDecoder;prefs.edit().putBoolean("software_decoder",softwareDecoder).apply();view.setActivated(softwareDecoder);view.setContentDescription(softwareDecoder?"Software decoder":"Hardware decoder");
        PlaybackService.setSoftwareDecoder(softwareDecoder);playerView.setPlayer(null);connectPlayer(0);toast((softwareDecoder?"Software":"Hardware")+" decoder selected");
    }

    private void setupCast() {
        try {
            CastContext context=CastContext.getSharedInstance(this);CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),(MediaRouteButton)findViewById(R.id.castButton));castPlayer=new CastPlayer(context);
            castPlayer.setSessionAvailabilityListener(new SessionAvailabilityListener(){
                @Override public void onCastSessionAvailable(){
                    if(current<0||current>=videos.size()){toast("Choose a video link first");return;}String scheme=videos.get(current).getScheme();
                    if(!"http".equalsIgnoreCase(scheme)&&!"https".equalsIgnoreCase(scheme)){toast("Casting needs a network or cloud-accessible link");return;}
                    long position=localPlayer.getCurrentPosition();boolean playing=localPlayer.getPlayWhenReady();localPlayer.pause();ArrayList<MediaItem> items=new ArrayList<>();for(int i=0;i<videos.size();i++)items.add(new MediaItem.Builder().setUri(videos.get(i)).setTag(names.get(i)).build());
                    castPlayer.setMediaItems(items,Math.max(0,current),Math.max(0,position));castPlayer.prepare();castPlayer.setPlayWhenReady(playing);player=castPlayer;playerView.setPlayer(player);toast("Casting to TV");
                }
                @Override public void onCastSessionUnavailable(){if(player==castPlayer){int index=castPlayer.getCurrentMediaItemIndex();long position=castPlayer.getCurrentPosition();boolean playing=castPlayer.getPlayWhenReady();castPlayer.stop();player=localPlayer;playerView.setPlayer(player);if(index>=0&&index<localPlayer.getMediaItemCount())localPlayer.seekTo(index,position);localPlayer.setPlayWhenReady(playing);toast("Playing on phone");}}
            });
        } catch(Exception e){findViewById(R.id.castButton).setVisibility(View.GONE);}
    }

    private void wirePlayer() {
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if(state==Player.STATE_READY) playerView.setContentDescription("Video Ready");
            }
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                if (!isPlaying) savePosition();
            }
            @Override public void onMediaItemTransition(MediaItem item, int reason) {
                current = player.getCurrentMediaItemIndex();
                if (current >= 0 && current < names.size()) titleText.setText(names.get(current));
            }
            @Override public void onPlayerError(@NonNull PlaybackException error) {
                prefs.edit().putString("last_error",error.getErrorCodeName()+": "+error.getMessage()).apply();
                new AlertDialog.Builder(MainActivity.this).setTitle("Playback recovery").setMessage("The video stopped: "+error.getErrorCodeName()+"\n\nRetry, change decoder, or skip this file.").setPositiveButton("Retry",(d,w)->{player.prepare();player.play();}).setNeutralButton("Change decoder",(d,w)->toggleDecoder(findViewById(R.id.decoderButton))).setNegativeButton("Next video",(d,w)->playIndex(current+1,true)).show();
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
        subtitleUri = uri; subtitleOffsetMs = prefs.getLong("subtitle_offset", 0); applySubtitle();
    }

    private void applySubtitle() {
        if (subtitleUri == null || current < 0) return;
        Uri playableSubtitle = shiftSrt(subtitleUri, subtitleOffsetMs);
        String name = displayName(subtitleUri).toLowerCase(Locale.US);
        String mime = name.endsWith(".vtt") ? MimeTypes.TEXT_VTT : MimeTypes.APPLICATION_SUBRIP;
        MediaItem.SubtitleConfiguration sub = new MediaItem.SubtitleConfiguration.Builder(playableSubtitle)
                .setMimeType(mime).setLanguage("en").setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build();
        MediaItem item = new MediaItem.Builder().setUri(videos.get(current))
                .setSubtitleConfigurations(Collections.singletonList(sub)).setTag(names.get(current)).build();
        long pos = player.getCurrentPosition();
        player.replaceMediaItem(current, item);
        player.seekTo(current, pos);
        player.play();
        toast("Subtitles added • offset " + (subtitleOffsetMs / 1000f) + "s");
    }

    private void showSpeed(View anchor) {
        final String[] labels = {"0.25×","0.5×","0.75×","1×","1.25×","1.5×","1.75×","2×","3×"};
        final float[] values = {.25f,.5f,.75f,1f,1.25f,1.5f,1.75f,2f,3f};
        new AlertDialog.Builder(this).setTitle("Playback speed").setSingleChoiceItems(labels, indexOf(values, speed), (d, which) -> {
            speed = values[which]; player.setPlaybackSpeed(speed); prefs.edit().putFloat("speed", speed).apply(); anchor.setContentDescription("Playback speed " + labels[which]); anchor.setActivated(speed!=1f); d.dismiss();
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
        if (current < 0) { toast("Open a video first"); return; }
        if (pointA == C.TIME_UNSET) { pointA = player.getCurrentPosition(); view.setContentDescription("Set repeat point B");view.setActivated(true); toast("Point A saved"); }
        else if (pointB == C.TIME_UNSET) { pointB = Math.max(pointA + 250, player.getCurrentPosition()); view.setContentDescription("A B repeat on");view.setActivated(true); toast("A–B repeat on"); }
        else { pointA = pointB = C.TIME_UNSET; view.setContentDescription("A B repeat");view.setActivated(false); toast("A–B repeat off"); }
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
        input.setTextColor(android.graphics.Color.WHITE); input.setHintTextColor(android.graphics.Color.LTGRAY); input.setBackgroundColor(getColor(R.color.carbon)); input.setPadding(24,20,24,20);
        new AlertDialog.Builder(this).setTitle("Search videos").setView(input).setPositiveButton("Search",(d,w)->showPlaylist(input.getText().toString())).setNegativeButton("Cancel",null).show();
    }

    private void showMore(View anchor) {
        PopupMenu menu = new PopupMenu(new ContextThemeWrapper(this, R.style.ThemeOverlay_RedPlayer_Popup), anchor);
        menu.getMenu().add("Audio tracks"); menu.getMenu().add("Equalizer & Bass"); menu.getMenu().add("Subtitle timing"); menu.getMenu().add("Download subtitles"); menu.getMenu().add("History & recovery"); menu.getMenu().add("Private vault"); menu.getMenu().add("Rotate screen"); menu.getMenu().add("Sleep timer");
        menu.setOnMenuItemClickListener(item -> {
            String title=item.getTitle().toString();
            if(title.equals("Audio tracks"))showAudioTracks();
            if(title.equals("Equalizer & Bass"))showEqualizer();
            if(title.equals("Subtitle timing"))showSubtitleTiming();
            if(title.equals("Download subtitles")){if(current<0)toast("Open a video first");else downloadedSubtitleLauncher.launch(new Intent(this,SubtitleDownloadActivity.class).putExtra("video_name",names.get(current)));}
            if(title.equals("History & recovery"))historyLauncher.launch(new Intent(this,HistoryActivity.class));
            if(title.equals("Private vault"))unlockVault();
            if(title.equals("Rotate screen"))setRequestedOrientation(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            if(title.equals("Sleep timer"))showSleepTimer();
            return true;
        }); menu.show();
    }

    private void showAudioTracks() {
        Tracks tracks=player.getCurrentTracks(); ArrayList<Tracks.Group> groups=new ArrayList<>(); ArrayList<Integer> trackIndexes=new ArrayList<>(); ArrayList<String> labels=new ArrayList<>();
        for(Tracks.Group g:tracks.getGroups())if(g.getType()==C.TRACK_TYPE_AUDIO)for(int i=0;i<g.length;i++){groups.add(g);trackIndexes.add(i);String lang=g.getTrackFormat(i).language;String label=g.getTrackFormat(i).label;labels.add("Audio "+(labels.size()+1)+(lang==null?"":" • "+lang)+(label==null?"":" • "+label));}
        if(labels.isEmpty()){toast("No extra audio tracks found");return;}
        new AlertDialog.Builder(this).setTitle("Audio tracks").setSingleChoiceItems(labels.toArray(new String[0]),-1,(d,w)->{Tracks.Group g=groups.get(w);player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setOverrideForType(new TrackSelectionOverride(g.getMediaTrackGroup(),trackIndexes.get(w))).build());d.dismiss();toast("Audio track changed");}).show();
    }

    private void unlockVault() {
        vaultLauncher.launch(new Intent(this, VaultActivity.class));
    }

    private void showEqualizer() {
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(36,16,36,8);panel.setBackgroundColor(android.graphics.Color.BLACK);
        TextView eqLabel=new TextView(this);eqLabel.setText("Equalizer level");eqLabel.setTextColor(android.graphics.Color.WHITE);panel.addView(eqLabel);
        android.widget.SeekBar eq=new android.widget.SeekBar(this);eq.setMax(100);eq.setProgress(prefs.getInt("eq_level",50));panel.addView(eq);
        TextView bassLabel=new TextView(this);bassLabel.setText("Bass boost");bassLabel.setTextColor(android.graphics.Color.WHITE);panel.addView(bassLabel);
        android.widget.SeekBar bass=new android.widget.SeekBar(this);bass.setMax(100);bass.setProgress(prefs.getInt("bass",50));panel.addView(bass);
        android.content.res.ColorStateList red=android.content.res.ColorStateList.valueOf(getColor(R.color.red_player)); eq.setProgressTintList(red);eq.setThumbTintList(red);bass.setProgressTintList(red);bass.setThumbTintList(red);
        new AlertDialog.Builder(this).setTitle("Equalizer & Bass Boost").setView(panel).setNegativeButton("Off",(d,w)->{prefs.edit().putBoolean("eq_on",false).apply();PlaybackService.setEqualizer(false,eq.getProgress(),bass.getProgress());}).setPositiveButton("Save",(d,w)->{prefs.edit().putBoolean("eq_on",true).putInt("eq_level",eq.getProgress()).putInt("bass",bass.getProgress()).apply();PlaybackService.setEqualizer(true,eq.getProgress(),bass.getProgress());}).show();
    }

    private void showSubtitleTiming() {
        if(subtitleUri==null){toast("Load an SRT subtitle first");return;}
        String[] labels={"2 seconds earlier","1 second earlier","0.5 seconds earlier","No offset","0.5 seconds later","1 second later","2 seconds later"};
        long[] values={-2000,-1000,-500,0,500,1000,2000};
        new AlertDialog.Builder(this).setTitle("Subtitle timing").setItems(labels,(d,w)->{subtitleOffsetMs=values[w];prefs.edit().putLong("subtitle_offset",subtitleOffsetMs).apply();applySubtitle();}).show();
    }

    private Uri shiftSrt(Uri source,long offset) {
        if(offset==0)return source;
        try(InputStream in=getContentResolver().openInputStream(source)){
            if(in==null)return source;String text=new String(in.readAllBytes(),StandardCharsets.UTF_8);
            Pattern p=Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})");Matcher m=p.matcher(text);StringBuffer out=new StringBuffer();
            while(m.find()){long ms=((Long.parseLong(m.group(1))*60+Long.parseLong(m.group(2)))*60+Long.parseLong(m.group(3)))*1000+Long.parseLong(m.group(4));ms=Math.max(0,ms+offset);long h=ms/3600000,mi=(ms/60000)%60,s=(ms/1000)%60,x=ms%1000;m.appendReplacement(out,String.format(Locale.US,"%02d:%02d:%02d,%03d",h,mi,s,x));}m.appendTail(out);
            File folder=new File(getCacheDir(),"subtitles");folder.mkdirs();File file=new File(folder,"adjusted.srt");try(FileOutputStream stream=new FileOutputStream(file)){stream.write(out.toString().getBytes(StandardCharsets.UTF_8));}return FileProvider.getUriForFile(this,getPackageName()+".files",file);
        }catch(Exception e){toast("Subtitle timing could not be changed");return source;}
    }

    private void showSleepTimer() {
        String[] choices={"15 minutes","30 minutes","1 hour","2 hours"};
        int[] mins={15,30,60,120};
        new AlertDialog.Builder(this).setTitle("Sleep timer").setItems(choices,(d,w)->{handler.postDelayed(()->{player.pause();toast("Sleep timer stopped playback");},mins[w]*60000L);toast("Sleep timer set");}).show();
    }

    private void showSettings(View view) {
        String[] options={"Repeat playlist","Shuffle videos","Keep screen awake","Resume where I stopped","Swipe and double-tap controls","Software decoder"};
        boolean[] checked={player.getRepeatMode()!=Player.REPEAT_MODE_OFF,player.getShuffleModeEnabled(),prefs.getBoolean("keep_awake",false),resumeEnabled,gesturesEnabled,softwareDecoder};
        new AlertDialog.Builder(this).setTitle("Settings").setMultiChoiceItems(options,checked,(d,w,on)->{
            if(w==0){player.setRepeatMode(on?Player.REPEAT_MODE_ALL:Player.REPEAT_MODE_OFF);prefs.edit().putBoolean("repeat",on).apply();}
            if(w==1){player.setShuffleModeEnabled(on);prefs.edit().putBoolean("shuffle",on).apply();}
            if(w==2){prefs.edit().putBoolean("keep_awake",on).apply();applyKeepAwake(on);}
            if(w==3){resumeEnabled=on;prefs.edit().putBoolean("resume",on).apply();}
            if(w==4){gesturesEnabled=on;prefs.edit().putBoolean("gestures",on).apply();}
            if(w==5){softwareDecoder=on;prefs.edit().putBoolean("software_decoder",on).apply();PlaybackService.setSoftwareDecoder(on);playerView.setPlayer(null);connectPlayer(0);}
        }).setNeutralButton("Clear history",(d,w)->{clearHistoryOnly();toast("Playback history cleared");}).setPositiveButton("Done",null).show();
    }

    private void restoreSettings() {
        speed = prefs.getFloat("speed", 1f);
        resumeEnabled = prefs.getBoolean("resume", true);
        gesturesEnabled = prefs.getBoolean("gestures", true);
        softwareDecoder = prefs.getBoolean("software_decoder", false);
        player.setPlaybackSpeed(speed);
        player.setRepeatMode(prefs.getBoolean("repeat", false) ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF);
        player.setShuffleModeEnabled(prefs.getBoolean("shuffle", false));
        PlaybackService.setEqualizer(prefs.getBoolean("eq_on",false),prefs.getInt("eq_level",50),prefs.getInt("bass",50));
        applyKeepAwake(prefs.getBoolean("keep_awake", false));
        View speedIcon=findViewById(R.id.speedButton);speedIcon.setContentDescription("Playback speed "+speed+" times");speedIcon.setActivated(speed!=1f);
        View decoderIcon=findViewById(R.id.decoderButton);decoderIcon.setContentDescription(softwareDecoder?"Software decoder":"Hardware decoder");decoderIcon.setActivated(softwareDecoder);
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
        androidx.media3.common.Format f=localPlayer.getVideoFormat();
        androidx.media3.common.Format audio=null;for(Tracks.Group g:localPlayer.getCurrentTracks().getGroups())if(g.getType()==C.TRACK_TYPE_AUDIO)for(int i=0;i<g.length;i++)if(g.isTrackSelected(i)){audio=g.getTrackFormat(i);break;}
        String details=names.get(current)+"\n\nDuration: "+formatTime(player.getDuration())+"\nResolution: "+(f==null?"Unknown":f.width+" × "+f.height)+"\nFrame rate: "+(f==null||f.frameRate<=0?"Unknown":String.format(Locale.US,"%.2f fps",f.frameRate))+"\nVideo bitrate: "+(f==null||f.bitrate<=0?"Unknown":(f.bitrate/1000)+" kbps")+"\nVideo codec: "+(f==null?"Unknown":(f.codecs==null?f.sampleMimeType:f.codecs))+"\nAudio codec: "+(audio==null?"Unknown":(audio.codecs==null?audio.sampleMimeType:audio.codecs))+"\nAudio channels: "+(audio==null||audio.channelCount<=0?"Unknown":audio.channelCount)+"\nSample rate: "+(audio==null||audio.sampleRate<=0?"Unknown":audio.sampleRate+" Hz")+"\nAudio bitrate: "+(audio==null||audio.bitrate<=0?"Unknown":(audio.bitrate/1000)+" kbps")+"\nDecoder: "+(softwareDecoder?"Software":"Hardware")+"\nLocation: "+videos.get(current);
        startActivity(new Intent(this,TechnicalInspectorActivity.class).putExtra("details",details));
    }

    private String displayName(Uri uri) {
        try (android.database.Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())return c.getString(0);}catch(Exception ignored){}
        return uri.getLastPathSegment()==null?"Video":uri.getLastPathSegment();
    }
    private String formatTime(long ms){if(ms<0)return"Unknown";long s=ms/1000;return String.format(Locale.US,"%d:%02d:%02d",s/3600,(s/60)%60,s%60);}
    private long savedPosition(Uri uri){return prefs.getLong("pos:"+uri,0);}
    private void savePosition(){if(current>=0&&current<videos.size()){long position=player.getCurrentPosition();prefs.edit().putLong("pos:"+videos.get(current),position).apply();try{JSONArray source=new JSONArray(prefs.getString("history","[]"));JSONArray next=new JSONArray();JSONObject item=new JSONObject().put("uri",videos.get(current).toString()).put("name",names.get(current)).put("position",position).put("watched",System.currentTimeMillis());next.put(item);for(int i=0;i<source.length()&&next.length()<50;i++){JSONObject old=source.optJSONObject(i);if(old!=null&&!old.optString("uri").equals(videos.get(current).toString()))next.put(old);}prefs.edit().putString("history",next.toString()).apply();}catch(Exception ignored){}}}
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_SHORT).show();}
    private void handleIncomingVideo(Intent intent){if(intent!=null&&Intent.ACTION_VIEW.equals(intent.getAction())&&intent.getData()!=null)addVideos(Collections.singletonList(intent.getData()));}

    @Override public void onPictureInPictureModeChanged(boolean inPip,@NonNull Configuration config){super.onPictureInPictureModeChanged(inPip,config);bottomBar.setVisibility(inPip?View.GONE:View.VISIBLE);}
    @Override protected void onStop(){super.onStop();if(!isInPictureInPictureMode())savePosition();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);playerView.setPlayer(null);if(castPlayer!=null)castPlayer.release();super.onDestroy();}
}
