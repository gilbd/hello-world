# Tuya SmartLife Integration Setup Guide

This guide explains how to set up the Baby Monitor app to work with Tuya SmartLife, allowing your phone to appear as a smart camera in the SmartLife app.

## Overview

The integration allows:
- Your phone to appear as a smart camera in SmartLife
- Live video streaming to SmartLife app viewers
- Remote control of motion/cry detection from SmartLife
- Push notifications through Tuya's infrastructure
- Two-way audio communication

## Prerequisites

- Tuya Developer Account
- SmartLife app installed on viewer devices
- Android phone running the Baby Monitor app

## Step 1: Create Tuya Developer Account

1. Go to [Tuya IoT Platform](https://developer.tuya.com)
2. Click "Register" and create an account
3. Verify your email address

## Step 2: Create a Cloud Project

1. Log into the Tuya IoT Platform
2. Go to **Cloud** → **Development** → **Create Cloud Project**
3. Fill in project details:
   - **Project Name**: "Baby Monitor" (or your choice)
   - **Industry**: Smart Home
   - **Development Method**: Smart Home PaaS
   - **Data Center**: Choose based on your region:
     - Americas: Western America
     - Europe: Central Europe
     - Asia: China (or India for India)
4. Click **Create**

## Step 3: Create an IPC Product

1. Go to **Product** → **Development** → **Create Product**
2. Select product category:
   - **Category**: Security & Video Surveillance
   - **Product**: Smart Camera (IPC)
3. Configure product:
   - **Product Name**: "Baby Monitor Camera"
   - **Protocol**: WiFi
   - **Power Type**: Mains Power (or Battery if applicable)
4. Click **Create**

## Step 4: Get Your Credentials

### Product Credentials
1. Go to **Product** → select your product
2. Find and copy:
   - **Product ID (PID)**: Found in product overview
   - **Product Key**: Found in device development section

### Cloud Project Credentials
1. Go to **Cloud** → **Development** → select your project
2. Click on **Overview** tab
3. Find and copy:
   - **Access ID**: Your client ID
   - **Access Secret**: Your client secret (click to reveal)

## Step 5: Configure the App

1. Open the Baby Monitor app
2. Go to **Settings** → **Smart Home Integration** → **Tuya SmartLife**
3. Enter your credentials:
   - Access ID
   - Access Secret
   - Product ID
   - Product Key (optional)
4. Select your region
5. Tap **Save Configuration**

## Step 6: Pair with SmartLife

### QR Code Pairing (Recommended)

1. In Baby Monitor app, tap **QR Code Pairing**
2. A QR code will appear on screen
3. Open SmartLife app on your viewing device
4. Tap **+** → **Add Device** → **Scan QR Code**
5. Scan the QR code displayed on the camera phone
6. Wait for pairing to complete

### AP Mode Pairing (Alternative)

1. In Baby Monitor app, tap **AP Mode Pairing**
2. The phone will create a WiFi hotspot
3. Open SmartLife app on your viewing device
4. Tap **+** → **Add Device** → **Security & Video** → **Smart Camera**
5. Select **AP Mode**
6. Connect to "BabyMonitor_AP" WiFi network
7. Follow the in-app instructions

## Step 7: Verify Connection

After pairing:
1. The Baby Monitor app should show "Connected" status
2. Open SmartLife app
3. Your camera should appear in the device list
4. Tap on it to view the live stream

## Features Available in SmartLife

Once paired, SmartLife viewers can:

| Feature | SmartLife Control |
|---------|------------------|
| Live Video | View real-time camera feed |
| Two-Way Audio | Talk through the camera |
| Motion Detection | Enable/disable and adjust sensitivity |
| Cry Detection | Enable/disable and adjust sensitivity |
| Night Vision | Auto/On/Off modes |
| Privacy Mode | Turn camera on/off |
| Alerts | Receive push notifications |
| Recording | View recorded clips (if cloud storage enabled) |

## Tuya Data Points (DPs)

The app uses these Tuya data points for communication:

| DP ID | Feature | Type | Values |
|-------|---------|------|--------|
| 101 | Online Status | Boolean | true/false |
| 103 | Motion Detection | Boolean | true/false |
| 104 | Cry Detection | Boolean | true/false |
| 106 | Night Vision | Integer | 0=Auto, 1=On, 2=Off |
| 108 | Recording Status | Boolean | true/false |
| 109 | Battery Level | Integer | 0-100 |
| 119 | PTZ Control | String | up/down/left/right/stop |
| 150 | Privacy Mode | Boolean | true/false |

## Troubleshooting

### Pairing Failed

1. Ensure credentials are entered correctly
2. Check internet connection on both devices
3. Verify the Product ID matches an IPC product
4. Try AP mode if QR code fails

### Connection Drops

1. Check WiFi signal strength
2. Ensure the phone isn't entering sleep mode
3. Disable battery optimization for the app
4. Check Tuya server status

### Video Not Loading in SmartLife

1. Wait 30 seconds after pairing for video initialization
2. Check if privacy mode is enabled (disable it)
3. Verify both devices have internet access
4. Try force-closing and reopening SmartLife

### "Device Offline" in SmartLife

1. Ensure Baby Monitor app is running in foreground
2. Check if foreground service notification is visible
3. Restart the Baby Monitor app
4. Re-pair if issue persists

## Security Considerations

1. **Credentials**: Store your Tuya credentials securely. The app encrypts them on device.

2. **Network**: Use a secure WiFi network. Consider a dedicated IoT network.

3. **Access Control**: Only share SmartLife family access with trusted people.

4. **Updates**: Keep the app updated for security patches.

## Region-Specific Notes

### China
- Use `m1.tuyacn.com` MQTT endpoint
- Use `openapi.tuyacn.com` API endpoint
- Data stored in Chinese data centers

### Americas (US)
- Use `m1.tuyaus.com` MQTT endpoint
- Use `openapi.tuyaus.com` API endpoint
- Data stored in US data centers

### Europe
- Use `m1.tuyaeu.com` MQTT endpoint
- Use `openapi.tuyaeu.com` API endpoint
- GDPR compliant data handling

### India
- Use `m1.tuyain.com` MQTT endpoint
- Use `openapi.tuyain.com` API endpoint
- Data stored in Indian data centers

## API Rate Limits

Tuya imposes rate limits on API calls:
- Cloud API: 500 calls/minute
- MQTT messages: 50 messages/second
- P2P connections: 3 concurrent viewers

## Support

- Tuya Developer Forums: https://developer.tuya.com/en/community
- SmartLife Support: Available in the app
- Baby Monitor Issues: https://github.com/your-repo/issues
