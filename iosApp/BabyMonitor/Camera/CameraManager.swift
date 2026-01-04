import AVFoundation
import UIKit

class CameraManager: NSObject, ObservableObject {
    @Published var isTorchOn = false

    let session = AVCaptureSession()
    private var videoOutput: AVCaptureVideoDataOutput?
    private var audioOutput: AVCaptureAudioDataOutput?
    private let sessionQueue = DispatchQueue(label: "camera.session.queue")

    private var previousFrameLuminance: [UInt8]?
    private var frameCount = 0

    // Motion detection callback
    var onMotionDetected: ((Float) -> Void)?

    override init() {
        super.init()
        configureSession()
    }

    private func configureSession() {
        sessionQueue.async { [weak self] in
            self?.setupCamera()
        }
    }

    private func setupCamera() {
        session.beginConfiguration()
        session.sessionPreset = .hd1280x720

        // Video input
        guard let videoDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let videoInput = try? AVCaptureDeviceInput(device: videoDevice) else {
            session.commitConfiguration()
            return
        }

        if session.canAddInput(videoInput) {
            session.addInput(videoInput)
        }

        // Audio input
        if let audioDevice = AVCaptureDevice.default(for: .audio),
           let audioInput = try? AVCaptureDeviceInput(device: audioDevice) {
            if session.canAddInput(audioInput) {
                session.addInput(audioInput)
            }
        }

        // Video output for frame analysis
        let videoOutput = AVCaptureVideoDataOutput()
        videoOutput.setSampleBufferDelegate(self, queue: DispatchQueue(label: "video.output.queue"))
        videoOutput.videoSettings = [
            kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA
        ]

        if session.canAddOutput(videoOutput) {
            session.addOutput(videoOutput)
            self.videoOutput = videoOutput
        }

        session.commitConfiguration()
    }

    func startSession() {
        sessionQueue.async { [weak self] in
            if self?.session.isRunning == false {
                self?.session.startRunning()
            }
        }
    }

    func stopSession() {
        sessionQueue.async { [weak self] in
            if self?.session.isRunning == true {
                self?.session.stopRunning()
            }
        }
    }

    func toggleTorch() {
        guard let device = AVCaptureDevice.default(for: .video),
              device.hasTorch else { return }

        do {
            try device.lockForConfiguration()
            let newMode: AVCaptureDevice.TorchMode = isTorchOn ? .off : .on
            if device.isTorchModeSupported(newMode) {
                device.torchMode = newMode
                DispatchQueue.main.async {
                    self.isTorchOn = !self.isTorchOn
                }
            }
            device.unlockForConfiguration()
        } catch {
            print("Torch error: \(error)")
        }
    }
}

// MARK: - Video Frame Analysis
extension CameraManager: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput,
                       didOutput sampleBuffer: CMSampleBuffer,
                       from connection: AVCaptureConnection) {
        frameCount += 1

        // Analyze every 3rd frame
        guard frameCount % 3 == 0 else { return }

        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        CVPixelBufferLockBaseAddress(pixelBuffer, .readOnly)
        defer { CVPixelBufferUnlockBaseAddress(pixelBuffer, .readOnly) }

        let width = CVPixelBufferGetWidth(pixelBuffer)
        let height = CVPixelBufferGetHeight(pixelBuffer)

        guard let baseAddress = CVPixelBufferGetBaseAddress(pixelBuffer) else { return }

        let bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer)
        let buffer = baseAddress.assumingMemoryBound(to: UInt8.self)

        // Downsample and extract luminance
        let downsampleFactor = 8
        let dsWidth = width / downsampleFactor
        let dsHeight = height / downsampleFactor
        var luminance = [UInt8](repeating: 0, count: dsWidth * dsHeight)

        for y in 0..<dsHeight {
            for x in 0..<dsWidth {
                let srcY = y * downsampleFactor
                let srcX = x * downsampleFactor
                let offset = srcY * bytesPerRow + srcX * 4

                // BGRA format - calculate luminance
                let b = Float(buffer[offset])
                let g = Float(buffer[offset + 1])
                let r = Float(buffer[offset + 2])
                let lum = UInt8(0.299 * r + 0.587 * g + 0.114 * b)

                luminance[y * dsWidth + x] = lum
            }
        }

        // Detect motion
        if let previous = previousFrameLuminance, previous.count == luminance.count {
            let motionLevel = detectMotion(previous: previous, current: luminance)
            if motionLevel > 0.02 {
                DispatchQueue.main.async {
                    self.onMotionDetected?(motionLevel)
                }
            }
        }

        previousFrameLuminance = luminance
    }

    private func detectMotion(previous: [UInt8], current: [UInt8]) -> Float {
        var changedPixels = 0
        let threshold: UInt8 = 30

        for i in 0..<current.count {
            let diff = abs(Int(current[i]) - Int(previous[i]))
            if diff > Int(threshold) {
                changedPixels += 1
            }
        }

        return Float(changedPixels) / Float(current.count)
    }
}
