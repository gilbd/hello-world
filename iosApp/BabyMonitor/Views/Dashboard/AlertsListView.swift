import SwiftUI

struct AlertsListView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var registry = CameraRegistry.shared

    var allAlerts: [(camera: BabyCamera, alert: CameraAlert)] {
        registry.cameras.flatMap { camera in
            camera.alerts.map { (camera: camera, alert: $0) }
        }
        .sorted { $0.alert.timestamp > $1.alert.timestamp }
    }

    var body: some View {
        NavigationStack {
            Group {
                if allAlerts.isEmpty {
                    emptyState
                } else {
                    alertsList
                }
            }
            .navigationTitle("Alerts")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Close") {
                        dismiss()
                    }
                }

                if !allAlerts.isEmpty {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Menu {
                            Button {
                                markAllAsRead()
                            } label: {
                                Label("Mark All as Read", systemImage: "checkmark.circle")
                            }

                            Button(role: .destructive) {
                                clearAllAlerts()
                            } label: {
                                Label("Clear All Alerts", systemImage: "trash")
                            }
                        } label: {
                            Image(systemName: "ellipsis.circle")
                        }
                    }
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "bell.slash")
                .font(.system(size: 60))
                .foregroundColor(.secondary)

            Text("No Alerts")
                .font(.title2)
                .fontWeight(.semibold)

            Text("You'll see alerts here when motion or crying is detected")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
    }

    private var alertsList: some View {
        List {
            ForEach(groupedByDay.keys.sorted().reversed(), id: \.self) { dateKey in
                Section {
                    ForEach(groupedByDay[dateKey] ?? [], id: \.alert.id) { item in
                        AlertRow(camera: item.camera, alert: item.alert)
                            .onTapGesture {
                                registry.markAlertAsRead(
                                    cameraId: item.camera.id,
                                    alertId: item.alert.id
                                )
                            }
                    }
                } header: {
                    Text(formatDateHeader(dateKey))
                }
            }
        }
        .listStyle(.insetGrouped)
    }

    private var groupedByDay: [Date: [(camera: BabyCamera, alert: CameraAlert)]] {
        Dictionary(grouping: allAlerts) { item in
            Calendar.current.startOfDay(for: item.alert.timestamp)
        }
    }

    private func formatDateHeader(_ date: Date) -> String {
        if Calendar.current.isDateInToday(date) {
            return "Today"
        } else if Calendar.current.isDateInYesterday(date) {
            return "Yesterday"
        } else {
            let formatter = DateFormatter()
            formatter.dateStyle = .medium
            return formatter.string(from: date)
        }
    }

    private func markAllAsRead() {
        for camera in registry.cameras {
            registry.markAllAlertsAsRead(cameraId: camera.id)
        }
    }

    private func clearAllAlerts() {
        for camera in registry.cameras {
            registry.clearAlerts(cameraId: camera.id)
        }
    }
}

struct AlertRow: View {
    let camera: BabyCamera
    let alert: CameraAlert

    var body: some View {
        HStack(spacing: 12) {
            // Alert type icon
            ZStack {
                Circle()
                    .fill(alertColor.opacity(0.2))
                    .frame(width: 44, height: 44)

                Image(systemName: alertIcon)
                    .font(.system(size: 18))
                    .foregroundColor(alertColor)
            }

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(camera.name)
                        .font(.subheadline)
                        .fontWeight(.semibold)

                    if !alert.isRead {
                        Circle()
                            .fill(Color.blue)
                            .frame(width: 8, height: 8)
                    }
                }

                Text(alert.message)
                    .font(.subheadline)
                    .foregroundColor(.primary)

                Text(formatTime(alert.timestamp))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()
        }
        .padding(.vertical, 4)
        .opacity(alert.isRead ? 0.7 : 1.0)
    }

    private var alertIcon: String {
        switch alert.type {
        case .cry: return "waveform"
        case .motion: return "figure.walk"
        case .disconnected: return "wifi.slash"
        case .connected: return "wifi"
        }
    }

    private var alertColor: Color {
        switch alert.type {
        case .cry: return .red
        case .motion: return .orange
        case .disconnected: return .gray
        case .connected: return .green
        }
    }

    private func formatTime(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}

#Preview {
    AlertsListView()
}
