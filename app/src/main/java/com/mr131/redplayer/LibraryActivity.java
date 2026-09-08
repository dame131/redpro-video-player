package com.mr131.redplayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LibraryActivity extends AppCompatActivity {
    private final ArrayList<VideoEntry> allVideos = new ArrayList<>();
    private final ArrayList<VideoEntry> shownVideos = new ArrayList<>();
    private final ArrayList<String> shownLabels = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView countText;

    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), allowed -> { if (allowed) loadVideos(); else showPermissionMessage(); });
    private final ActivityResultLauncher<String[]> filePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) choose(uri); });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Video Library");
        setContentView(buildScreen());
        requestLibrary();
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setId(View.generateViewId());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(12), dp(14), dp(12));
        RedPlayerBackground.apply(root);

        TextView heading = label("VIDEO LIBRARY", 24, Color.WHITE);
        heading.setContentDescription("Video Library");
        root.addView(heading);
        countText = label("Scanning phone…", 13, Color.LTGRAY);
        root.addView(countText);

        SearchView search = new SearchView(this);
        search.setQueryHint("Search videos or folders");
        search.setIconifiedByDefault(false);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { filter(query); return true; }
            @Override public boolean onQueryTextChange(String query) { filter(query); return true; }
        });
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(58)));

        Button browse = new Button(this);
        browse.setText("BROWSE FILES"); browse.setTextColor(Color.WHITE); browse.setBackgroundColor(getColor(R.color.red_player));
        browse.setOnClickListener(v -> filePicker.launch(new String[]{"video/*"}));
        root.addView(browse, new LinearLayout.LayoutParams(-1, dp(48)));

        ListView list = new ListView(this);
        list.setDividerHeight(1); list.setBackgroundColor(Color.TRANSPARENT);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, shownLabels) {
            @NonNull @Override public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                TextView row = (TextView) super.getView(position, convertView, parent);
                row.setTextColor(Color.WHITE); row.setTextSize(15); row.setGravity(Gravity.CENTER_VERTICAL); row.setMinHeight(dp(64));
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> choose(shownVideos.get(position).uri));
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private void requestLibrary() {
        String required = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_VIDEO : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, required) == PackageManager.PERMISSION_GRANTED) loadVideos();
        else permission.launch(required);
    }

    private void loadVideos() {
        new Thread(() -> scanVideos()).start();
    }

    private void scanVideos() {
        ArrayList<VideoEntry> scanned = new ArrayList<>();
        String[] columns = {MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME, MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE};
        try (Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                columns, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC")) {
            if (cursor != null) while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String name = cursor.getString(1);
                String folder = cursor.getString(2);
                long duration = cursor.getLong(3);
                long size = cursor.getLong(4);
                Uri uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                scanned.add(new VideoEntry(uri, name == null ? "Video" : name,
                        folder == null ? "Phone" : folder, duration, size));
            }
        }
        runOnUiThread(() -> { allVideos.clear(); allVideos.addAll(scanned); filter(""); });
    }

    private void filter(String query) {
        String term = query == null ? "" : query.trim().toLowerCase(Locale.US);
        shownVideos.clear(); shownLabels.clear();
        for (VideoEntry video : allVideos) {
            if (!term.isEmpty() && !video.name.toLowerCase(Locale.US).contains(term)
                    && !video.folder.toLowerCase(Locale.US).contains(term)) continue;
            shownVideos.add(video);
            shownLabels.add(video.name + "\n" + video.folder + "  •  " + time(video.duration) + "  •  " + size(video.size));
        }
        if (adapter != null) adapter.notifyDataSetChanged();
        if (countText != null) countText.setText(shownVideos.size() + " videos on this phone");
    }

    private void showPermissionMessage() {
        countText.setText("Video access is off. Use Browse Files or allow Videos in phone settings.");
    }

    private void choose(Uri uri) {
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
        setResult(RESULT_OK, new Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        finish();
    }

    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color); view.setPadding(0, dp(6), 0, dp(6)); return view;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private String time(long ms) { long s=ms/1000; return String.format(Locale.US, "%d:%02d", s/60, s%60); }
    private String size(long bytes) { return bytes >= 1_073_741_824L ? String.format(Locale.US,"%.1f GB",bytes/1_073_741_824f) : String.format(Locale.US,"%.0f MB",bytes/1_048_576f); }

    private static final class VideoEntry {
        final Uri uri; final String name; final String folder; final long duration; final long size;
        VideoEntry(Uri uri, String name, String folder, long duration, long size) { this.uri=uri; this.name=name; this.folder=folder; this.duration=duration; this.size=size; }
    }
}
