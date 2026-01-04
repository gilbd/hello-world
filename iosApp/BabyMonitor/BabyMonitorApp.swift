import SwiftUI

@main
struct BabyMonitorApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
        }
    }
}

class AppState: ObservableObject {
    @Published var isMonitoring = false
    @Published var roomCode: String?
    @Published var settings = MonitorSettings()
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
