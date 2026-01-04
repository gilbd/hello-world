import SwiftUI

struct RootView: View {
    @EnvironmentObject var appState: AppState
    @EnvironmentObject var registry: CameraRegistry

    @State private var showPINEntry = false
    @State private var hasCheckedPIN = false

    var body: some View {
        Group {
            if !hasCheckedPIN && registry.hasAdminPIN && !registry.isAdminMode {
                // Show PIN entry if PIN is set and not authenticated
                PINEntryView(mode: .unlock) { pin in
                    registry.isAdminMode = true
                    hasCheckedPIN = true
                }
            } else {
                // Main content
                mainContent
            }
        }
        .onAppear {
            if !registry.hasAdminPIN {
                hasCheckedPIN = true
            }
        }
    }

    @ViewBuilder
    private var mainContent: some View {
        switch appState.appMode {
        case .dashboard:
            DashboardView()
        case .camera:
            CameraHostView()
        }
    }
}

/// View for when this device is acting as a camera
struct CameraHostView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var cameraManager = CameraManager()
    @StateObject private var audioAnalyzer = AudioAnalyzer()

    var body: some View {
        NavigationStack {
            CameraView(onBack: {
                appState.appMode = .dashboard
            })
        }
    }
}

#Preview {
    RootView()
        .environmentObject(AppState())
        .environmentObject(CameraRegistry.shared)
        .environmentObject(NotificationManager.shared)
}
