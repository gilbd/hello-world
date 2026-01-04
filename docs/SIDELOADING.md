# Installing Without App Stores (Sideloading)

This guide explains how to install Baby Monitor directly on your devices without using the App Store or Play Store.

---

## Android: APK Sideloading

Android is very flexible with sideloading - you just need to enable it in settings.

### Method 1: Direct APK Install

#### Step 1: Build Debug APK

```bash
# Build debug APK (no signing required)
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

#### Step 2: Transfer APK to Device

**Option A: USB Transfer**
```bash
# Connect phone via USB and copy
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Option B: Share via Cloud/Email**
- Upload APK to Google Drive, Dropbox, or email it
- Download on phone

**Option C: Local Web Server**
```bash
# Start a simple HTTP server
cd app/build/outputs/apk/debug/
python -m http.server 8080

# On phone, browse to: http://YOUR_COMPUTER_IP:8080/app-debug.apk
```

#### Step 3: Enable Installation from Unknown Sources

On your Android device:

**Android 8.0+ (per-app permission):**
1. When you try to install, it will prompt you
2. Settings → Apps → Special access → Install unknown apps
3. Enable for the app you're installing from (Files, Chrome, etc.)

**Android 7.x and earlier:**
1. Settings → Security
2. Enable "Unknown sources"

#### Step 4: Install

1. Open the APK file on your phone
2. Tap "Install"
3. Wait for installation
4. Tap "Open"

### Method 2: Build Release APK (Self-Signed)

For better performance without Play Store:

```bash
# Create a debug keystore if you don't have one
keytool -genkey -v -keystore debug.keystore \
  -alias androiddebugkey \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass android \
  -keypass android \
  -dname "CN=Android Debug,O=Android,C=US"

# Build release APK
./gradlew assembleRelease

# Install via ADB
adb install app/build/outputs/apk/release/app-release.apk
```

### Method 3: Using ADB Wirelessly

```bash
# First, connect via USB and enable wireless debugging
adb tcpip 5555

# Disconnect USB, then connect wirelessly
adb connect YOUR_PHONE_IP:5555

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Updating the App

Simply install the new APK over the existing one:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
# -r flag allows reinstall/update
```

---

## iOS: Sideloading Methods

iOS is more restrictive, but there are several options.

### Method 1: Xcode (Mac Required) - FREE

Best for personal use. Apps last 7 days before re-signing needed.

#### Requirements
- Mac with Xcode installed
- Apple ID (free, no developer account needed)
- USB cable

#### Steps

1. **Connect iPhone to Mac**

2. **Open project in Xcode**
   ```bash
   open iosApp/BabyMonitor.xcodeproj
   ```

3. **Configure signing**
   - Select project → Target → Signing & Capabilities
   - Team: Select "Personal Team" (your Apple ID)
   - Bundle Identifier: Change to something unique, e.g., `com.yourname.babymonitor`

4. **Select your device**
   - In toolbar, select your connected iPhone

5. **Trust developer on iPhone**
   - First time: Settings → General → VPN & Device Management
   - Trust your Apple ID

6. **Build and run**
   - Click the Play button or `Cmd + R`
   - App installs directly to your iPhone

#### Limitations
- App expires after 7 days (free account)
- Must re-install weekly
- Limited to 3 apps per Apple ID
- Only 2 devices per Apple ID

### Method 2: Apple Developer Account ($99/year)

Apps last 1 year before re-signing.

Same as Method 1, but:
- Select your paid Developer Team
- Apps valid for 1 year
- No app limit
- Can share with up to 100 test devices via Ad Hoc

### Method 3: TestFlight (Requires Developer Account)

Best for sharing with family/friends.

1. Build and upload to App Store Connect (see PUBLISHING.md)

2. Go to App Store Connect → TestFlight

3. Add internal testers (up to 25, no review needed)
   - They install TestFlight app
   - Accept invitation via email

4. Or add external testers (up to 10,000)
   - Requires brief Apple review
   - More flexible distribution

### Method 4: Ad Hoc Distribution

For distributing to specific devices without TestFlight.

#### Step 1: Collect Device UDIDs

Each device needs its UDID registered:

```bash
# Connect device and run:
idevice_id -l
# or
xcrun xctrace list devices
```

Or via Finder/iTunes, click on Serial Number to show UDID.

#### Step 2: Register Devices

In Apple Developer portal:
1. Certificates, Identifiers & Profiles
2. Devices → Add (+)
3. Enter device name and UDID

#### Step 3: Create Ad Hoc Provisioning Profile

1. Profiles → Add (+)
2. Select "Ad Hoc"
3. Select your App ID
4. Select certificate
5. Select devices to include
6. Download profile

#### Step 4: Build IPA

In Xcode:
1. Product → Archive
2. Distribute App → Ad Hoc
3. Select profile
4. Export IPA file

#### Step 5: Install IPA

**Option A: Apple Configurator 2**
- Open Configurator
- Connect device
- Drag IPA onto device

**Option B: Finder (macOS Catalina+)**
- Connect device
- Open Finder → Select device
- Drag IPA to device window

**Option C: AltStore (No Mac Required)**
- Download AltStore: https://altstore.io
- Install AltServer on Windows/Mac
- Sideload apps from your phone
- Re-signs automatically every 7 days when on same WiFi

### Method 5: Enterprise Distribution (Expensive)

For organizations only:
- Apple Enterprise Developer Program: $299/year
- Must be a legitimate organization
- Allows distribution to any device
- Violating terms can result in certificate revocation

---

## Comparison Table

| Method | Platform | Cost | Duration | Devices | Complexity |
|--------|----------|------|----------|---------|------------|
| APK Sideload | Android | Free | Permanent | Unlimited | Easy |
| Xcode Personal | iOS | Free | 7 days | 2 | Medium |
| Xcode Developer | iOS | $99/yr | 1 year | 100 | Medium |
| TestFlight | iOS | $99/yr | 90 days | 10,000 | Medium |
| Ad Hoc | iOS | $99/yr | 1 year | 100 | Hard |
| AltStore | iOS | Free | 7 days* | Personal | Easy |

*AltStore auto-refreshes when on same WiFi as computer

---

## Recommended Approach

### For Personal Use
- **Android**: Just build APK and install
- **iOS**: Use Xcode with free Apple ID, re-install weekly

### For Family/Friends (< 5 people)
- **Android**: Share APK file
- **iOS**: TestFlight with Apple Developer account ($99/year)

### For Wider Distribution
- Publish to App Stores (see PUBLISHING.md)

---

## Troubleshooting

### Android

**"App not installed" error**
- Check if a different signed version is already installed
- Uninstall first, then reinstall

**"Parse error"**
- APK may be corrupted
- Re-build and transfer again

### iOS

**"Unable to install" error**
- Ensure device UDID is registered (if using Ad Hoc)
- Check bundle ID isn't already in use
- Try restarting Xcode and device

**"Untrusted Developer"**
- Settings → General → VPN & Device Management
- Trust the developer profile

**App crashes immediately**
- Check Xcode console for errors
- Verify provisioning profile matches bundle ID
