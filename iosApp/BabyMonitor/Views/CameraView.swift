import SwiftUI
import AVFoundation

struct CameraView: View {
    let onBack: () -> Void

    @StateObject private var cameraManager = CameraManager()
    @StateObject private var audioAnalyzer = AudioAnalyzer()
    @EnvironmentObject var appState: AppState

    @State private var isStreaming = false
    @State private var roomCode: String?
    @State private var motionDetected = false
    @State private var cryDetected = false
    @State private var showAlert = false
    @State private var alertMessage = ""

    var body: some View {
        ZStack {
            // Camera Preview
            CameraPreviewView(session: cameraManager.session)
                .ignoresSafeArea()

            VStack {
                // Status Bar
                HStack(spacing: 16) {
                    StatusIndicator(
                        label: isStreaming ? "LIVE" : "OFF",
                        isActive: isStreaming,
                        activeColor: .red
                    )

                    StatusIndicator(
                        label: "Motion",
                        isActive: appState.settings.motionDetectionEnabled,
                        activeColor: motionDetected ? .yellow : .green
                    )

                    StatusIndicator(
                        label: "Audio",
                        isActive: appState.settings.cryDetectionEnabled,
                        activeColor: cryDetected ? .red : .green
                    )
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(.ultraThinMaterial)
                .cornerRadius(20)
                .padding(.top, 8)

                Spacer()

                // Room Code Display
                if isStreaming, let code = roomCode {
                    VStack(spacing: 4) {
                        Text("Room Code")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(code)
                            .font(.system(size: 32, weight: .bold, design: .monospaced))
                        Text("Share this code with viewers")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)
                    .padding(.bottom, 20)
                }

                // Control Panel
                VStack(spacing: 16) {
                    // Main streaming button
                    Button {
                        toggleStreaming()
                    } label: {
                        HStack {
                            Image(systemName: isStreaming ? "stop.fill" : "play.fill")
                            Text(isStreaming ? "Stop Streaming" : "Start Streaming")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(isStreaming ? Color.red : Color.accentColor)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }

                    // Toggle chips
                    HStack(spacing: 12) {
                        ToggleChip(
                            label: "Motion",
                            icon: "eye.fill",
                            isOn: $appState.settings.motionDetectionEnabled
                        )

                        ToggleChip(
                            label: "Cry Detection",
                            icon: "mic.fill",
                            isOn: $appState.settings.cryDetectionEnabled
                        )
                    }

                    // Audio level
                    if appState.settings.cryDetectionEnabled {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Audio Level")
                                .font(.caption)
                                .foregroundColor(.secondary)

                            ProgressView(value: audioAnalyzer.audioLevel)
                                .tint(audioLevel(audioAnalyzer.audioLevel))
                        }
                    }
                }
                .padding()
                .background(.ultraThinMaterial)
                .cornerRadius(16)
                .padding()
            }
        }
        .navigationTitle("Camera Mode")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Back") { onBack() }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    cameraManager.toggleTorch()
                } label: {
                    Image(systemName: cameraManager.isTorchOn ? "flashlight.on.fill" : "flashlight.off.fill")
                }
            }
        }
        .alert("Alert", isPresented: $showAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(alertMessage)
        }
        .onAppear {
            cameraManager.startSession()
            if appState.settings.cryDetectionEnabled {
                audioAnalyzer.startAnalysis()
            }
        }
        .onDisappear {
            cameraManager.stopSession()
            audioAnalyzer.stopAnalysis()
        }
    }

    private func toggleStreaming() {
        isStreaming.toggle()
        if isStreaming {
            roomCode = generateRoomCode()
            appState.roomCode = roomCode
        } else {
            roomCode = nil
            appState.roomCode = nil
        }
    }

    private func generateRoomCode() -> String {
        let chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return String((0..<6).map { _ in chars.randomElement()! })
    }

    private func audioLevel(_ level: Double) -> Color {
        if level > 0.7 { return .red }
        if level > 0.4 { return .orange }
        return .green
    }
}

struct StatusIndicator: View {
    let label: String
    let isActive: Bool
    let activeColor: Color

    var body: some View {
        HStack(spacing: 4) {
            Circle()
                .fill(isActive ? activeColor : .gray)
                .frame(width: 8, height: 8)
            Text(label)
                .font(.caption2)
                .foregroundColor(.primary)
        }
    }
}

struct ToggleChip: View {
    let label: String
    let icon: String
    @Binding var isOn: Bool

    var body: some View {
        Button {
            isOn.toggle()
        } label: {
            HStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.caption)
                Text(label)
                    .font(.caption)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(isOn ? Color.accentColor.opacity(0.2) : Color.gray.opacity(0.2))
            .foregroundColor(isOn ? .accentColor : .secondary)
            .cornerRadius(20)
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(isOn ? Color.accentColor : Color.clear, lineWidth: 1)
            )
        }
    }
}

// Camera Preview UIViewRepresentable
struct CameraPreviewView: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)

        let previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.videoGravity = .resizeAspectFill
        previewLayer.frame = view.bounds
        view.layer.addSublayer(previewLayer)

        context.coordinator.previewLayer = previewLayer
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        context.coordinator.previewLayer?.frame = uiView.bounds
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    class Coordinator {
        var previewLayer: AVCaptureVideoPreviewLayer?
    }
}

#Preview {
    NavigationStack {
        CameraView(onBack: {})
    }
    .environmentObject(AppState())
}
