# Add project specific ProGuard rules here.

# Keep WebRTC classes
-keep class org.webrtc.** { *; }

# Keep TensorFlow Lite classes
-keep class org.tensorflow.lite.** { *; }

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }

# Keep data classes
-keepclassmembers class com.babymonitor.data.** { *; }
