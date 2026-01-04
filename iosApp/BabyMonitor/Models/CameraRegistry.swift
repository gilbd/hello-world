import Foundation
import Combine

/// Represents a connected baby camera
struct BabyCamera: Identifiable, Codable, Equatable {
    let id: String
    var name: String
    var roomCode: String
    var isConnected: Bool
    var lastSeen: Date
    var thumbnailData: Data?
    var alerts: [CameraAlert]

    init(id: String = UUID().uuidString,
         name: String,
         roomCode: String,
         isConnected: Bool = false,
         lastSeen: Date = Date(),
         thumbnailData: Data? = nil,
         alerts: [CameraAlert] = []) {
        self.id = id
        self.name = name
        self.roomCode = roomCode
        self.isConnected = isConnected
        self.lastSeen = lastSeen
        self.thumbnailData = thumbnailData
        self.alerts = alerts
    }
}

struct CameraAlert: Identifiable, Codable, Equatable {
    let id: String
    let type: AlertType
    let message: String
    let timestamp: Date
    var isRead: Bool

    enum AlertType: String, Codable {
        case cry = "cry"
        case motion = "motion"
        case disconnected = "disconnected"
        case connected = "connected"
    }

    init(id: String = UUID().uuidString,
         type: AlertType,
         message: String,
         timestamp: Date = Date(),
         isRead: Bool = false) {
        self.id = id
        self.type = type
        self.message = message
        self.timestamp = timestamp
        self.isRead = isRead
    }
}

/// Manages the registry of all connected cameras
class CameraRegistry: ObservableObject {
    static let shared = CameraRegistry()

    @Published var cameras: [BabyCamera] = []
    @Published var unreadAlertCount: Int = 0
    @Published var isAdminMode: Bool = false

    private let camerasKey = "registered_cameras"
    private let adminPINKey = "admin_pin"

    private init() {
        loadCameras()
    }

    // MARK: - Camera Management

    func addCamera(name: String, roomCode: String) -> BabyCamera {
        let camera = BabyCamera(name: name, roomCode: roomCode)
        cameras.append(camera)
        saveCameras()
        return camera
    }

    func removeCamera(id: String) {
        cameras.removeAll { $0.id == id }
        saveCameras()
    }

    func updateCamera(_ camera: BabyCamera) {
        if let index = cameras.firstIndex(where: { $0.id == camera.id }) {
            cameras[index] = camera
            saveCameras()
        }
    }

    func renameCamera(id: String, newName: String) {
        if let index = cameras.firstIndex(where: { $0.id == id }) {
            cameras[index].name = newName
            saveCameras()
        }
    }

    func setConnected(id: String, connected: Bool) {
        if let index = cameras.firstIndex(where: { $0.id == id }) {
            cameras[index].isConnected = connected
            cameras[index].lastSeen = Date()

            // Add connection alert
            let alert = CameraAlert(
                type: connected ? .connected : .disconnected,
                message: connected ? "Camera connected" : "Camera disconnected"
            )
            cameras[index].alerts.insert(alert, at: 0)

            updateUnreadCount()
            saveCameras()
        }
    }

    func updateThumbnail(id: String, data: Data) {
        if let index = cameras.firstIndex(where: { $0.id == id }) {
            cameras[index].thumbnailData = data
            cameras[index].lastSeen = Date()
        }
    }

    // MARK: - Alerts

    func addAlert(cameraId: String, type: CameraAlert.AlertType, message: String) {
        if let index = cameras.firstIndex(where: { $0.id == cameraId }) {
            let alert = CameraAlert(type: type, message: message)
            cameras[index].alerts.insert(alert, at: 0)

            // Keep only last 50 alerts per camera
            if cameras[index].alerts.count > 50 {
                cameras[index].alerts = Array(cameras[index].alerts.prefix(50))
            }

            updateUnreadCount()
            saveCameras()

            // Trigger notification
            NotificationManager.shared.sendLocalNotification(
                title: cameras[index].name,
                body: message,
                category: type.rawValue
            )
        }
    }

    func markAlertAsRead(cameraId: String, alertId: String) {
        if let cameraIndex = cameras.firstIndex(where: { $0.id == cameraId }),
           let alertIndex = cameras[cameraIndex].alerts.firstIndex(where: { $0.id == alertId }) {
            cameras[cameraIndex].alerts[alertIndex].isRead = true
            updateUnreadCount()
            saveCameras()
        }
    }

    func markAllAlertsAsRead(cameraId: String) {
        if let index = cameras.firstIndex(where: { $0.id == cameraId }) {
            for i in cameras[index].alerts.indices {
                cameras[index].alerts[i].isRead = true
            }
            updateUnreadCount()
            saveCameras()
        }
    }

    func clearAlerts(cameraId: String) {
        if let index = cameras.firstIndex(where: { $0.id == cameraId }) {
            cameras[index].alerts.removeAll()
            updateUnreadCount()
            saveCameras()
        }
    }

    private func updateUnreadCount() {
        unreadAlertCount = cameras.reduce(0) { count, camera in
            count + camera.alerts.filter { !$0.isRead }.count
        }
    }

    // MARK: - Admin PIN

    var hasAdminPIN: Bool {
        UserDefaults.standard.string(forKey: adminPINKey) != nil
    }

    func setAdminPIN(_ pin: String) {
        UserDefaults.standard.set(pin, forKey: adminPINKey)
    }

    func verifyAdminPIN(_ pin: String) -> Bool {
        guard let storedPIN = UserDefaults.standard.string(forKey: adminPINKey) else {
            return true // No PIN set, allow access
        }
        return pin == storedPIN
    }

    func clearAdminPIN() {
        UserDefaults.standard.removeObject(forKey: adminPINKey)
    }

    // MARK: - Persistence

    private func saveCameras() {
        if let encoded = try? JSONEncoder().encode(cameras) {
            UserDefaults.standard.set(encoded, forKey: camerasKey)
        }
    }

    private func loadCameras() {
        if let data = UserDefaults.standard.data(forKey: camerasKey),
           let decoded = try? JSONDecoder().decode([BabyCamera].self, from: data) {
            cameras = decoded
            updateUnreadCount()
        }
    }
}
