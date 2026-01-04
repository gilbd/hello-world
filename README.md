# Baby Monitor

A secure Android app that turns your old phone into a smart baby monitor with real-time video streaming, cry detection, and motion alerts.

## Features

- **Camera Mode**: Transform any Android phone into a baby camera
  - Real-time video streaming via WebRTC
  - Night vision support (uses phone's flash)
  - Motion detection with configurable sensitivity
  - Baby cry detection using audio analysis
  - Secure peer-to-peer connection

- **Viewer Mode**: Watch the stream on another device
  - Enter room code to connect securely
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

## Architecture

```
com.babymonitor/
├── camera/           # CameraX integration
├── detection/        # Motion & cry detection algorithms
├── streaming/        # WebRTC streaming & signaling
├── notification/     # Alert notifications
├── service/          # Foreground monitoring service
├── data/             # Settings persistence
├── di/               # Hilt dependency injection
└── ui/               # Compose UI screens
    ├── screens/
    │   ├── home/     # Mode selection
    │   ├── camera/   # Camera streaming
    │   ├── viewer/   # Stream viewer
    │   └── settings/ # App configuration
    ├── navigation/   # Navigation setup
    └── theme/        # Material 3 theming
```

## Requirements

- Android 7.0 (API 24) or higher
- Camera permission
- Microphone permission
- Internet access for streaming

## Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build and run on your device

## Signaling Server

For peer-to-peer connection, you'll need a signaling server. Options:

1. **Local Development**: Run a simple WebSocket server
2. **Firebase Realtime Database**: Use as a signaling backend
3. **Custom Server**: Deploy your own WebSocket server

Example signaling server (Node.js):
```javascript
const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 8080 });

const rooms = new Map();

wss.on('connection', (ws, req) => {
  const url = new URL(req.url, 'http://localhost');
  const room = url.searchParams.get('room');

  if (!rooms.has(room)) {
    rooms.set(room, new Set());
  }
  rooms.get(room).add(ws);

  ws.on('message', (message) => {
    // Broadcast to other peers in the room
    rooms.get(room).forEach((client) => {
      if (client !== ws && client.readyState === WebSocket.OPEN) {
        client.send(message);
      }
    });
  });

  ws.on('close', () => {
    rooms.get(room)?.delete(ws);
  });
});
```

## Future Phases

- [ ] Multi-camera support (connect multiple phones)
- [ ] Recording & playback
- [ ] Two-way audio (talk to baby)
- [ ] Temperature/humidity sensor integration
- [ ] Cloud streaming option
- [ ] iOS companion app

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Camera**: CameraX
- **Streaming**: WebRTC
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **Storage**: DataStore

## License

MIT License
