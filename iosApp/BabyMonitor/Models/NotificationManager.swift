import Foundation
import UserNotifications
import UIKit

class NotificationManager: NSObject, ObservableObject {
    static let shared = NotificationManager()

    @Published var isAuthorized = false
    @Published var pendingNotifications: [UNNotificationRequest] = []

    private override init() {
        super.init()
        checkAuthorization()
    }

    func requestAuthorization() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            DispatchQueue.main.async {
                self.isAuthorized = granted
            }

            if granted {
                self.setupNotificationCategories()
            }

            if let error = error {
                print("Notification authorization error: \(error)")
            }
        }
    }

    func checkAuthorization() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            DispatchQueue.main.async {
                self.isAuthorized = settings.authorizationStatus == .authorized
            }
        }
    }

    private func setupNotificationCategories() {
        // Cry alert category
        let viewAction = UNNotificationAction(
            identifier: "VIEW_CAMERA",
            title: "View Camera",
            options: [.foreground]
        )

        let dismissAction = UNNotificationAction(
            identifier: "DISMISS",
            title: "Dismiss",
            options: [.destructive]
        )

        let cryCategory = UNNotificationCategory(
            identifier: "cry",
            actions: [viewAction, dismissAction],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )

        let motionCategory = UNNotificationCategory(
            identifier: "motion",
            actions: [viewAction, dismissAction],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )

        let disconnectedCategory = UNNotificationCategory(
            identifier: "disconnected",
            actions: [viewAction],
            intentIdentifiers: [],
            options: []
        )

        UNUserNotificationCenter.current().setNotificationCategories([
            cryCategory,
            motionCategory,
            disconnectedCategory
        ])
    }

    func sendLocalNotification(title: String, body: String, category: String, userInfo: [String: Any] = [:]) {
        guard isAuthorized else {
            print("Notifications not authorized")
            return
        }

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = category == "cry" ? .defaultCritical : .default
        content.categoryIdentifier = category
        content.userInfo = userInfo

        // Add badge count
        let unreadCount = CameraRegistry.shared.unreadAlertCount
        content.badge = NSNumber(value: unreadCount)

        // Trigger immediately
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 0.1, repeats: false)

        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: trigger
        )

        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Notification error: \(error)")
            }
        }
    }

    func clearBadge() {
        UIApplication.shared.applicationIconBadgeNumber = 0
    }

    func removeAllPendingNotifications() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    }

    func removeAllDeliveredNotifications() {
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
    }
}

// MARK: - UNUserNotificationCenterDelegate
extension NotificationManager: UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // Show notification even when app is in foreground
        completionHandler([.banner, .sound, .badge])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo

        switch response.actionIdentifier {
        case "VIEW_CAMERA":
            if let cameraId = userInfo["cameraId"] as? String {
                // Post notification to open camera view
                NotificationCenter.default.post(
                    name: .openCamera,
                    object: nil,
                    userInfo: ["cameraId": cameraId]
                )
            }
        case "DISMISS":
            // Mark as read
            if let cameraId = userInfo["cameraId"] as? String,
               let alertId = userInfo["alertId"] as? String {
                CameraRegistry.shared.markAlertAsRead(cameraId: cameraId, alertId: alertId)
            }
        default:
            break
        }

        completionHandler()
    }
}

extension Notification.Name {
    static let openCamera = Notification.Name("openCamera")
}
