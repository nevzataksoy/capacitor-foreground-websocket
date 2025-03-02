import Capacitor
import Foundation
import PushKit
import UserNotifications

@objc(CapacitorForegroundWebsocketPlugin)
public class CapacitorForegroundWebsocketPlugin: CAPPlugin {
    private var webSocketTask: URLSessionWebSocketTask?
    private var urlSession: URLSession?
    var pushRegistry: PKPushRegistry?

    // Plugin yüklendiğinde PushKit ile VoIP push bildirimlerine kaydoluyoruz.
    override public func load() {
        pushRegistry = PKPushRegistry(queue: DispatchQueue.main)
        pushRegistry?.delegate = self
        pushRegistry?.desiredPushTypes = [.voIP]
    }

    @objc func start(_ call: CAPPluginCall) {
        guard let ip = call.getString("ip"),
              let port = call.getInt("port"),
              let title = call.getString("title"),
              let description = call.getString("description") else {
            call.reject("Missing parameters")
            return
        }
        let isWss = call.getBool("isWss") ?? false
        let protocolString = isWss ? "wss" : "ws"
        let urlString = "\(protocolString)://\(ip):\(port)"
        guard let url = URL(string: urlString) else {
            call.reject("Invalid URL")
            return
        }

        // URLSession oluşturuluyor; arka plan görevleri için ek ayarlamalar yapılabilir.
        let configuration = URLSessionConfiguration.default
        urlSession = URLSession(configuration: configuration, delegate: self, delegateQueue: OperationQueue())
        webSocketTask = urlSession?.webSocketTask(with: url)
        webSocketTask?.resume()
        listen()
        call.resolve()
    }

    @objc func stop(_ call: CAPPluginCall) {
        webSocketTask?.cancel(with: .goingAway, reason: nil)
        call.resolve()
    }

    @objc func send(_ call: CAPPluginCall) {
        guard let message = call.getString("message") else {
            call.reject("Message is required")
            return
        }
        let wsMessage = URLSessionWebSocketTask.Message.string(message)
        webSocketTask?.send(wsMessage) { error in
            if let error = error {
                call.reject("Error sending message: \(error.localizedDescription)")
            } else {
                call.resolve()
            }
        }
    }

    private func listen() {
        webSocketTask?.receive { [weak self] result in
            switch result {
            case .failure(let error):
                self?.notifyListeners("onerror", data: ["error": error.localizedDescription])
            case .success(let message):
                switch message {
                case .string(let text):
                    self?.notifyListeners("onmessage", data: ["message": text])
                case .data(let data):
                    // Binary data processing if needed.
                    break
                @unknown default:
                    break
                }
            }
            self?.listen()
        }
    }
}

extension CapacitorForegroundWebsocketPlugin: URLSessionWebSocketDelegate {
    public func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask,
                           didOpenWithProtocol protocol: String?) {
        notifyListeners("onopen", data: ["protocol": protocol ?? ""])
    }

    public func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask,
                           didCloseWith closeCode: URLSessionWebSocketTask.CloseCode, reason: Data?) {
        notifyListeners("onclose", data: ["code": closeCode.rawValue])
    }
}

extension CapacitorForegroundWebsocketPlugin: PKPushRegistryDelegate {
    // VoIP push token güncellendiğinde
    public func pushRegistry(_ registry: PKPushRegistry, didUpdate pushCredentials: PKPushCredentials, for type: PKPushType) {
        let deviceToken = pushCredentials.token.map { String(format: "%02x", $0) }.joined()
        notifyListeners("onpushToken", data: ["token": deviceToken])
    }

    public func pushRegistry(_ registry: PKPushRegistry, didInvalidatePushTokenFor type: PKPushType) {
        // Token geçersiz hale geldiğinde yapılacak işlemler.
    }

    // VoIP push bildirimi alındığında; uygulamayı arka planda uyandırıp, JS katmanına "onpush" eventi gönderir.
    public func pushRegistry(_ registry: PKPushRegistry,
                             didReceiveIncomingPushWith payload: PKPushPayload,
                             for type: PKPushType,
                             completion: @escaping () -> Void) {
        notifyListeners("onpush", data: payload.dictionaryPayload)
        completion()
    }
}