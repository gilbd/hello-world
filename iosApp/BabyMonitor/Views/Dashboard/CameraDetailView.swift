import SwiftUI

struct CameraDetailView: View {
    let camera: BabyCamera

    @Environment(\.dismiss) private var dismiss
    @ObservedObject var registry = CameraRegistry.shared
    @State private var isMuted = false
    @State private var showRenameAlert = false
    @State private var newName = ""

    var body: some View {
        NavigationStack {
            ZStack {
                // Full screen video view
                Color.black
                    .ignoresSafeArea()

                VStack {
                    // Status bar
                    statusBar
                        .padding(.top, 8)

                    Spacer()

                    // Placeholder for video
                    if let thumbnailData = camera.thumbnailData,
                       let uiImage = UIImage(data: thumbnailData) {
                        Image(uiImage: uiImage)
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                    } else {
                        VStack(spacing: 16) {
                            Image(systemName: camera.isConnected ? "video.fill" : "video.slash.fill")
                                .font(.system(size: 60))
                                .foregroundColor(.gray)

                            Text(camera.isConnected ? "Connecting to stream..." : "Camera Offline")
                                .foregroundColor(.gray)
                        }
                    }

                    Spacer()

                    // Controls
                    controlBar
                        .padding(.bottom, 20)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title2)
                            .foregroundColor(.white.opacity(0.8))
                    }
                }

                ToolbarItem(placement: .principal) {
                    VStack(spacing: 2) {
                        Text(camera.name)
                            .font(.headline)
                            .foregroundColor(.white)
                        Text(camera.roomCode)
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.7))
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button {
                            newName = camera.name
                            showRenameAlert = true
                        } label: {
                            Label("Rename", systemImage: "pencil")
                        }

                        Button {
                            registry.clearAlerts(cameraId: camera.id)
                        } label: {
                            Label("Clear Alerts", systemImage: "bell.slash")
                        }

                        Divider()

                        Button(role: .destructive) {
                            registry.removeCamera(id: camera.id)
                            dismiss()
                        } label: {
                            Label("Remove Camera", systemImage: "trash")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle.fill")
                            .font(.title2)
                            .foregroundColor(.white.opacity(0.8))
                    }
                }
            }
            .toolbarBackground(.hidden, for: .navigationBar)
            .alert("Rename Camera", isPresented: $showRenameAlert) {
                TextField("Camera Name", text: $newName)
                Button("Cancel", role: .cancel) {}
                Button("Save") {
                    if !newName.trimmingCharacters(in: .whitespaces).isEmpty {
                        registry.renameCamera(id: camera.id, newName: newName)
                    }
                }
            }
        }
    }

    private var statusBar: some View {
        HStack(spacing: 16) {
            // Live indicator
            HStack(spacing: 6) {
                Circle()
                    .fill(camera.isConnected ? Color.red : Color.gray)
                    .frame(width: 10, height: 10)
                Text(camera.isConnected ? "LIVE" : "OFFLINE")
                    .font(.caption)
                    .fontWeight(.bold)
            }
            .foregroundColor(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(.ultraThinMaterial)
            .cornerRadius(20)

            Spacer()

            // Alert count
            if !camera.alerts.isEmpty {
                let unreadCount = camera.alerts.filter { !$0.isRead }.count
                HStack(spacing: 4) {
                    Image(systemName: "bell.fill")
                        .font(.caption)
                    Text("\(unreadCount) new")
                        .font(.caption)
                        .fontWeight(.medium)
                }
                .foregroundColor(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(unreadCount > 0 ? Color.red : Color.gray.opacity(0.5))
                .cornerRadius(20)
            }
        }
        .padding(.horizontal)
    }

    private var controlBar: some View {
        HStack(spacing: 40) {
            // Mute button
            ControlButton(
                icon: isMuted ? "speaker.slash.fill" : "speaker.wave.2.fill",
                label: isMuted ? "Unmute" : "Mute"
            ) {
                isMuted.toggle()
            }

            // Snapshot button
            ControlButton(
                icon: "camera.fill",
                label: "Snapshot"
            ) {
                // Take snapshot
            }

            // Talk button (future feature)
            ControlButton(
                icon: "mic.fill",
                label: "Talk",
                disabled: true
            ) {
                // Two-way audio
            }

            // Night vision
            ControlButton(
                icon: "moon.fill",
                label: "Night"
            ) {
                // Toggle night vision
            }
        }
        .padding()
        .background(.ultraThinMaterial)
        .cornerRadius(20)
        .padding(.horizontal)
    }
}

struct ControlButton: View {
    let icon: String
    let label: String
    var disabled: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.title2)
                Text(label)
                    .font(.caption2)
            }
            .foregroundColor(disabled ? .gray : .white)
        }
        .disabled(disabled)
    }
}

#Preview {
    CameraDetailView(camera: BabyCamera(
        name: "Nursery",
        roomCode: "ABC123",
        isConnected: true
    ))
}
