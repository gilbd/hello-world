import SwiftUI

struct SettingsView: View {
    let onBack: () -> Void
    @EnvironmentObject var appState: AppState

    var body: some View {
        Form {
            // Motion Detection Section
            Section {
                Toggle("Enable Motion Detection", isOn: $appState.settings.motionDetectionEnabled)

                if appState.settings.motionDetectionEnabled {
                    VStack(alignment: .leading) {
                        HStack {
                            Text("Sensitivity")
                            Spacer()
                            Text("\(Int(appState.settings.motionSensitivity * 100))%")
                                .foregroundColor(.secondary)
                        }
                        Slider(value: $appState.settings.motionSensitivity, in: 0...1)
                    }
                }
            } header: {
                Label("Motion Detection", systemImage: "eye.fill")
            }

            // Cry Detection Section
            Section {
                Toggle("Enable Cry Detection", isOn: $appState.settings.cryDetectionEnabled)

                if appState.settings.cryDetectionEnabled {
                    VStack(alignment: .leading) {
                        HStack {
                            Text("Sensitivity")
                            Spacer()
                            Text("\(Int(appState.settings.crySensitivity * 100))%")
                                .foregroundColor(.secondary)
                        }
                        Slider(value: $appState.settings.crySensitivity, in: 0...1)
                    }

                    VStack(alignment: .leading) {
                        HStack {
                            Text("Volume Threshold")
                            Spacer()
                            Text("\(Int(appState.settings.volumeThreshold * 100))%")
                                .foregroundColor(.secondary)
                        }
                        Slider(value: $appState.settings.volumeThreshold, in: 0.05...0.5)
                    }
                }
            } header: {
                Label("Cry Detection", systemImage: "mic.fill")
            }

            // Notifications Section
            Section {
                Toggle("Enable Notifications", isOn: $appState.settings.notificationsEnabled)

                if appState.settings.notificationsEnabled {
                    Toggle("Vibration", isOn: $appState.settings.vibrationEnabled)
                }
            } header: {
                Label("Notifications", systemImage: "bell.fill")
            }

            // Video Quality Section
            Section {
                Picker("Resolution", selection: $appState.settings.videoResolution) {
                    Text("480p").tag("480p")
                    Text("720p").tag("720p")
                    Text("1080p").tag("1080p")
                }
            } header: {
                Label("Video Quality", systemImage: "video.fill")
            }

            // About Section
            Section {
                HStack {
                    Text("Version")
                    Spacer()
                    Text("1.0.0")
                        .foregroundColor(.secondary)
                }
            } header: {
                Label("About", systemImage: "info.circle.fill")
            }
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Back") { onBack() }
            }
        }
    }
}

#Preview {
    NavigationStack {
        SettingsView(onBack: {})
    }
    .environmentObject(AppState())
}
