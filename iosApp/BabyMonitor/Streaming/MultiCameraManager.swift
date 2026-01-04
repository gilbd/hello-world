import Foundation
import Combine

/// Manages connections to multiple cameras simultaneously
class MultiCameraManager: ObservableObject {
    static let shared = MultiCameraManager()

    @Published var connections: [String: CameraConnection] = [:]

    private var signalingClients: [String: SignalingClient] = [:]
    private var cancellables = Set<AnyCancellable>()

    private init() {
        // Observe camera registry changes
        CameraRegistry.shared.$cameras
            .sink { [weak self] cameras in
                self?.syncConnections(with: cameras)
            }
            .store(in: &cancellables)
    }

    /// Connect to a specific camera
    func connect(to camera: BabyCamera) {
        guard connections[camera.id] == nil else { return }

        let connection = CameraConnection(camera: camera)
        connections[camera.id] = connection

        // Create signaling client
        let client = SignalingClient()
        signalingClients[camera.id] = client

        // Connect to signaling server
        client.connect(roomCode: camera.roomCode, asHost: false)

        // Update connection state
        connection.state = .connecting

        // Simulate connection (replace with actual WebRTC)
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) { [weak self] in
            connection.state = .connected
            CameraRegistry.shared.setConnected(id: camera.id, connected: true)
        }
    }

    /// Disconnect from a specific camera
    func disconnect(from cameraId: String) {
        signalingClients[cameraId]?.disconnect()
        signalingClients.removeValue(forKey: cameraId)
        connections.removeValue(forKey: cameraId)
        CameraRegistry.shared.setConnected(id: cameraId, connected: false)
    }

    /// Disconnect from all cameras
    func disconnectAll() {
        for (id, _) in connections {
            disconnect(from: id)
        }
    }

    /// Handle incoming alert from a camera
    func handleAlert(cameraId: String, type: CameraAlert.AlertType, message: String) {
        CameraRegistry.shared.addAlert(cameraId: cameraId, type: type, message: message)
    }

    /// Sync connections with camera registry
    private func syncConnections(with cameras: [BabyCamera]) {
        // Connect to new cameras
        for camera in cameras {
            if connections[camera.id] == nil {
                connect(to: camera)
            }
        }

        // Disconnect from removed cameras
        let cameraIds = Set(cameras.map { $0.id })
        for connectionId in connections.keys {
            if !cameraIds.contains(connectionId) {
                disconnect(from: connectionId)
            }
        }
    }
}

/// Represents a connection to a single camera
class CameraConnection: ObservableObject, Identifiable {
    let id: String
    let camera: BabyCamera

    @Published var state: ConnectionState = .disconnected
    @Published var latestFrame: Data?
    @Published var audioLevel: Float = 0

    enum ConnectionState {
        case disconnected
        case connecting
        case connected
        case reconnecting
        case failed(String)
    }

    init(camera: BabyCamera) {
        self.id = camera.id
        self.camera = camera
    }
}
