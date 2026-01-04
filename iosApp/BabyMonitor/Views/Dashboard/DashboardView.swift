import SwiftUI

struct DashboardView: View {
    @ObservedObject var registry = CameraRegistry.shared
    @State private var showAddCamera = false
    @State private var showSettings = false
    @State private var selectedCamera: BabyCamera?
    @State private var showCameraDetail = false
    @State private var showAlerts = false

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
        NavigationStack {
            ZStack {
                if registry.cameras.isEmpty {
                    emptyState
                } else {
                    cameraGrid
                }
            }
            .navigationTitle("Baby Monitor")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button {
                        showSettings = true
                    } label: {
                        Image(systemName: "gearshape.fill")
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack(spacing: 16) {
                        // Alerts button with badge
                        Button {
                            showAlerts = true
                        } label: {
                            ZStack(alignment: .topTrailing) {
                                Image(systemName: "bell.fill")
                                if registry.unreadAlertCount > 0 {
                                    Text("\(min(registry.unreadAlertCount, 99))")
                                        .font(.caption2)
                                        .fontWeight(.bold)
                                        .foregroundColor(.white)
                                        .padding(4)
                                        .background(Color.red)
                                        .clipShape(Circle())
                                        .offset(x: 8, y: -8)
                                }
                            }
                        }

                        // Add camera button
                        Button {
                            showAddCamera = true
                        } label: {
                            Image(systemName: "plus.circle.fill")
                        }
                    }
                }
            }
            .sheet(isPresented: $showAddCamera) {
                AddCameraSheet()
            }
            .sheet(isPresented: $showSettings) {
                AdminSettingsView()
            }
            .sheet(isPresented: $showAlerts) {
                AlertsListView()
            }
            .sheet(item: $selectedCamera) { camera in
                CameraDetailView(camera: camera)
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 24) {
            Image(systemName: "video.badge.plus")
                .font(.system(size: 80))
                .foregroundColor(.secondary)

            Text("No Cameras Added")
                .font(.title2)
                .fontWeight(.bold)

            Text("Add your first baby camera to start monitoring")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Button {
                showAddCamera = true
            } label: {
                HStack {
                    Image(systemName: "plus")
                    Text("Add Camera")
                }
                .padding()
                .background(Color.accentColor)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
        }
    }

    private var cameraGrid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(registry.cameras) { camera in
                    CameraGridCell(camera: camera)
                        .onTapGesture {
                            selectedCamera = camera
                        }
                        .contextMenu {
                            Button {
                                selectedCamera = camera
                            } label: {
                                Label("View Full Screen", systemImage: "arrow.up.left.and.arrow.down.right")
                            }

                            Button {
                                registry.markAllAlertsAsRead(cameraId: camera.id)
                            } label: {
                                Label("Mark Alerts as Read", systemImage: "checkmark.circle")
                            }

                            Divider()

                            Button(role: .destructive) {
                                registry.removeCamera(id: camera.id)
                            } label: {
                                Label("Remove Camera", systemImage: "trash")
                            }
                        }
                }
            }
            .padding()
        }
        .refreshable {
            // Refresh camera connections
            await refreshCameras()
        }
    }

    private func refreshCameras() async {
        // Simulate refresh delay
        try? await Task.sleep(nanoseconds: 500_000_000)
        // In production, this would reconnect to cameras
    }
}

struct CameraGridCell: View {
    let camera: BabyCamera
    @ObservedObject var registry = CameraRegistry.shared

    var unreadCount: Int {
        camera.alerts.filter { !$0.isRead }.count
    }

    var body: some View {
        VStack(spacing: 0) {
            // Video preview area
            ZStack {
                // Placeholder/thumbnail
                if let thumbnailData = camera.thumbnailData,
                   let uiImage = UIImage(data: thumbnailData) {
                    Image(uiImage: uiImage)
                        .resizable()
                        .aspectRatio(16/9, contentMode: .fill)
                } else {
                    Rectangle()
                        .fill(Color.black)
                        .aspectRatio(16/9, contentMode: .fill)
                        .overlay(
                            Image(systemName: "video.fill")
                                .font(.largeTitle)
                                .foregroundColor(.gray)
                        )
                }

                // Status overlay
                VStack {
                    HStack {
                        // Connection status
                        HStack(spacing: 4) {
                            Circle()
                                .fill(camera.isConnected ? Color.green : Color.red)
                                .frame(width: 8, height: 8)
                            Text(camera.isConnected ? "LIVE" : "OFFLINE")
                                .font(.caption2)
                                .fontWeight(.bold)
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(.ultraThinMaterial)
                        .cornerRadius(8)

                        Spacer()

                        // Alert badge
                        if unreadCount > 0 {
                            HStack(spacing: 2) {
                                Image(systemName: "bell.fill")
                                    .font(.caption2)
                                Text("\(unreadCount)")
                                    .font(.caption2)
                                    .fontWeight(.bold)
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.red)
                            .cornerRadius(8)
                        }
                    }
                    .padding(8)

                    Spacer()

                    // Latest alert banner
                    if let latestAlert = camera.alerts.first(where: { !$0.isRead }) {
                        HStack {
                            Image(systemName: alertIcon(for: latestAlert.type))
                            Text(latestAlert.message)
                                .font(.caption)
                                .lineLimit(1)
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .frame(maxWidth: .infinity)
                        .background(alertColor(for: latestAlert.type).opacity(0.9))
                    }
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            // Camera name and info
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(camera.name)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .lineLimit(1)

                    Text(camera.roomCode)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                // Expand button
                Image(systemName: "arrow.up.left.and.arrow.down.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 8)
        }
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
    }

    private func alertIcon(for type: CameraAlert.AlertType) -> String {
        switch type {
        case .cry: return "waveform"
        case .motion: return "figure.walk"
        case .disconnected: return "wifi.slash"
        case .connected: return "wifi"
        }
    }

    private func alertColor(for type: CameraAlert.AlertType) -> Color {
        switch type {
        case .cry: return .red
        case .motion: return .orange
        case .disconnected: return .gray
        case .connected: return .green
        }
    }
}

#Preview {
    DashboardView()
}
