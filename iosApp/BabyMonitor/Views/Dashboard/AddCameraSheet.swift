import SwiftUI

struct AddCameraSheet: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var registry = CameraRegistry.shared

    @State private var cameraName = ""
    @State private var roomCode = ""
    @State private var isConnecting = false
    @State private var connectionError: String?

    var isValid: Bool {
        !cameraName.trimmingCharacters(in: .whitespaces).isEmpty &&
        roomCode.count == 6
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Camera Name", text: $cameraName)
                        .textInputAutocapitalization(.words)

                    TextField("Room Code", text: $roomCode)
                        .textInputAutocapitalization(.characters)
                        .onChange(of: roomCode) { newValue in
                            roomCode = String(newValue.uppercased().prefix(6))
                        }
                } header: {
                    Text("Camera Details")
                } footer: {
                    Text("Enter the 6-character room code shown on the camera device")
                }

                if let error = connectionError {
                    Section {
                        HStack {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.red)
                            Text(error)
                                .foregroundColor(.red)
                        }
                    }
                }

                Section {
                    Button {
                        addCamera()
                    } label: {
                        HStack {
                            Spacer()
                            if isConnecting {
                                ProgressView()
                                    .padding(.trailing, 8)
                                Text("Connecting...")
                            } else {
                                Image(systemName: "plus.circle.fill")
                                    .padding(.trailing, 4)
                                Text("Add Camera")
                            }
                            Spacer()
                        }
                    }
                    .disabled(!isValid || isConnecting)
                }
            }
            .navigationTitle("Add Camera")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
    }

    private func addCamera() {
        guard isValid else { return }

        isConnecting = true
        connectionError = nil

        // Simulate connection attempt
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            let camera = registry.addCamera(
                name: cameraName.trimmingCharacters(in: .whitespaces),
                roomCode: roomCode
            )

            // In production, this would actually connect to the camera
            registry.setConnected(id: camera.id, connected: true)

            isConnecting = false
            dismiss()
        }
    }
}

#Preview {
    AddCameraSheet()
}
