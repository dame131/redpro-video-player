package com.mr131.redplayer;

import android.Manifest;
import android.content.ContentUris;
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
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Device-local songs, artists, albums and genres browser. */
public final class AudioLibraryActivity extends AppCompatActivity {
    private enum BrowseMode { SONGS, ARTISTS, ALBUMS, GENRES }

    private final ArrayList<Track> all = new ArrayList<>();
    private final ArrayList<Row> rows = new ArrayList<>();
    private RowAdapter adapter;
    private TextView count;
    private BrowseMode mode = BrowseMode.SONGS;
    private String selectedGroup;
    private String query = "";

    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), ok -> {
                if (ok) load(); else count.setText("Music permission is off");
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(10), dp(14), dp(10));
        RedPlayerBackground.apply(root);
        root.addView(LuxuryIconView.create(this, R.drawable.icon_playlist_thick, "Music Library emblem"));
        TextView title = text("MUSIC LIBRARY", 25, Color.WHITE);
        title.setContentDescription("Music Library Screen");
        root.addView(title);
        count = text("Scanning music…", 13, Color.LTGRAY);
        root.addView(count);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setGravity(Gravity.CENTER);
        addTab(tabs, "Songs", BrowseMode.SONGS);
        addTab(tabs, "Artists", BrowseMode.ARTISTS);
        addTab(tabs, "Albums", BrowseMode.ALBUMS);
        addTab(tabs, "Genres", BrowseMode.GENRES);
        root.addView(tabs, new LinearLayout.LayoutParams(-1, dp(48)));

        SearchView search = new SearchView(this);
        search.setQueryHint("Search music");
        search.setIconifiedByDefault(false);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String value) { filter(value); return true; }
            @Override public boolean onQueryTextChange(String value) { filter(value); return true; }
        });
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(56)));

        ListView list = new ListView(this);
        adapter = new RowAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> open(rows.get(position)));
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        request();
    }

    private void addTab(LinearLayout tabs, String label, BrowseMode target) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(11);
        button.setAllCaps(false);
        button.setOnClickListener(view -> { mode = target; selectedGroup = null; rebuild(); });
        tabs.addView(button, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void request() {
        String required = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, required) == PackageManager.PERMISSION_GRANTED) load();
        else permission.launch(required);
    }

    private void load() {
        new Thread(() -> {
            Map<Long, String> genres = readGenres();
            ArrayList<Track> found = new ArrayList<>();
            String[] columns = {
                    MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.TRACK,
                    MediaStore.Audio.Media.ALBUM_ID
            };
            try (Cursor cursor = getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns,
                    MediaStore.Audio.Media.IS_MUSIC + "!=0", null,
                    MediaStore.Audio.Media.TITLE + " COLLATE NOCASE")) {
                if (cursor != null) while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    long albumId = cursor.getLong(6);
                    Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    Uri art = albumId > 0 ? ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"), albumId) : null;
                    int rawTrack = cursor.getInt(5);
                    int trackNumber = rawTrack > 999 ? rawTrack % 1000 : rawTrack;
                    found.add(new Track(uri, art,
                            safe(cursor.getString(1), "Unknown track"),
                            safe(cursor.getString(2), "Unknown artist"),
                            safe(cursor.getString(3), "Unknown album"),
                            safe(genres.get(id), "Unknown genre"), cursor.getLong(4), trackNumber));
                }
            } catch (RuntimeException ignored) { }
            runOnUiThread(() -> { all.clear(); all.addAll(found); rebuild(); });
        }, "music-scan").start();
    }

    private Map<Long, String> readGenres() {
        Map<Long, String> result = new LinkedHashMap<>();
        try (Cursor genres = getContentResolver().query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME}, null, null, null)) {
            if (genres == null) return result;
            while (genres.moveToNext()) {
                long genreId = genres.getLong(0);
                String genre = safe(genres.getString(1), "Unknown genre");
                Uri members = MediaStore.Audio.Genres.Members.getContentUri("external", genreId);
                try (Cursor songs = getContentResolver().query(members,
                        new String[]{MediaStore.Audio.Genres.Members.AUDIO_ID}, null, null, null)) {
                    if (songs != null) while (songs.moveToNext()) result.put(songs.getLong(0), genre);
                } catch (RuntimeException ignored) { }
            }
        } catch (RuntimeException ignored) { }
        return result;
    }

    private void filter(String value) {
        query = value == null ? "" : value.trim().toLowerCase(Locale.US);
        rebuild();
    }

    private void rebuild() {
        rows.clear();
        if (mode == BrowseMode.SONGS || selectedGroup != null) {
            for (Track track : all) {
                if ((selectedGroup == null || selectedGroup.equals(groupValue(track))) && matches(track))
                    rows.add(Row.song(track));
            }
        } else {
            LinkedHashMap<String, Integer> groups = new LinkedHashMap<>();
            for (Track track : all) {
                String value = groupValue(track);
                if (query.isEmpty() || value.toLowerCase(Locale.US).contains(query))
                    groups.put(value, groups.getOrDefault(value, 0) + 1);
            }
            for (Map.Entry<String, Integer> entry : groups.entrySet())
                rows.add(Row.group(entry.getKey(), entry.getValue()));
        }
        adapter.notifyDataSetChanged();
        String heading = selectedGroup != null ? selectedGroup : mode.name().toLowerCase(Locale.US);
        count.setText(rows.size() + (selectedGroup != null || mode == BrowseMode.SONGS ? " songs • " : " groups • ") + heading);
    }

    private boolean matches(Track track) {
        if (query.isEmpty()) return true;
        return (track.title + " " + track.artist + " " + track.album + " " + track.genre)
                .toLowerCase(Locale.US).contains(query);
    }

    private String groupValue(Track track) {
        if (mode == BrowseMode.ARTISTS) return track.artist;
        if (mode == BrowseMode.ALBUMS) return track.album;
        if (mode == BrowseMode.GENRES) return track.genre;
        return "Songs";
    }

    private void open(Row row) {
        if (row.track == null) {
            selectedGroup = row.group;
            rebuild();
            return;
        }
        Track track = row.track;
        Intent result = new Intent().setData(track.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        result.putExtra("title", track.title);
        result.putExtra("artist", track.artist);
        result.putExtra("album", track.album);
        result.putExtra("genre", track.genre);
        result.putExtra("track_number", track.trackNumber);
        if (track.artwork != null) result.putExtra("artwork", track.artwork.toString());
        setResult(RESULT_OK, result);
        finish();
    }

    @Override public void onBackPressed() {
        if (selectedGroup != null) { selectedGroup = null; rebuild(); }
        else super.onBackPressed();
    }

    private final class RowAdapter extends ArrayAdapter<Row> {
        RowAdapter() { super(AudioLibraryActivity.this, android.R.layout.simple_list_item_1, rows); }

        @NonNull @Override public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            Row row = getItem(position);
            LinearLayout line = new LinearLayout(AudioLibraryActivity.this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(dp(5), dp(5), dp(5), dp(5));
            ImageView art = new ImageView(AudioLibraryActivity.this);
            art.setScaleType(ImageView.ScaleType.CENTER_CROP);
            art.setImageResource(R.drawable.icon_playlist_thick);
            if (row != null && row.track != null && row.track.artwork != null) {
                try { art.setImageURI(row.track.artwork); } catch (RuntimeException ignored) { }
            }
            line.addView(art, new LinearLayout.LayoutParams(dp(58), dp(58)));
            TextView label = text(row == null ? "" : row.label(), 15, Color.WHITE);
            label.setGravity(Gravity.CENTER_VERTICAL);
            label.setPadding(dp(12), 0, 0, 0);
            line.addView(label, new LinearLayout.LayoutParams(0, dp(70), 1));
            return line;
        }
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() || "<unknown>".equalsIgnoreCase(value)
                ? fallback : value;
    }
    private TextView text(String value, int size, int color) { TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); view.setGravity(Gravity.CENTER); view.setPadding(0, dp(7), 0, dp(7)); return view; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static final class Row {
        final Track track; final String group; final int songCount;
        private Row(Track track, String group, int songCount) { this.track = track; this.group = group; this.songCount = songCount; }
        static Row song(Track track) { return new Row(track, null, 0); }
        static Row group(String name, int count) { return new Row(null, name, count); }
        String label() {
            if (track == null) return group + "\n" + songCount + (songCount == 1 ? " song" : " songs");
            String number = track.trackNumber > 0 ? track.trackNumber + ". " : "";
            return number + track.title + "\n" + track.artist + " • " + track.album + " • " + track.genre + " • " + track.time();
        }
    }

    private static final class Track {
        final Uri uri, artwork; final String title, artist, album, genre; final long duration; final int trackNumber;
        Track(Uri uri, Uri artwork, String title, String artist, String album, String genre, long duration, int trackNumber) {
            this.uri = uri; this.artwork = artwork; this.title = title; this.artist = artist;
            this.album = album; this.genre = genre; this.duration = duration; this.trackNumber = trackNumber;
        }
        String time() { long seconds = duration / 1000; return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60); }
    }
}
