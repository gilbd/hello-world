import AVFoundation
import Accelerate

class AudioAnalyzer: ObservableObject {
    @Published var audioLevel: Double = 0
    @Published var isCryDetected = false

    private var audioEngine: AVAudioEngine?
    private var isAnalyzing = false

    // Cry detection parameters
    private let cryFrequencyLow: Float = 250
    private let cryFrequencyHigh: Float = 600
    private let volumeThreshold: Float = 0.15
    private let cryDurationThreshold: TimeInterval = 0.5

    private var cryStartTime: Date?
    private var consecutiveCryFrames = 0

    // Callback for cry detection
    var onCryDetected: (() -> Void)?

    func startAnalysis() {
        guard !isAnalyzing else { return }

        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.playAndRecord, mode: .measurement)
            try audioSession.setActive(true)

            audioEngine = AVAudioEngine()
            guard let engine = audioEngine else { return }

            let inputNode = engine.inputNode
            let format = inputNode.outputFormat(forBus: 0)

            inputNode.installTap(onBus: 0, bufferSize: 4096, format: format) { [weak self] buffer, time in
                self?.processAudioBuffer(buffer)
            }

            try engine.start()
            isAnalyzing = true

        } catch {
            print("Audio engine error: \(error)")
        }
    }

    func stopAnalysis() {
        audioEngine?.stop()
        audioEngine?.inputNode.removeTap(onBus: 0)
        audioEngine = nil
        isAnalyzing = false

        DispatchQueue.main.async {
            self.audioLevel = 0
            self.isCryDetected = false
        }
    }

    private func processAudioBuffer(_ buffer: AVAudioPCMBuffer) {
        guard let channelData = buffer.floatChannelData?[0] else { return }

        let frameCount = Int(buffer.frameLength)

        // Calculate RMS
        var rms: Float = 0
        vDSP_rmsqv(channelData, 1, &rms, vDSP_Length(frameCount))

        // Estimate frequency using zero-crossing rate
        var crossings = 0
        for i in 1..<frameCount {
            if (channelData[i] >= 0 && channelData[i-1] < 0) ||
               (channelData[i] < 0 && channelData[i-1] >= 0) {
                crossings += 1
            }
        }

        let sampleRate = buffer.format.sampleRate
        let zeroCrossingRate = Float(crossings) / Float(frameCount)
        let estimatedFrequency = zeroCrossingRate * Float(sampleRate) / 2

        // Check for cry characteristics
        let isCryLike = rms > volumeThreshold &&
                        estimatedFrequency >= cryFrequencyLow &&
                        estimatedFrequency <= cryFrequencyHigh

        // Update cry detection with duration check
        if isCryLike {
            if cryStartTime == nil {
                cryStartTime = Date()
            }
            consecutiveCryFrames += 1

            if let startTime = cryStartTime,
               Date().timeIntervalSince(startTime) >= cryDurationThreshold {
                DispatchQueue.main.async {
                    if !self.isCryDetected {
                        self.isCryDetected = true
                        self.onCryDetected?()
                    }
                }
            }
        } else {
            cryStartTime = nil
            consecutiveCryFrames = 0
            DispatchQueue.main.async {
                self.isCryDetected = false
            }
        }

        // Update audio level
        DispatchQueue.main.async {
            self.audioLevel = Double(min(rms * 5, 1.0)) // Scale for visibility
        }
    }
}
