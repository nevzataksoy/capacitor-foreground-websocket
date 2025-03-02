# capacitor-foreground-websocket

**capacitor-foreground-websocket** is a Capacitor plugin that provides a persistent WebSocket client running as a foreground service. It works on both Android and iOS, ensuring the WebSocket connection remains active regardless of window changes, backgrounding, or other apps being opened. This plugin is designed to integrate seamlessly with Quasar Framework applications.

## Features

- **Foreground Service:** Runs as a persistent foreground service on Android (displayed in the notification bar) and as a background task on iOS.
- **WebSocket Connection:** Connects to a specified IP and port using either `ws` or `wss` protocols.
- **Event Handling:** Emits `onopen`, `onmessage`, `onclose`, `onerror` events to the JavaScript layer.
- **Message Sending:** Supports sending messages from the Quasar app to the native layer via a `send` method.
- **Wake-Up Mechanism:** Automatically wakes up the main application when a message is received, using native mechanisms:
  - **Android:** Uses WakeLock and Intent to bring the MainActivity to the foreground.
  - **iOS:** Uses PushKit (VoIP push) to wake the app in the background.
- **Cross-Platform Compatibility:** Works on both Android and iOS.

## Installation

### 1. Install the Plugin

Install via npm:

```bash
npm install capacitor-foreground-websocket
npx cap sync
```

### Android
OkHttp: Used for managing the WebSocket connection.
Add the following dependency in your <strong>android/app/build.gradle</strong> file:
```groovy
dependencies {
    implementation 'com.squareup.okhttp3:okhttp:4.9.1'
    // other dependencies...
}
```
### iOS
No additional CocoaPod is required; however, configure background modes in your Info.plist as described below.

## Platform Specific Configuration
### Android
AndroidManifest.xml Modifications
In your <strong>android/app/src/main/AndroidManifest.xml</strong> file, add the following permissions and service declaration:
```xml
<manifest ... >
    <!-- Required Permissions -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

    <application ... >
        <!-- Foreground Service Declaration -->
        <service
            android:name="com.example.capacitorforegroundwebsocket.ForegroundWebSocketService"
            android:foregroundServiceType="dataSync" 
            android:enabled="true" 
            android:exported="false" 
            tools:replace="android:exported"/>
        ...
    </application>
</manifest>
```
### iOS
Info.plist Modifications
Add the following keys to your <strong>Info.plist</strong> file to enable background modes:
```xml
<key>UIBackgroundModes</key>
<array>
    <string>voip</string>
    <string>fetch</string>
</array>
```

### Sample Usage in a Quasar Framework Application
```javascript
// Import the plugin
import { CapacitorForegroundWebsocket } from 'capacitor-foreground-websocket'

// Define WebSocket options
const options = {
  ip: '192.168.1.100',
  port: 8080,
  isWss: false,
  title: 'Foreground WebSocket',
  description: 'Persistent connection running in foreground'
}

// Start the WebSocket service
CapacitorForegroundWebsocket.start(options)
  .then(() => console.log('WebSocket service started successfully'))
  .catch(err => console.error('Error starting WebSocket service:', err))

// Add event listeners for WebSocket events
CapacitorForegroundWebsocket.addListener('onopen', (data) => {
  console.log('WebSocket opened:', data)
})

CapacitorForegroundWebsocket.addListener('onmessage', (data) => {
  console.log('Received message:', data)
})

CapacitorForegroundWebsocket.addListener('onclose', (data) => {
  console.log('WebSocket closed:', data)
})

CapacitorForegroundWebsocket.addListener('onerror', (data) => {
  console.error('WebSocket error:', data)
})

CapacitorForegroundWebsocket.addListener('onpush', (data) => {
  console.log('Push notification received:', data)
})

CapacitorForegroundWebsocket.addListener('onpushToken', (data) => {
  console.log('Push token:', data)
})

// Function to send messages through the WebSocket
function sendMessage(message) {
  CapacitorForegroundWebsocket.send(message)
    .then(() => console.log('Message sent successfully'))
    .catch(err => console.error('Error sending message:', err))
}
```
### Contributing
Contributions are welcome! If you would like to contribute to this project, please open an issue or submit a pull request. All feedback and contributions are greatly appreciated.

### License
This project is licensed under the MIT License.
