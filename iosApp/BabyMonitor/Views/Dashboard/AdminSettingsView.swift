import SwiftUI

struct AdminSettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var registry = CameraRegistry.shared
    @ObservedObject var notificationManager = NotificationManager.shared

    @State private var showSetPIN = false
    @State private var showChangePIN = false
    @State private var showClearPINConfirm = false

    var body: some View {
        NavigationStack {
            Form {
                // Notifications Section
                Section {
                    if notificationManager.isAuthorized {
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            Text("Notifications Enabled")
                        }
                    } else {
                        Button {
                            notificationManager.requestAuthorization()
                        } label: {
                            HStack {
                                Image(systemName: "bell.badge")
                                Text("Enable Notifications")
                            }
                        }
                    }
                } header: {
                    Label("Notifications", systemImage: "bell.fill")
                } footer: {
                    Text("Get alerts when baby is crying or moving")
                }

                // Security Section
                Section {
                    if registry.hasAdminPIN {
                        HStack {
                            Image(systemName: "lock.fill")
                                .foregroundColor(.green)
                            Text("PIN Protection Enabled")
                        }

                        Button("Change PIN") {
                            showChangePIN = true
                        }

                        Button("Remove PIN", role: .destructive) {
                            showClearPINConfirm = true
                        }
                    } else {
                        Button {
                            showSetPIN = true
                        } label: {
                            HStack {
                                Image(systemName: "lock.open")
                                Text("Set Admin PIN")
                            }
                        }
                    }
                } header: {
                    Label("Security", systemImage: "lock.shield.fill")
                } footer: {
                    Text("Protect access to the dashboard with a PIN")
                }

                // Cameras Section
                Section {
                    ForEach(registry.cameras) { camera in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(camera.name)
                                    .font(.body)
                                Text(camera.roomCode)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }

                            Spacer()

                            Circle()
                                .fill(camera.isConnected ? Color.green : Color.red)
                                .frame(width: 10, height: 10)
                        }
                    }
                    .onDelete { indexSet in
                        for index in indexSet {
                            registry.removeCamera(id: registry.cameras[index].id)
                        }
                    }
                } header: {
                    Label("Cameras (\(registry.cameras.count))", systemImage: "video.fill")
                }

                // Data Section
                Section {
                    Button("Clear All Alerts") {
                        for camera in registry.cameras {
                            registry.clearAlerts(cameraId: camera.id)
                        }
                    }

                    Button("Clear Notification Badge") {
                        notificationManager.clearBadge()
                    }
                } header: {
                    Label("Data", systemImage: "externaldrive.fill")
                }

                // About Section
                Section {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0.0")
                            .foregroundColor(.secondary)
                    }

                    HStack {
                        Text("Cameras Connected")
                        Spacer()
                        Text("\(registry.cameras.filter { $0.isConnected }.count)/\(registry.cameras.count)")
                            .foregroundColor(.secondary)
                    }
                } header: {
                    Label("About", systemImage: "info.circle.fill")
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $showSetPIN) {
                PINEntryView(mode: .set) { pin in
                    registry.setAdminPIN(pin)
                }
            }
            .sheet(isPresented: $showChangePIN) {
                PINEntryView(mode: .change) { pin in
                    registry.setAdminPIN(pin)
                }
            }
            .alert("Remove PIN?", isPresented: $showClearPINConfirm) {
                Button("Cancel", role: .cancel) {}
                Button("Remove", role: .destructive) {
                    registry.clearAdminPIN()
                }
            } message: {
                Text("Anyone will be able to access the dashboard without a PIN")
            }
        }
    }
}

struct PINEntryView: View {
    enum Mode {
        case set
        case change
        case unlock
    }

    let mode: Mode
    var onComplete: ((String) -> Void)?
    var onVerify: ((String) -> Bool)?

