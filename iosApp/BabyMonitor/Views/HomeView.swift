import SwiftUI

struct HomeView: View {
    @Binding var selectedTab: ContentView.Tab

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            // Header
            VStack(spacing: 8) {
                Text("Welcome to Baby Monitor")
                    .font(.title)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)

                Text("Use your old phone as a baby camera with cry and motion detection")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }

            Spacer()

            // Mode Cards
            VStack(spacing: 16) {
                ModeCard(
                    title: "Camera Mode",
                    description: "Turn this phone into a baby camera that streams video and detects crying or movement",
                    icon: "camera.fill",
                    color: .purple
                ) {
                    selectedTab = .camera
                }

                ModeCard(
                    title: "Viewer Mode",
                    description: "Watch the live stream from your baby camera and receive alerts",
                    icon: "eye.fill",
                    color: .teal
                ) {
                    selectedTab = .viewer
                }
            }
            .padding(.horizontal)

            Spacer()
        }
        .navigationTitle("Baby Monitor")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    selectedTab = .settings
                } label: {
                    Image(systemName: "gearshape.fill")
                }
            }
        }
    }
}

struct ModeCard: View {
    let title: String
    let description: String
    let icon: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.system(size: 32))
                    .foregroundColor(color)
                    .frame(width: 48, height: 48)

                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline)
                        .foregroundColor(.primary)

                    Text(description)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(color.opacity(0.1))
            .cornerRadius(12)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

#Preview {
    NavigationStack {
        HomeView(selectedTab: .constant(.home))
    }
}
