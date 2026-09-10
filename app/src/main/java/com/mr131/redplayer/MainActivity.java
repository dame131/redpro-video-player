package com.mr131.redplayer;

import androidx.appcompat.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ContentValues;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.provider.MediaStore;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;
import android.view.ContextThemeWrapper;
import android.view.WindowManager;
import android.view.TextureView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
    private TextView positionText, durationText;
    private SeekBar playerSeek;
    private ImageView seekPreview;
    private int previewRequest;
    private View playerControls;
    private Uri subtitleUri;
    private long subtitleOffsetMs = 0;
    private boolean softwareDecoder = false;
    private boolean audioOnly = false;
    private boolean muted = false;
    private float videoScale = 1f;
    private ScaleGestureDetector scaleDetector;
    private boolean fourKMode = true;
    private LocalCastServer localCastServer;
    private String lastAutomaticFallback="";
    private final int[] chromeIconIds={R.id.castButton,R.id.searchButton,R.id.moreButton,R.id.networkButton,R.id.cloudButton,R.id.decoderButton,R.id.subtitleButton,R.id.speedButton,R.id.fitButton,R.id.abButton,R.id.pipButton,R.id.lockButton,R.id.infoButton,R.id.openButton,R.id.previousButton,R.id.playlistButton,R.id.nextButton,R.id.settingsButton};

    private final ActivityResultLauncher<String[]> videoPicker = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(), this::addVideos);
    private final ActivityResultLauncher<String[]> playlistFilePicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::importPlaylistFile);
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
    private final ActivityResultLauncher<Intent> networkLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if(result.getResultCode()==RESULT_OK&&result.getData()!=null&&result.getData().getData()!=null){
                    Uri uri=result.getData().getData();addVideos(Collections.singletonList(uri));playIndex(videos.size()-1,true);String scheme=uri.getScheme();if(scheme!=null&&!scheme.equalsIgnoreCase("http")&&!scheme.equalsIgnoreCase("https")&&!scheme.equalsIgnoreCase("rtsp"))openVlcCodec();
                }
            });
    private final ActivityResultLauncher<Intent> downloadedSubtitleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {if(result.getResultCode()==RESULT_OK&&result.getData()!=null&&result.getData().getData()!=null)addSubtitle(result.getData().getData());});
    private final ActivityResultLauncher<Intent> historyLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {if(result.getResultCode()==RESULT_OK&&result.getData()!=null&&result.getData().getData()!=null)addVideos(Collections.singletonList(result.getData().getData()));});

    @Override protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RedPlayerBackground.apply(findViewById(R.id.root));
        findViewById(R.id.root).setContentDescription("131 Red Player Home Ready");
        prefs = getSharedPreferences("red_player", MODE_PRIVATE);
        playerView = findViewById(R.id.playerView);
        titleText = findViewById(R.id.titleText);
        bottomBar = findViewById(R.id.bottomBar);
        gestureOverlay = findViewById(R.id.gestureOverlay);
        positionText = findViewById(R.id.positionText);
        durationText = findViewById(R.id.durationText);
        playerSeek = findViewById(R.id.playerSeek);
        seekPreview = findViewById(R.id.seekPreview);
        playerControls = findViewById(R.id.playerControls);
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
        player = localPlayer; playerView.setPlayer(player); restoreSettings(); wirePlayer(); playerView.post(() -> setupCast()); handleIncomingVideo(getIntent());if(videos.isEmpty())loadSavedPlaylist();
    }

    private void wireButtons() {
        for(int id:chromeIconIds){View icon=findViewById(id);if(icon!=null)icon.setBackgroundColor(android.graphics.Color.TRANSPARENT);}
        bindAnimated(R.id.openButton,v -> libraryLauncher.launch(new Intent(this, LibraryActivity.class)));
        bindAnimated(R.id.networkButton,v -> networkLauncher.launch(new Intent(this,NetworkSourcesActivity.class)));
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
        bindAnimated(R.id.rewindButton,v -> {if(player!=null)player.seekTo(Math.max(0,player.getCurrentPosition()-10_000));});
        bindAnimated(R.id.centerPlayButton,v -> {if(player!=null){if(player.isPlaying())player.pause();else player.play();}});
        bindAnimated(R.id.forwardButton,v -> {if(player!=null)player.seekTo(Math.min(Math.max(0,player.getDuration()),player.getCurrentPosition()+10_000));});
        playerSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar bar,int value,boolean fromUser){if(fromUser&&player!=null&&player.getDuration()>0){long target=player.getDuration()*value/1000L;player.seekTo(target);loadSeekPreview(target);}}
            @Override public void onStartTrackingTouch(SeekBar bar){seekPreview.setVisibility(View.VISIBLE);}
            @Override public void onStopTrackingTouch(SeekBar bar){previewRequest++;seekPreview.setVisibility(View.GONE);}
        });
    }

    private void bindAnimated(int id, View.OnClickListener action) {
        View icon=findViewById(id);icon.setOnClickListener(v->{animateIcon(v);action.onClick(v);});
    }

    private void animateIcon(View icon) {
        if(icon instanceof ChromeIconButton)((ChromeIconButton)icon).pulse();
        icon.animate().cancel();icon.animate().scaleX(.72f).scaleY(.72f).rotationBy(18f).alpha(.6f).setDuration(90).withEndAction(()->icon.animate().scaleX(1.12f).scaleY(1.12f).rotation(0f).alpha(1f).setDuration(120).withEndAction(()->icon.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start()).start();
    }

    private void setChromeActive(View icon, boolean active) {
        icon.setActivated(active);
        if(icon instanceof ChromeIconButton)((ChromeIconButton)icon).setChromeActive(active);
    }

    private void startIconEntrance() {
        int[] ids={R.id.searchButton,R.id.moreButton,R.id.networkButton,R.id.cloudButton,R.id.decoderButton,R.id.subtitleButton,R.id.speedButton,R.id.fitButton,R.id.abButton,R.id.pipButton,R.id.lockButton,R.id.infoButton,R.id.openButton,R.id.previousButton,R.id.playlistButton,R.id.nextButton,R.id.settingsButton};
        String fingerprint=android.os.Build.FINGERPRINT.toLowerCase();
        if(fingerprint.contains("generic")||fingerprint.contains("emulator")){
            for(int id:ids){View icon=findViewById(id);icon.setAlpha(1f);icon.setScaleX(1f);icon.setScaleY(1f);icon.setTranslationY(0f);}
            setChromeActive(findViewById(R.id.openButton),true);
            return;
        }
        for(int i=0;i<ids.length;i++){View icon=findViewById(ids[i]);icon.setAlpha(0f);icon.setScaleX(.55f);icon.setScaleY(.55f);icon.setTranslationY(14f);icon.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setStartDelay(i*22L).setDuration(260).start();}
        setChromeActive(findViewById(R.id.openButton),true);
        handler.postDelayed(()->{for(int i=0;i<chromeIconIds.length;i++){final View icon=findViewById(chromeIconIds[i]);handler.postDelayed(()->{if(icon instanceof ChromeIconButton)((ChromeIconButton)icon).pulse();},i*70L);}},500);
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
        softwareDecoder=!softwareDecoder;prefs.edit().putBoolean("software_decoder",softwareDecoder).apply();setChromeActive(view,softwareDecoder);view.setContentDescription(softwareDecoder?"Software decoder":"Hardware decoder");
        if(softwareDecoder){toast("VLC software codec selected");if(current>=0)openVlcCodec();}
        else{PlaybackService.setSoftwareDecoder(false);playerView.setPlayer(null);connectPlayer(0);toast("Hardware decoder selected");}
    }

    private void setupCast() {
        try {
            CastContext context=CastContext.getSharedInstance(this);CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),(MediaRouteButton)findViewById(R.id.castButton));castPlayer=new CastPlayer(context);
            castPlayer.setSessionAvailabilityListener(new SessionAvailabilityListener(){
                @Override public void onCastSessionAvailable(){
                    if(current<0||current>=videos.size()){toast("Choose a video first");return;}Uri source=videos.get(current);String scheme=source.getScheme();Uri castUri=source;
                    try{if(!"http".equalsIgnoreCase(scheme)&&!"https".equalsIgnoreCase(scheme)){if(localCastServer!=null)localCastServer.stop();localCastServer=new LocalCastServer(MainActivity.this,source);castUri=Uri.parse(localCastServer.start());}}catch(Exception e){toast(e.getMessage()==null?"Local casting could not start":e.getMessage());return;}
                    long position=localPlayer.getCurrentPosition();boolean playing=localPlayer.getPlayWhenReady();localPlayer.pause();MediaItem item=new MediaItem.Builder().setUri(castUri).setTag(names.get(current)).build();
                    castPlayer.setMediaItem(item,Math.max(0,position));castPlayer.prepare();castPlayer.setPlayWhenReady(playing);player=castPlayer;playerView.setPlayer(player);toast("Casting to TV");
                }
                @Override public void onCastSessionUnavailable(){if(localCastServer!=null){localCastServer.stop();localCastServer=null;}if(player==castPlayer){long position=castPlayer.getCurrentPosition();boolean playing=castPlayer.getPlayWhenReady();castPlayer.stop();player=localPlayer;playerView.setPlayer(player);if(current>=0&&current<localPlayer.getMediaItemCount())localPlayer.seekTo(current,position);localPlayer.setPlayWhenReady(playing);toast("Playing on phone");}}
            });
        } catch(Exception e){findViewById(R.id.castButton).setVisibility(View.GONE);}
    }

    private void wirePlayer() {
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if(state==Player.STATE_READY){playerView.setContentDescription("Video Ready");updatePlayerControls();updateFourKBadge();}
            }
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                if (!isPlaying) savePosition();
                findViewById(R.id.centerPlayButton).setContentDescription(isPlaying?"Pause":"Play");
            }
            @Override public void onMediaItemTransition(MediaItem item, int reason) {
                current = player.getCurrentMediaItemIndex();
                if (current >= 0 && current < names.size()) titleText.setText(names.get(current));
            }
            @Override public void onPlayerError(@NonNull PlaybackException error) {
                prefs.edit().putString("last_error",error.getErrorCodeName()+": "+error.getMessage()).apply();
                String failed=current>=0&&current<videos.size()?videos.get(current).toString():"";if(!failed.isEmpty()&&!failed.equals(lastAutomaticFallback)){lastAutomaticFallback=failed;toast("Switching to VLC codec…");openVlcCodec();return;}
                new AlertDialog.Builder(MainActivity.this).setTitle("Playback recovery").setMessage("The phone codec could not play this video. Use the built-in VLC codec, retry, or skip it.").setPositiveButton("VLC CODEC",(d,w)->openVlcCodec()).setNeutralButton("Retry",(d,w)->{player.prepare();player.play();}).setNegativeButton("Next video",(d,w)->playIndex(current+1,true)).show();
            }
        });
        handler.post(abLoop);
        handler.post(progressLoop);
    }

    private final Runnable progressLoop=new Runnable(){@Override public void run(){updatePlayerControls();handler.postDelayed(this,500);}};
    private void updatePlayerControls(){
        if(player==null)return;long position=Math.max(0,player.getCurrentPosition());long duration=player.getDuration();
        positionText.setText(formatTime(position));durationText.setText(duration>0?formatTime(duration):"0:00:00");
        playerSeek.setProgress(duration>0?(int)Math.min(1000,position*1000/duration):0);
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
    private void loadSavedPlaylist(){java.util.Set<String> saved=prefs.getStringSet("saved_playlist",java.util.Collections.emptySet());for(String value:saved){Uri uri=Uri.parse(value);if(!videos.contains(uri)){videos.add(uri);names.add(displayName(uri));}}if(!videos.isEmpty()){rebuildPlaylist();playIndex(0,false);}}

    private void playIndex(int index, boolean play) {
        if (videos.isEmpty()) { toast("Add videos first"); return; }
        if (index < 0) index = videos.size() - 1;
        if (index >= videos.size()) index = 0;
        current = index;
        speed=prefs.getFloat(videoKey("speed"),prefs.getFloat("speed",1f));fitMode=prefs.getInt(videoKey("fit"),fitMode);subtitleOffsetMs=prefs.getLong(videoKey("subtitle_offset"),prefs.getLong("subtitle_offset",0));player.setPlaybackSpeed(speed);
        applyFourKMode();applyFitMode();
        player.seekTo(index, resumeEnabled ? savedPosition(videos.get(index)) : 0);
        player.prepare();
        if (play) player.play();
        titleText.setText(names.get(index));
    }

    private void addSubtitle(Uri uri) {
        if (uri == null || current < 0) return;
        subtitleUri = uri; subtitleOffsetMs = prefs.getLong("subtitle_offset", 0); applySubtitleAppearance(); applySubtitle();
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
        final String[] labels = {"0.25×","0.5×","0.75×","1×","1.25×","1.5×","1.75×","2×","3×","4×","6×","8×"};
        final float[] values = {.25f,.5f,.75f,1f,1.25f,1.5f,1.75f,2f,3f,4f,6f,8f};
        new AlertDialog.Builder(this).setTitle("Playback speed").setSingleChoiceItems(labels, indexOf(values, speed), (d, which) -> {
            speed = values[which]; player.setPlaybackSpeed(speed); SharedPreferences.Editor edit=prefs.edit().putFloat("speed",speed);if(current>=0)edit.putFloat(videoKey("speed"),speed);edit.apply(); anchor.setContentDescription("Playback speed " + labels[which]); setChromeActive(anchor,speed!=1f); d.dismiss();
        }).show();
    }

    private int indexOf(float[] values, float value) { for (int i=0;i<values.length;i++) if(values[i]==value)return i; return 3; }

    private void changeFit() {
        int[] modes = {AspectRatioFrameLayout.RESIZE_MODE_FIT, AspectRatioFrameLayout.RESIZE_MODE_ZOOM, AspectRatioFrameLayout.RESIZE_MODE_FILL};
        String[] labels = {"Fit inside", "Fill screen", "Stretch"};
        fitMode = (fitMode + 1) % modes.length;
        playerView.setResizeMode(modes[fitMode]);if(current>=0)prefs.edit().putInt(videoKey("fit"),fitMode).apply();
        toast(labels[fitMode]);
    }
    private void applyFitMode(){int[] modes={AspectRatioFrameLayout.RESIZE_MODE_FIT,AspectRatioFrameLayout.RESIZE_MODE_ZOOM,AspectRatioFrameLayout.RESIZE_MODE_FILL};playerView.setResizeMode(modes[Math.max(0,Math.min(2,fitMode))]);}

    private void setAB(View view) {
        if (current < 0) { toast("Open a video first"); return; }
        if (pointA == C.TIME_UNSET) { pointA = player.getCurrentPosition(); view.setContentDescription("Set repeat point B");setChromeActive(view,true); toast("Point A saved"); }
        else if (pointB == C.TIME_UNSET) { pointB = Math.max(pointA + 250, player.getCurrentPosition()); view.setContentDescription("A B repeat on");setChromeActive(view,true); toast("A–B repeat on"); }
        else { pointA = pointB = C.TIME_UNSET; view.setContentDescription("A B repeat");setChromeActive(view,false); toast("A–B repeat off"); }
    }

    private void enterPip() {
        if (current < 0) { toast("Open a video first"); return; }
        Rect rect = new Rect(); playerView.getGlobalVisibleRect(rect);
        PictureInPictureParams params = new PictureInPictureParams.Builder().setAspectRatio(new Rational(16,9)).setSourceRectHint(rect).build();
        enterPictureInPictureMode(params);
    }

    private void toggleLock() {
        controlsLocked = !controlsLocked;
        playerControls.setVisibility(controlsLocked?View.GONE:View.VISIBLE);
        findViewById(R.id.lockButton).setAlpha(controlsLocked ? .45f : 1f);
        toast(controlsLocked ? "Controls locked. Hold video to unlock." : "Controls unlocked");
    }

    private void wireGestures() {
        scaleDetector = new ScaleGestureDetector(this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector detector){if(controlsLocked||!gesturesEnabled)return false;videoScale=Math.max(1f,Math.min(5f,videoScale*detector.getScaleFactor()));View surface=playerView.getVideoSurfaceView();if(surface!=null){surface.setPivotX(detector.getFocusX());surface.setPivotY(detector.getFocusY());surface.setScaleX(videoScale);surface.setScaleY(videoScale);}showGesture(String.format(Locale.US,"Zoom %.1f×",videoScale));return true;}
        });
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        GestureDetector detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(@NonNull MotionEvent e) { return true; }
            @Override public void onLongPress(@NonNull MotionEvent e) { if (controlsLocked) toggleLock(); }
            @Override public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (!controlsLocked) playerControls.setVisibility(playerControls.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);
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
        playerView.setOnTouchListener((view, event) -> {boolean zoom=scaleDetector.onTouchEvent(event);if(scaleDetector.isInProgress())return true;return detector.onTouchEvent(event)||zoom;});
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
        menu.getMenu().add("Audio tracks");menu.getMenu().add("Equalizer & Bass");menu.getMenu().add("Subtitle timing");menu.getMenu().add("Download subtitles");menu.getMenu().add("History & recovery");menu.getMenu().add("Private vault");menu.getMenu().add("Tools & storage");menu.getMenu().add("Rotate screen");menu.getMenu().add("Sleep timer");
        menu.setOnMenuItemClickListener(item -> {
            String title=item.getTitle().toString();
            if(title.equals("Tools & storage"))showTools();
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
    private void showTools(){String[] items={"Advanced playback","Music library","Import M3U playlist","VLC codec player","Audio-only mode","Save video screenshot","Backup & restore","Network sources","Playback Lab"};new AlertDialog.Builder(this).setTitle("Tools & storage").setItems(items,(d,w)->{if(w==0)showAdvancedPlayback();if(w==1)libraryLauncher.launch(new Intent(this,AudioLibraryActivity.class));if(w==2)playlistFilePicker.launch(new String[]{"audio/x-mpegurl","application/x-mpegURL","application/vnd.apple.mpegurl","text/plain"});if(w==3)openVlcCodec();if(w==4)toggleAudioOnly();if(w==5)saveVideoScreenshot();if(w==6)startActivity(new Intent(this,BackupActivity.class));if(w==7)networkLauncher.launch(new Intent(this,NetworkSourcesActivity.class));if(w==8)startActivity(new Intent(this,PlaybackLabActivity.class));}).setNegativeButton("Close",null).show();}



    private void importPlaylistFile(Uri uri){if(uri==null)return;new Thread(()->{int added=0;try(BufferedReader reader=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri),StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){line=line.trim();if(line.isEmpty()||line.startsWith("#"))continue;Uri media=Uri.parse(line);String scheme=media.getScheme();if(scheme==null){try{media=Uri.withAppendedPath(uri.buildUpon().path(uri.getPath()==null?"":uri.getPath().substring(0,Math.max(0,uri.getPath().lastIndexOf('/')+1))).build(),line);}catch(Exception ignored){continue;}}final Uri item=media;runOnUiThread(()->addVideos(Collections.singletonList(item)));added++;}}catch(Exception e){runOnUiThread(()->toast("Playlist could not be read"));return;}int count=added;runOnUiThread(()->{toast(count+" playlist items imported");if(count>0)playIndex(Math.max(0,videos.size()-count),false);});},"playlist-import").start();}

    private void showAdvancedPlayback(){
        String[] items={"Quick mute","Frame backward","Frame forward","Jump to time","Reset pinch zoom","Video enhancement filters","Subtitle appearance","Preferred audio & subtitle language","Equalizer presets"};
        new AlertDialog.Builder(this).setTitle("Advanced playback").setItems(items,(d,w)->{if(w==0)toggleMute();if(w==1)stepFrame(-1);if(w==2)stepFrame(1);if(w==3)showJumpToTime();if(w==4)resetZoom();if(w==5)showVideoFilters();if(w==6)showSubtitleAppearance();if(w==7)showPreferredLanguages();if(w==8)showEqualizerPresets();}).setNegativeButton("Close",null).show();
    }
    private void toggleMute(){AudioManager a=(AudioManager)getSystemService(AUDIO_SERVICE);muted=!muted;a.adjustStreamVolume(AudioManager.STREAM_MUSIC,muted?AudioManager.ADJUST_MUTE:AudioManager.ADJUST_UNMUTE,0);toast(muted?"Quick mute on":"Sound restored");}
    private void stepFrame(int direction){if(player==null||current<0){toast("Open a video first");return;}player.pause();long step=33;androidx.media3.common.Format f=localPlayer.getVideoFormat();if(f!=null&&f.frameRate>0)step=Math.max(1,Math.round(1000f/f.frameRate));player.seekTo(Math.max(0,Math.min(player.getDuration(),player.getCurrentPosition()+direction*step)));showGesture(direction<0?"Previous frame":"Next frame");}
    private void showJumpToTime(){if(player==null||current<0){toast("Open a video first");return;}EditText input=new EditText(this);input.setHint("HH:MM:SS or seconds");input.setSingleLine(true);input.setTextColor(android.graphics.Color.WHITE);input.setHintTextColor(android.graphics.Color.LTGRAY);new AlertDialog.Builder(this).setTitle("Jump to time").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Jump",(d,w)->{try{String[] p=input.getText().toString().trim().split(":");long seconds=0;for(String part:p)seconds=seconds*60+Long.parseLong(part);player.seekTo(Math.max(0,Math.min(player.getDuration(),seconds*1000)));}catch(Exception e){toast("Enter a valid time");}}).show();}
    private void resetZoom(){videoScale=1f;View surface=playerView.getVideoSurfaceView();if(surface!=null){surface.setScaleX(1f);surface.setScaleY(1f);surface.setTranslationX(0f);surface.setTranslationY(0f);}toast("Pinch zoom reset");}
    private void showVideoFilters(){String[] labels={"Natural","Vivid","Cinema","Grayscale","High contrast"};new AlertDialog.Builder(this).setTitle("Video enhancement filters").setSingleChoiceItems(labels,prefs.getInt("video_filter",0),(d,w)->{prefs.edit().putInt("video_filter",w).apply();applyVideoFilter(w);d.dismiss();toast(labels[w]+" filter");}).show();}
    private void applyVideoFilter(int mode){View surface=playerView.getVideoSurfaceView();if(surface==null)return;ColorMatrix matrix=new ColorMatrix();if(mode==1)matrix.setSaturation(1.35f);else if(mode==2)matrix.set(new float[]{1.15f,0,0,0,8,0,1.05f,0,0,2,0,0,.9f,0,-4,0,0,0,1,0});else if(mode==3)matrix.setSaturation(0f);else if(mode==4)matrix.set(new float[]{1.35f,0,0,0,-28,0,1.35f,0,0,-28,0,0,1.35f,0,-28,0,0,0,1,0});Paint paint=new Paint();paint.setColorFilter(mode==0?null:new ColorMatrixColorFilter(matrix));surface.setLayerType(View.LAYER_TYPE_HARDWARE,paint);surface.setLayerPaint(paint);}
    private void showSubtitleAppearance(){String[] labels={"Small white","Medium white","Large white","Large yellow","Large crimson"};new AlertDialog.Builder(this).setTitle("Subtitle appearance").setSingleChoiceItems(labels,prefs.getInt("subtitle_style",1),(d,w)->{prefs.edit().putInt("subtitle_style",w).apply();applySubtitleAppearance();d.dismiss();}).show();}
    private void applySubtitleAppearance(){if(playerView.getSubtitleView()==null)return;int style=prefs.getInt("subtitle_style",1);float size=style==0?.045f:style==1?.055f:.07f;int color=style==3?android.graphics.Color.YELLOW:style==4?getColor(R.color.red_player):android.graphics.Color.WHITE;playerView.getSubtitleView().setFractionalTextSize(size);playerView.getSubtitleView().setStyle(new androidx.media3.ui.CaptionStyleCompat(color,android.graphics.Color.TRANSPARENT,android.graphics.Color.BLACK,androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE,android.graphics.Color.BLACK,null));}
    private void showPreferredLanguages(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(30,10,30,10);EditText audio=new EditText(this);audio.setHint("Audio language code, e.g. en");audio.setText(prefs.getString("audio_language",""));audio.setTextColor(android.graphics.Color.WHITE);EditText subtitle=new EditText(this);subtitle.setHint("Subtitle language code, e.g. en");subtitle.setText(prefs.getString("subtitle_language",""));subtitle.setTextColor(android.graphics.Color.WHITE);box.addView(audio);box.addView(subtitle);new AlertDialog.Builder(this).setTitle("Preferred languages").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{prefs.edit().putString("audio_language",audio.getText().toString().trim()).putString("subtitle_language",subtitle.getText().toString().trim()).apply();applyPreferredLanguages();}).show();}
    private void applyPreferredLanguages(){if(localPlayer==null)return;String audio=prefs.getString("audio_language",""),sub=prefs.getString("subtitle_language","");androidx.media3.common.TrackSelectionParameters.Builder b=localPlayer.getTrackSelectionParameters().buildUpon();if(!audio.isEmpty())b.setPreferredAudioLanguage(audio);if(!sub.isEmpty())b.setPreferredTextLanguage(sub);localPlayer.setTrackSelectionParameters(b.build());}
    private void showEqualizerPresets(){String[] labels={"Flat","Bass Boost","Voice","Cinema","Late Night"};int[] eq={50,60,70,65,45},bass={50,100,25,75,35};new AlertDialog.Builder(this).setTitle("Equalizer presets").setItems(labels,(d,w)->{prefs.edit().putBoolean("eq_on",true).putInt("eq_level",eq[w]).putInt("bass",bass[w]).apply();PlaybackService.setEqualizer(true,eq[w],bass[w]);toast(labels[w]+" preset");}).show();}

    private void showAudioTracks() {
        Tracks tracks=player.getCurrentTracks(); ArrayList<Tracks.Group> groups=new ArrayList<>(); ArrayList<Integer> trackIndexes=new ArrayList<>(); ArrayList<String> labels=new ArrayList<>();
        for(Tracks.Group g:tracks.getGroups())if(g.getType()==C.TRACK_TYPE_AUDIO)for(int i=0;i<g.length;i++){groups.add(g);trackIndexes.add(i);String lang=g.getTrackFormat(i).language;String label=g.getTrackFormat(i).label;labels.add("Audio "+(labels.size()+1)+(lang==null?"":" • "+lang)+(label==null?"":" • "+label));}
        if(labels.isEmpty()){toast("No extra audio tracks found");return;}
        new AlertDialog.Builder(this).setTitle("Audio tracks").setSingleChoiceItems(labels.toArray(new String[0]),-1,(d,w)->{Tracks.Group g=groups.get(w);player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setOverrideForType(new TrackSelectionOverride(g.getMediaTrackGroup(),trackIndexes.get(w))).build());d.dismiss();toast("Audio track changed");}).show();
    }

    private void unlockVault() {
        vaultLauncher.launch(new Intent(this, VaultActivity.class));
    }

    private void openVlcCodec(){if(current<0||current>=videos.size()){toast("Open a video first");return;}long position=player==null?0:Math.max(0,player.getCurrentPosition());if(player!=null)player.pause();Intent intent=new Intent(this,VlcPlayerActivity.class).setData(videos.get(current)).putExtra("title",names.get(current)).putExtra("position",position).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(intent);}

    private void showEqualizer() {
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(36,16,36,8);RedPlayerBackground.apply(panel);
        panel.addView(LuxuryIconView.create(this,R.drawable.icon_equalizer_thick,"Equalizer emblem"));
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
        new AlertDialog.Builder(this).setTitle("Subtitle timing").setItems(labels,(d,w)->{subtitleOffsetMs=values[w];SharedPreferences.Editor edit=prefs.edit().putLong("subtitle_offset",subtitleOffsetMs);if(current>=0)edit.putLong(videoKey("subtitle_offset"),subtitleOffsetMs);edit.apply();applySubtitle();}).show();
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
        String[] options={"4K Ultra HD mode","Repeat playlist","Shuffle videos","Keep screen awake","Resume where I stopped","Swipe and double-tap controls","Software decoder"};
        boolean[] checked={fourKMode,player.getRepeatMode()!=Player.REPEAT_MODE_OFF,player.getShuffleModeEnabled(),prefs.getBoolean("keep_awake",false),resumeEnabled,gesturesEnabled,softwareDecoder};
        new AlertDialog.Builder(this).setTitle("Settings").setMultiChoiceItems(options,checked,(d,w,on)->{
            if(w==0){fourKMode=on;prefs.edit().putBoolean("four_k_mode",on).apply();applyFourKMode();toast(on?"4K Ultra HD mode on":"4K mode off");}
            if(w==1){player.setRepeatMode(on?Player.REPEAT_MODE_ALL:Player.REPEAT_MODE_OFF);prefs.edit().putBoolean("repeat",on).apply();}
            if(w==2){player.setShuffleModeEnabled(on);prefs.edit().putBoolean("shuffle",on).apply();}
            if(w==3){prefs.edit().putBoolean("keep_awake",on).apply();applyKeepAwake(on);}
            if(w==4){resumeEnabled=on;prefs.edit().putBoolean("resume",on).apply();}
            if(w==5){gesturesEnabled=on;prefs.edit().putBoolean("gestures",on).apply();}
            if(w==6){softwareDecoder=on;prefs.edit().putBoolean("software_decoder",on).apply();if(on)openVlcCodec();else{PlaybackService.setSoftwareDecoder(false);playerView.setPlayer(null);connectPlayer(0);}}
        }).setNeutralButton("Clear history",(d,w)->{clearHistoryOnly();toast("Playback history cleared");}).setPositiveButton("Done",null).show();
    }

    private void restoreSettings() {
        speed = prefs.getFloat("speed", 1f);
        resumeEnabled = prefs.getBoolean("resume", true);
        gesturesEnabled = prefs.getBoolean("gestures", true);
        softwareDecoder = prefs.getBoolean("software_decoder", false);
        fourKMode = prefs.getBoolean("four_k_mode", true);
        audioOnly = prefs.getBoolean("audio_only",false);playerView.setAlpha(audioOnly?0f:1f);
        applyPreferredLanguages();applyVideoFilter(prefs.getInt("video_filter",0));applySubtitleAppearance();
        player.setPlaybackSpeed(speed);
        player.setRepeatMode(prefs.getBoolean("repeat", false) ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF);
        player.setShuffleModeEnabled(prefs.getBoolean("shuffle", false));
        PlaybackService.setEqualizer(prefs.getBoolean("eq_on",false),prefs.getInt("eq_level",50),prefs.getInt("bass",50));
        applyKeepAwake(prefs.getBoolean("keep_awake", false));
        View speedIcon=findViewById(R.id.speedButton);speedIcon.setContentDescription("Playback speed "+speed+" times");setChromeActive(speedIcon,speed!=1f);
        View decoderIcon=findViewById(R.id.decoderButton);decoderIcon.setContentDescription(softwareDecoder?"VLC software codec":"Hardware decoder");setChromeActive(decoderIcon,softwareDecoder);
    }

    private void applyFourKMode() {
        if (localPlayer == null) return;
        androidx.media3.common.TrackSelectionParameters.Builder parameters=localPlayer.getTrackSelectionParameters().buildUpon();
        if(fourKMode) parameters.setMaxVideoSize(7680,4320).setForceHighestSupportedBitrate(true);
        else parameters.setMaxVideoSize(1920,1080).setForceHighestSupportedBitrate(false);
        localPlayer.setTrackSelectionParameters(parameters.build());
        updateFourKBadge();
    }

    private void updateFourKBadge() {
        TextView badge=findViewById(R.id.fourKBadge);if(badge==null)return;
        androidx.media3.common.Format format=localPlayer==null?null:localPlayer.getVideoFormat();
        boolean ultra=format!=null&&(format.width>=3840||format.height>=2160);
        badge.setText(ultra?"4K UHD":fourKMode?"4K READY":"HD");
        badge.setContentDescription(ultra?"4K Ultra HD video active":fourKMode?"4K Ultra HD mode ready":"HD playback mode");
        badge.setVisibility(View.VISIBLE);
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
    private void toggleAudioOnly(){audioOnly=!audioOnly;playerView.setAlpha(audioOnly?0f:1f);prefs.edit().putBoolean("audio_only",audioOnly).apply();toast(audioOnly?"Audio-only mode on":"Video restored");}
    private void saveVideoScreenshot(){if(current<0){toast("Open a video first");return;}View surface=playerView.getVideoSurfaceView();if(!(surface instanceof TextureView)){toast("Screenshot is unavailable for this video");return;}Bitmap bitmap=((TextureView)surface).getBitmap();if(bitmap==null){toast("Video frame is not ready");return;}new Thread(()->{try{ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,"131-player-"+System.currentTimeMillis()+".png");values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");if(android.os.Build.VERSION.SDK_INT>=29)values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/131 Red Player");Uri target=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);if(target==null)throw new Exception();try(OutputStream out=getContentResolver().openOutputStream(target)){if(out==null||!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new Exception();}runOnUiThread(()->toast("Video screenshot saved"));}catch(Exception e){runOnUiThread(()->toast("Screenshot could not be saved"));}finally{bitmap.recycle();}},"frame-save").start();}
    private void loadSeekPreview(long position){if(current<0||current>=videos.size())return;int request=++previewRequest;Uri uri=videos.get(current);new Thread(()->{MediaMetadataRetriever r=new MediaMetadataRetriever();try{r.setDataSource(this,uri);Bitmap frame=r.getFrameAtTime(position*1000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);if(frame!=null)runOnUiThread(()->{if(request==previewRequest&&seekPreview.getVisibility()==View.VISIBLE)seekPreview.setImageBitmap(frame);else frame.recycle();});}catch(Exception ignored){}finally{try{r.release();}catch(Exception ignored){}}},"seek-preview").start();}

    private String displayName(Uri uri) {
        try (android.database.Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())return c.getString(0);}catch(Exception ignored){}
        return uri.getLastPathSegment()==null?"Video":uri.getLastPathSegment();
    }
    private String formatTime(long ms){if(ms<0)return"Unknown";long s=ms/1000;return String.format(Locale.US,"%d:%02d:%02d",s/3600,(s/60)%60,s%60);}
    private long savedPosition(Uri uri){return prefs.getLong("pos:"+uri,0);}
    private String videoKey(String setting){return setting+":"+(current>=0&&current<videos.size()?videos.get(current):"none");}
    private void savePosition(){if(current>=0&&current<videos.size()){long position=player.getCurrentPosition();prefs.edit().putLong("pos:"+videos.get(current),position).apply();try{JSONArray source=new JSONArray(prefs.getString("history","[]"));JSONArray next=new JSONArray();JSONObject item=new JSONObject().put("uri",videos.get(current).toString()).put("name",names.get(current)).put("position",position).put("watched",System.currentTimeMillis());next.put(item);for(int i=0;i<source.length()&&next.length()<50;i++){JSONObject old=source.optJSONObject(i);if(old!=null&&!old.optString("uri").equals(videos.get(current).toString()))next.put(old);}prefs.edit().putString("history",next.toString()).apply();}catch(Exception ignored){}}}
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_SHORT).show();}
    private void handleIncomingVideo(Intent intent){if(intent!=null&&Intent.ACTION_VIEW.equals(intent.getAction())&&intent.getData()!=null)addVideos(Collections.singletonList(intent.getData()));}

    @Override public void onPictureInPictureModeChanged(boolean inPip,@NonNull Configuration config){super.onPictureInPictureModeChanged(inPip,config);bottomBar.setVisibility(inPip?View.GONE:View.VISIBLE);playerControls.setVisibility(inPip?View.GONE:View.VISIBLE);}
    @Override protected void onStop(){super.onStop();if(!isInPictureInPictureMode())savePosition();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);playerView.setPlayer(null);if(localCastServer!=null)localCastServer.stop();if(castPlayer!=null)castPlayer.release();super.onDestroy();}
}