    @Environment(\.dismiss) private var dismiss
    @State private var currentPIN = ""
    @State private var newPIN = ""
    @State private var confirmPIN = ""
    @State private var step = 0
    @State private var errorMessage: String?
    @State private var isShaking = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 32) {
                Spacer()

                // Title
                VStack(spacing: 8) {
                    Image(systemName: mode == .unlock ? "lock.fill" : "lock.open.fill")
                        .font(.system(size: 50))
                        .foregroundColor(.accentColor)

                    Text(titleText)
                        .font(.title2)
                        .fontWeight(.bold)

                    Text(subtitleText)
                        .font(.body)
                        .foregroundColor(.secondary)
                }

                // PIN dots
                HStack(spacing: 20) {
                    ForEach(0..<4, id: \.self) { index in
                        Circle()
                            .fill(index < currentPINLength ? Color.accentColor : Color.gray.opacity(0.3))
                            .frame(width: 16, height: 16)
                    }
                }
                .modifier(ShakeEffect(animatableData: isShaking ? 1 : 0))

                // Error message
                if let error = errorMessage {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                }

                Spacer()

                // Number pad
                VStack(spacing: 16) {
                    ForEach(0..<3) { row in
                        HStack(spacing: 32) {
                            ForEach(1...3, id: \.self) { col in
                                let number = row * 3 + col
                                NumberButton(number: "\(number)") {
                                    addDigit("\(number)")
                                }
                            }
                        }
                    }

                    HStack(spacing: 32) {
                        // Empty space
                        Circle()
                            .fill(Color.clear)
                            .frame(width: 70, height: 70)

                        NumberButton(number: "0") {
                            addDigit("0")
                        }

                        // Delete button
                        Button {
                            deleteDigit()
                        } label: {
                            Image(systemName: "delete.left.fill")
                                .font(.title2)
                                .frame(width: 70, height: 70)
                                .foregroundColor(.primary)
                        }
                    }
                }
                .padding(.bottom, 40)
            }
            .padding()
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    if mode != .unlock {
                        Button("Cancel") {
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    private var titleText: String {
        switch mode {
        case .set:
            return step == 0 ? "Set PIN" : "Confirm PIN"
        case .change:
            return step == 0 ? "Enter Current PIN" : (step == 1 ? "Enter New PIN" : "Confirm New PIN")
        case .unlock:
            return "Enter PIN"
        }
    }

    private var subtitleText: String {
        switch mode {
        case .set:
            return step == 0 ? "Enter a 4-digit PIN" : "Re-enter your PIN"
        case .change:
            return step == 0 ? "Enter your current PIN" : (step == 1 ? "Enter a new 4-digit PIN" : "Re-enter your new PIN")
        case .unlock:
            return "Enter your 4-digit PIN to continue"
        }
    }

    private var currentPINLength: Int {
        switch mode {
        case .set:
            return step == 0 ? newPIN.count : confirmPIN.count
        case .change:
            return step == 0 ? currentPIN.count : (step == 1 ? newPIN.count : confirmPIN.count)
        case .unlock:
            return currentPIN.count
        }
    }

    private func addDigit(_ digit: String) {
        errorMessage = nil

        switch mode {
        case .set:
            if step == 0 {
                if newPIN.count < 4 {
                    newPIN += digit
                    if newPIN.count == 4 {
                        step = 1
                    }
                }
            } else {
                if confirmPIN.count < 4 {
                    confirmPIN += digit
                    if confirmPIN.count == 4 {
                        verifySetPIN()
                    }
                }
            }

        case .change:
            if step == 0 {
                if currentPIN.count < 4 {
                    currentPIN += digit
                    if currentPIN.count == 4 {
                        verifyCurrentPIN()
                    }
                }
            } else if step == 1 {
                if newPIN.count < 4 {
                    newPIN += digit
                    if newPIN.count == 4 {
                        step = 2
                    }
                }
            } else {
                if confirmPIN.count < 4 {
                    confirmPIN += digit
                    if confirmPIN.count == 4 {
                        verifySetPIN()
                    }
                }
            }

        case .unlock:
            if currentPIN.count < 4 {
                currentPIN += digit
                if currentPIN.count == 4 {
                    verifyUnlockPIN()
                }
            }
        }
    }

    private func deleteDigit() {
        switch mode {
        case .set:
            if step == 0 && !newPIN.isEmpty {
                newPIN.removeLast()
            } else if step == 1 && !confirmPIN.isEmpty {
                confirmPIN.removeLast()
            }

        case .change:
            if step == 0 && !currentPIN.isEmpty {
                currentPIN.removeLast()
            } else if step == 1 && !newPIN.isEmpty {
                newPIN.removeLast()
            } else if step == 2 && !confirmPIN.isEmpty {
                confirmPIN.removeLast()
            }

        case .unlock:
            if !currentPIN.isEmpty {
                currentPIN.removeLast()
            }
        }
    }

    private func verifySetPIN() {
        if newPIN == confirmPIN {
            onComplete?(newPIN)
            dismiss()
        } else {
            errorMessage = "PINs don't match"
            confirmPIN = ""
            shake()
        }
    }

    private func verifyCurrentPIN() {
        if let verify = onVerify, verify(currentPIN) {
            step = 1
        } else if CameraRegistry.shared.verifyAdminPIN(currentPIN) {
            step = 1
        } else {
            errorMessage = "Incorrect PIN"
            currentPIN = ""
            shake()
        }
    }

    private func verifyUnlockPIN() {
        if let verify = onVerify {
            if verify(currentPIN) {
                dismiss()
            } else {
                errorMessage = "Incorrect PIN"
                currentPIN = ""
                shake()
            }
        } else if CameraRegistry.shared.verifyAdminPIN(currentPIN) {
            CameraRegistry.shared.isAdminMode = true
            dismiss()
        } else {
            errorMessage = "Incorrect PIN"
            currentPIN = ""
            shake()
        }
    }

    private func shake() {
        withAnimation(.default) {
            isShaking = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            isShaking = false
        }
    }
}

struct NumberButton: View {
    let number: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(number)
                .font(.title)
                .fontWeight(.medium)
                .frame(width: 70, height: 70)
                .background(Color(.systemGray5))
                .clipShape(Circle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct ShakeEffect: GeometryEffect {
    var animatableData: CGFloat

    func effectValue(size: CGSize) -> ProjectionTransform {
        let translation = sin(animatableData * .pi * 4) * 10
        return ProjectionTransform(CGAffineTransform(translationX: translation, y: 0))
    }
}

#Preview {
    AdminSettingsView()
}
