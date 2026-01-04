# Publishing to App Stores

This guide covers publishing Baby Monitor to the Google Play Store and Apple App Store.

---

## Android: Google Play Store

### Prerequisites

1. **Google Play Developer Account** ($25 one-time fee)
   - Sign up at: https://play.google.com/console/signup
   - Requires a Google account

2. **Signing Key** (Keystore file)
   - Required to sign your release APK/AAB

### Step 1: Create Signing Key

```bash
# Generate a new keystore
keytool -genkey -v -keystore baby-monitor.keystore \
  -alias baby-monitor \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# You'll be prompted for:
# - Keystore password
# - Key password
# - Your name, organization, location
```

**IMPORTANT**: Store this keystore file and passwords securely. You cannot update your app without them!

### Step 2: Configure Signing in Gradle

Create `keystore.properties` (don't commit this file!):

```properties
storeFile=../baby-monitor.keystore
storePassword=your_keystore_password
keyAlias=baby-monitor
keyPassword=your_key_password
```

Update `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val keystoreProperties = Properties()
            keystoreProperties.load(FileInputStream(keystorePropertiesFile))

            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### Step 3: Build Release Bundle

```bash
# Build Android App Bundle (recommended for Play Store)
./gradlew bundleRelease

# Output: app/build/outputs/bundle/release/app-release.aab
```

### Step 4: Create Play Store Listing

1. Go to [Google Play Console](https://play.google.com/console)
2. Click "Create app"
3. Fill in:
   - **App name**: Baby Monitor
   - **Default language**: English (US)
   - **App or game**: App
   - **Free or paid**: Free

4. Complete the **Store listing**:
   - Short description (80 chars)
   - Full description (4000 chars)
   - Screenshots (phone, tablet, optionally TV/Wear)
   - App icon (512x512 PNG)
   - Feature graphic (1024x500 PNG)
   - Privacy policy URL

5. Complete **Content rating** questionnaire

6. Set **Target audience and content**

7. Go to **App releases** → **Production** → **Create release**

8. Upload your `.aab` file

9. Submit for review (typically 1-7 days)

### Step 5: App Content Requirements

Required for approval:
- Privacy Policy (especially for camera/mic access)
- Data safety section filled out
- Contact email/address

---

## iOS: Apple App Store

### Prerequisites

1. **Apple Developer Account** ($99/year)
   - Enroll at: https://developer.apple.com/programs/
   - Requires an Apple ID

2. **Xcode** (Mac required)
   - Download from Mac App Store

3. **App Store Connect** access
   - https://appstoreconnect.apple.com

### Step 1: Configure Signing & Capabilities

1. Open `iosApp/BabyMonitor.xcodeproj` in Xcode

2. Select the project in navigator → Select target "BabyMonitor"

3. Go to **Signing & Capabilities**:
   - Team: Select your Apple Developer team
   - Bundle Identifier: `com.yourcompany.babymonitor`
   - Check "Automatically manage signing"

4. Add required capabilities:
   - Background Modes (Audio, VoIP)
   - Push Notifications (if using)

### Step 2: Create App in App Store Connect

1. Go to [App Store Connect](https://appstoreconnect.apple.com)

2. Click **My Apps** → **+** → **New App**

3. Fill in:
   - Platform: iOS
   - Name: Baby Monitor
   - Primary language: English (US)
   - Bundle ID: Select from dropdown
   - SKU: `baby-monitor-1`

### Step 3: Build for Distribution

In Xcode:

1. Select **Product** → **Archive** (device must be "Any iOS Device")

2. When archive completes, **Organizer** opens

3. Select archive → **Distribute App**

4. Choose **App Store Connect** → **Upload**

5. Follow prompts, accept defaults

### Step 4: Complete App Store Listing

In App Store Connect, fill in:

1. **Version Information**:
   - Screenshots (iPhone 6.5", 5.5"; iPad if supporting)
   - Promotional text
   - Description
   - Keywords
   - Support URL
   - Marketing URL (optional)

2. **App Review Information**:
   - Contact info
   - Demo account (if applicable)
   - Notes for reviewer

3. **Build**: Select the uploaded build

4. **Age Rating**: Complete questionnaire

5. **Pricing and Availability**: Set to Free, select countries

### Step 5: Submit for Review

1. Ensure all required fields are complete

2. Click **Submit for Review**

3. Review typically takes 24-48 hours

### Privacy Requirements

You MUST provide:
- Privacy Policy URL
- App Privacy details (data collection practices)

For Baby Monitor, disclose:
- Camera usage
- Microphone usage
- Network usage (streaming)
- Any analytics/crash reporting

---

## Timeline Summary

| Platform | Account Setup | First Submission | Review Time |
|----------|---------------|------------------|-------------|
| Android  | Same day      | 1-2 days prep    | 1-7 days    |
| iOS      | 1-2 days      | 2-3 days prep    | 1-2 days    |

---

## Tips for Approval

### Android
- Complete data safety section thoroughly
- Provide clear privacy policy
- Use Play App Signing (recommended)
- Test on multiple device sizes

### iOS
- Explain camera/mic usage clearly in permission dialogs
- Ensure app works without crashes
- Follow Human Interface Guidelines
- Test on various iPhone sizes
- Provide demo mode or clear instructions for review
