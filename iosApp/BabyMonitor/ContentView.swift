import SwiftUI

struct ContentView: View {
    @State private var selectedTab: Tab = .home

    enum Tab {
        case home, camera, viewer, settings
    }

    var body: some View {
        NavigationStack {
            switch selectedTab {
            case .home:
                HomeView(selectedTab: $selectedTab)
            case .camera:
                CameraView(onBack: { selectedTab = .home })
            case .viewer:
                ViewerView(onBack: { selectedTab = .home })
            case .settings:
                SettingsView(onBack: { selectedTab = .home })
            }
        }
    }
}

#Preview {
    ContentView()
        .environmentObject(AppState())
}
