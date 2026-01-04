import SwiftUI
import UserNotifications

@main
struct BabyMonitorApp: App {
    @StateObject private var appState = AppState()
    @StateObject private var registry = CameraRegistry.shared
    @StateObject private var notificationManager = NotificationManager.shared

    init() {
        // Setup notification delegate
        UNUserNotificationCenter.current().delegate = NotificationManager.shared
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(appState)
                .environmentObject(registry)
                .environmentObject(notificationManager)
                .onAppear {
                    notificationManager.requestAuthorization()
                }
        }
    }
}

class AppState: ObservableObject {
    @Published var isMonitoring = false
    @Published var roomCode: String?
    @Published var settings = MonitorSettings()
    @Published var appMode: AppMode = .dashboard

    enum AppMode {
        case camera      // This device is a camera
        case dashboard   // This device is the admin dashboard
    }
}

struct MonitorSettings {
    var motionDetectionEnabled = true
    var motionSensitivity: Float = 0.5
    var cryDetectionEnabled = true
    var crySensitivity: Float = 0.5
    var volumeThreshold: Float = 0.15
    var notificationsEnabled = true
    var vibrationEnabled = true
    var videoResolution = "720p"
}
