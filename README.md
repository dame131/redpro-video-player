# 131 Red Player

The repository now contains a native Android application and the earlier web player.

## Android app

The Android app uses Media3 for real local playback. It includes:

- Multi-video picker with access to phone storage, Google Drive, and other Android file providers
- Hardware-accelerated playback with Media3/ExoPlayer
- 4K Ultra HD mode with highest-quality adaptive track selection and automatic 4K badge
- Persistent playlist access and resume positions
- Playback speed from 0.25× to 3×
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

This is a web app, not an Android APK project. Browser support controls video formats. Videos never upload. Google Drive, Google Photos, casting, biometric vault storage, and true AI upscaling are not included.

## Open the app

After GitHub Pages finishes publishing, open:

https://dame131.github.io/redpro-video-player/
