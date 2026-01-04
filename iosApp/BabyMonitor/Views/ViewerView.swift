import SwiftUI

struct ViewerView: View {
    let onBack: () -> Void

    @State private var roomCode = ""
    @State private var isConnecting = false
    @State private var isConnected = false
    @State private var isMuted = false
    @State private var errorMessage: String?
    @State private var hasAlert = false
    @State private var alertMessage = ""
    @State private var alertType = ""

    var body: some View {
        ZStack {
            if !isConnected && !isConnecting {
                // Room code entry
                roomCodeEntryView
            } else if isConnecting {
                // Connecting state
                connectingView
            } else {
                // Connected - show stream
                streamView
            }
        }
        .navigationTitle("Viewer Mode")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Back") {
                    disconnect()
                    onBack()
                }
            }
            if isConnected {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        isMuted.toggle()
                    } label: {
                        Image(systemName: isMuted ? "speaker.slash.fill" : "speaker.wave.2.fill")
                    }
                }
            }
        }
        .alert("Connection Error", isPresented: .constant(errorMessage != nil)) {
            Button("Retry") { connect() }
            Button("Cancel", role: .cancel) { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "Unknown error")
        }
    }

    private var roomCodeEntryView: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "qrcode.viewfinder")
                .font(.system(size: 72))
                .foregroundColor(.accentColor)

            Text("Enter Room Code")
                .font(.title2)
                .fontWeight(.bold)

            Text("Enter the 6-character code from the camera device")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            TextField("ABCD12", text: $roomCode)
                .font(.system(size: 32, weight: .bold, design: .monospaced))
                .multilineTextAlignment(.center)
                .textInputAutocapitalization(.characters)
                .frame(width: 200)
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
                .onChange(of: roomCode) { newValue in
                    roomCode = String(newValue.uppercased().prefix(6))
                }

            Button {
                connect()
            } label: {
                HStack {
                    Image(systemName: "play.fill")
                    Text("Connect")
                }
                .frame(width: 200)
                .padding()
                .background(isValidCode ? Color.accentColor : Color.gray)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .disabled(!isValidCode)

            Spacer()
        }
        .padding()
    }

    private var connectingView: some View {
        VStack(spacing: 24) {
            ProgressView()
                .scaleEffect(2)

            Text("Connecting to \(roomCode)...")
                .font(.title3)

            Text("Establishing secure connection")
                .font(.body)
                .foregroundColor(.secondary)

            Button("Cancel") {
                isConnecting = false
            }
            .padding(.top)
        }
    }

    private var streamView: some View {
        ZStack {
            // Placeholder for WebRTC video view
            Color.black
                .ignoresSafeArea()

            VStack {
                // Status overlay
                HStack(spacing: 12) {
                    Circle()
                        .fill(Color.red)
                        .frame(width: 8, height: 8)
                    Text("LIVE")
                        .font(.caption)
                        .fontWeight(.bold)

                    Divider()
                        .frame(height: 16)

                    Text(roomCode)
                        .font(.caption)

                    if isMuted {
                        Image(systemName: "speaker.slash.fill")
                            .font(.caption)
                    }
                }
                .foregroundColor(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(.ultraThinMaterial)
                .cornerRadius(20)
                .padding(.top, 8)

                Spacer()

                // Alert banner
                if hasAlert {
                    alertBanner
                        .padding()
                        .transition(.move(edge: .top).combined(with: .opacity))
                }

                Text("Video stream will appear here")
                    .foregroundColor(.white.opacity(0.5))

                Spacer()
            }
        }
    }

    private var alertBanner: some View {
        HStack {
            Image(systemName: alertType == "cry" ? "waveform" : "eye.fill")
                .foregroundColor(alertType == "cry" ? .red : .orange)

            Text(alertMessage)
                .font(.body)
                .fontWeight(.medium)

            Spacer()

            Button {
                withAnimation {
                    hasAlert = false
                }
            } label: {
                Image(systemName: "xmark")
            }
        }
        .padding()
        .background(alertType == "cry" ? Color.red.opacity(0.2) : Color.orange.opacity(0.2))
        .cornerRadius(12)
    }

    private var isValidCode: Bool {
        roomCode.count == 6
    }

    private func connect() {
        guard isValidCode else { return }

        isConnecting = true

        // Simulate connection delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            isConnecting = false
            isConnected = true
        }
    }

    private func disconnect() {
        isConnected = false
        isConnecting = false
        roomCode = ""
    }
}

#Preview {
    NavigationStack {
        ViewerView(onBack: {})
    }
}
