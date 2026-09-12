# 131 Red Player

The repository now contains a native Android application and the earlier web player.

## Android app

The Android app uses Media3 for real local playback. It includes:

- Multi-video picker with access to phone storage, Google Drive, and other Android file providers
- Hardware-accelerated playback with Media3/ExoPlayer
- 4K Ultra HD mode with highest-quality adaptive track selection and automatic 4K badge
- Persistent playlist access and resume positions
- Playback speed from 0.25× to 8×
- Two-finger pinch zoom up to 5× with reset
- Frame stepping, jump-to-time, Quick Mute, video filters, subtitle styling, preferred languages and equalizer presets
- Embedded subtitle-track selection with a subtitles-off choice
- Repeat off, repeat one video, or repeat all videos
- Configurable 5, 10, 15, 30, or 60 second skip controls
- Automatic, portrait, landscape, and reverse screen orientations
- Custom sleep timer, timer cancellation, and stop-after-current-video
- Drag to reposition the picture while pinch-zoomed and saved playback brightness
- Local SRT and WebVTT subtitles
- A–B repeat
- Picture-in-picture
- Brightness, volume, and seeking gestures
- Fit, fill-screen, and stretch modes
- Multiple audio-track selection
- Search, playlist, repeat, shuffle, sleep timer, screen-awake mode, rotation, and video information
- Fingerprint/device-lock protected private picker
- Open-with support from other Android applications

Every push that changes the Android project runs the **Build Android APK** workflow. Download the finished file from the workflow artifact named **131-Red-Player-APK**.

Private, ad-free, installable video player for phones and computers.

## Working

- Local videos and folders
- Complete on-device music library with song, artist and album search
- Local and internet M3U/M3U8 playlist importing
- Android TV compatible manifest and remote-friendly non-touch support
- Recoverable 30-day video Trash with restore and permanent delete
- Sort by name, size, duration or newest
- Expanded VLC network routing for SFTP, NFS, UDP, RTP, MMS and UPnP addresses
- Playlist and automatic next video
- Resume position
- Seek, skip, volume, speed, fullscreen
- A–B repeat
- Repeat one, repeat all, and shuffle
- Search, sorting, and saved favorites
- Swipe left or right to skip
- Inside, fill-screen, and stretch views
- Sleep timer and optional screen-awake mode
- Android lock-screen and headphone controls
- WebVTT subtitles
- Picture-in-picture when the browser supports it
- Control lock
- Offline app shell and Android install prompt

## Honest limits

The native Android app and the earlier browser player are both in this repository. Browser format support still depends on the browser. The Android app can open files exposed by Android's document providers, but it does not include a separate Google Photos sign-in. "4K Ultra HD mode" selects the highest available source track; it does not claim true AI upscaling.

## Open the app

After GitHub Pages finishes publishing, open:

https://dame131.github.io/redpro-video-player/
