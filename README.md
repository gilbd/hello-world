# Baby Monitor

A cross-platform app (Android & iOS) that turns your old phone into a smart baby monitor with real-time video streaming, cry detection, and motion alerts.

## Platforms

| Platform | Status | Min Version |
|----------|--------|-------------|
| Android  | ✅ Ready | Android 7.0 (API 24) |
| iOS      | ✅ Ready | iOS 15.0+ |

## Features

- **Camera Mode**: Transform any phone into a baby camera
  - Real-time video streaming via WebRTC
  - Night vision support (uses phone's flash/torch)
  - Motion detection with configurable sensitivity
  - Baby cry detection using audio analysis
  - Secure peer-to-peer connection

- **Viewer Mode**: Watch the stream on another device
  - Enter 6-character room code to connect securely
  - Real-time alerts for crying or movement
  - Audio mute control
  - Connection status indicator

- **Smart Detection**
  - Motion detection using frame difference analysis
  - Cry detection using frequency analysis (250-600Hz baby cry range)
  - Configurable sensitivity for both detectors
  - Alert cooldown to prevent notification spam

- **Notifications**
  - Push notifications for cry and motion events
  - Vibration alerts
  - Persistent monitoring notification

## Project Structure

```
BabyMonitor/
├── app/                    # Android app (Kotlin + Jetpack Compose)
├── iosApp/                 # iOS app (Swift + SwiftUI)
├── shared/                 # Kotlin Multiplatform shared code
│   └── src/
│       ├── commonMain/     # Cross-platform code
│       │   └── kotlin/
│       │       ├── detection/    # Motion & cry detection engines
│       │       ├── streaming/    # Signaling & room codes
│       │       └── settings/     # Settings models
│       ├── androidMain/    # Android-specific implementations
│       └── iosMain/        # iOS-specific implementations
└── docs/                   # Documentation
    ├── PUBLISHING.md       # App Store & Play Store guide
    └── SIDELOADING.md      # Direct installation guide
```

## Quick Start

### Android

```bash
# Clone and build
git clone <repo-url>
cd BabyMonitor

# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### iOS

1. Open `iosApp/BabyMonitor.xcodeproj` in Xcode
2. Select your team in Signing & Capabilities
3. Connect iPhone and click Run (⌘R)

## Installation Options

| Method | Android | iOS |
|--------|---------|-----|
| **App Store** | [Publishing Guide](docs/PUBLISHING.md) | [Publishing Guide](docs/PUBLISHING.md) |
| **Direct Install** | [Sideloading Guide](docs/SIDELOADING.md) | [Sideloading Guide](docs/SIDELOADING.md) |

### Quickest Options

- **Android**: Build APK, transfer to phone, install (free, permanent)
- **iOS**: Connect to Mac with Xcode, build & run (free, 7-day expiry)

## Signaling Server

For peer-to-peer connection, you'll need a signaling server:

```javascript
// server.js - Simple Node.js signaling server
const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 8080 });

const rooms = new Map();

wss.on('connection', (ws, req) => {
  const url = new URL(req.url, 'http://localhost');
  const room = url.searchParams.get('room');

  if (!rooms.has(room)) rooms.set(room, new Set());
  rooms.get(room).add(ws);

  ws.on('message', (message) => {
    rooms.get(room).forEach((client) => {
      if (client !== ws && client.readyState === WebSocket.OPEN) {
        client.send(message);
      }
    });
  });

  ws.on('close', () => rooms.get(room)?.delete(ws));
});

console.log('Signaling server running on ws://localhost:8080');
```

Run with: `node server.js`

Alternatives:
- Firebase Realtime Database
- PubNub / Ably
- Your own WebSocket server

## Tech Stack

### Shared (Kotlin Multiplatform)
- Detection algorithms (motion, cry)
- Signaling message parsing
- Room code generation
- Settings models

### Android
- **UI**: Jetpack Compose + Material 3
- **Camera**: CameraX
- **Streaming**: WebRTC (stream-webrtc-android)
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **Storage**: DataStore

### iOS
- **UI**: SwiftUI
- **Camera**: AVFoundation
- **Audio**: AVAudioEngine + Accelerate
- **Streaming**: WebRTC (to be integrated)

## Roadmap

- [x] Android app with all features
- [x] iOS app with SwiftUI
- [x] Kotlin Multiplatform shared code
- [x] Installation documentation
- [ ] Multi-camera dashboard (multiple streams)
- [ ] Recording & playback
- [ ] Two-way audio (talk to baby)
- [ ] Cloud streaming option

## Requirements

### Android
- Android 7.0 (API 24) or higher
- Camera permission
- Microphone permission
- Internet access

### iOS
- iOS 15.0 or higher
- Camera permission
- Microphone permission
- Internet access

## License

MIT License
