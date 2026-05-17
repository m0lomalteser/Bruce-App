//
//  TerminalView.swift
//  BruceApp
//
//  Created by Marlin Schuck on 16.05.26.
//

import SwiftUI
import NetworkExtension
import Combine

// MARK: - 1. Live Terminal View
struct TerminalView: View {
    @ObservedObject var bleManager: BruceBLEManager
    @State private var command: String = ""
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack {
                ScrollViewReader { proxy in
                    ScrollView {
                        VStack(alignment: .leading, spacing: 4) {
                            ForEach(Array(bleManager.terminalOutput.enumerated()), id: \.offset) { index, line in
                                Text(line)
                                    .foregroundStyle(line.starts(with: ">") ? .gray : .white)
                                    .id(index)
                            }
                        }
                        .font(.custom("doto", size: 14))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                    }
                    .onChange(of: bleManager.terminalOutput.count) { _, newValue in
                        if !bleManager.terminalOutput.isEmpty {
                            withAnimation { proxy.scrollTo(newValue - 1, anchor: .bottom) }
                        }
                    }
                }
                
                HStack {
                    Text(">").foregroundStyle(.gray).font(.system(.body, design: .monospaced))
                    TextField("COMMAND", text: $command)
                        .font(.custom("doto", size: 14))
                        .textFieldStyle(.plain)
                        .foregroundStyle(.white)
                        .onSubmit { send() }
                    
                    Button(action: send) {
                        Image(systemName: "return").foregroundStyle(.white)
                    }
                }
                .padding()
                .background(.ultraThinMaterial)
                .font(.custom("doto", size: 14))
            }
        }
        .navigationTitle("Terminal")
    }
    
    private func send() {
        guard !command.isEmpty else { return }
        bleManager.sendCommand(command)
        command = ""
    }
}

// MARK: - 2. Remote Control View
struct RemoteControlView: View {
    @ObservedObject var bleManager: BruceBLEManager
    
    let useRotaryEncoder: Bool = false
    
    @State private var drawCommands: [TFTCommand] = []
    @State private var canvasWidth: CGFloat = 240
    @State private var canvasHeight: CGFloat = 135
    
    @Environment(\.verticalSizeClass) var verticalSizeClass
    
    private let renderTimer = Timer.publish(every: 0.03, on: .main, in: .common).autoconnect()
    private let keepAliveTimer = Timer.publish(every: 2.0, on: .main, in: .common).autoconnect()
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            if verticalSizeClass == .compact {
                GeometryReader { geo in
                    Canvas { context, size in
                        context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(.black))
                        renderCanvasContent(context: context, size: size)
                    }
                    .frame(width: geo.size.width, height: geo.size.height)
                    .ignoresSafeArea()
                }
                .navigationBarHidden(true)
            } else {
                VStack {
                    ZStack {
                        RoundedRectangle(cornerRadius: 16)
                            .fill(Color.black)
                            .frame(width: canvasWidth, height: canvasHeight)
                            .overlay(
                                Canvas { context, size in
                                    context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(.black))
                                    renderCanvasContent(context: context, size: size)
                                }
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.3), lineWidth: 1))
                            .shadow(color: .white.opacity(0.05), radius: 20)
                            .scaleEffect(1.3)
                    }
                    .frame(height: 220)
                    .padding(.top, 40)
                    
                    Spacer()
                    
                    if useRotaryEncoder {
                        HStack(spacing: 30) {
                            Button(action: { bleManager.sendCommand("BTN_LEFT") }) {
                                RotaryButton(icon: "arrow.counterclockwise", label: "LEFT")
                            }
                            
                            Button(action: { bleManager.sendCommand("BTN_ACTION") }) {
                                Circle()
                                    .fill(Color.white.opacity(0.1))
                                    .background(.ultraThinMaterial, in: Circle())
                                    .overlay(Circle().stroke(Color.white.opacity(0.4), lineWidth: 1))
                                    .frame(width: 90, height: 90)
                                    .overlay(Text("CLICK").font(.system(.caption, design: .monospaced)).fontWeight(.bold).foregroundStyle(.white))
                            }
                            .buttonStyle(.plain)
                            
                            Button(action: { bleManager.sendCommand("BTN_RIGHT") }) {
                                RotaryButton(icon: "arrow.clockwise", label: "RIGHT")
                            }
                        }
                        .padding(.bottom, 50)
                    } else {
                        VStack(spacing: 20) {
                            Button(action: { bleManager.sendCommand("BTN_UP") }) { ControlButton(icon: "arrowtriangle.up.fill") }
                            HStack(spacing: 40) {
                                Button(action: { bleManager.sendCommand("BTN_LEFT") }) { ControlButton(icon: "arrowtriangle.left.fill") }
                                Button(action: { bleManager.sendCommand("BTN_ACTION") }) { ControlButton(icon: "circle.fill", isAction: true) }
                                Button(action: { bleManager.sendCommand("BTN_RIGHT") }) { ControlButton(icon: "arrowtriangle.right.fill") }
                            }
                            Button(action: { bleManager.sendCommand("BTN_DOWN") }) { ControlButton(icon: "arrowtriangle.down.fill") }
                        }
                        .padding(.bottom, 40)
                    }
                }
                .navigationTitle("Live Control")
                .navigationBarHidden(false)
            }
        }
        .onAppear {
            bleManager.rawScreenData = Data()
            bleManager.sendCommand("START_BLE_SCREEN")
        }
        .onDisappear {
            bleManager.sendCommand("STOP_BLE_SCREEN")
        }
        .onReceive(renderTimer) { _ in
            parseBinaryStream()
        }
        .onReceive(keepAliveTimer) { _ in
            bleManager.sendCommand("SCREEN_KEEP_ALIVE")
        }
    }
    
    private func renderCanvasContent(context: GraphicsContext, size: CGSize) {
        let scaleX = size.width / canvasWidth
        let scaleY = size.height / canvasHeight
        
        for cmd in drawCommands {
            switch cmd.type {
            case .fillScreen(let color):
                context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(color))
            case .fillRect(let rect, let color):
                let scaledRect = CGRect(x: rect.origin.x * scaleX, y: rect.origin.y * scaleY, width: rect.width * scaleX, height: rect.height * scaleY)
                context.fill(Path(scaledRect), with: .color(color))
            case .drawFastHLine(let x, let y, let w, let color):
                var path = Path()
                path.move(to: CGPoint(x: x * scaleX, y: y * scaleY))
                path.addLine(to: CGPoint(x: (x + w) * scaleX, y: y * scaleY))
                context.stroke(path, with: .color(color), lineWidth: 1.5)
            case .drawFastVLine(let x, let y, let h, let color):
                var path = Path()
                path.move(to: CGPoint(x: x * scaleX, y: y * scaleY))
                path.addLine(to: CGPoint(x: x * scaleX, y: (y + h) * scaleY))
                context.stroke(path, with: .color(color), lineWidth: 1.5)
            case .drawCircle(let rect, let color, let isFilled):
                let scaledRect = CGRect(x: rect.origin.x * scaleX, y: rect.origin.y * scaleY, width: rect.width * scaleX, height: rect.height * scaleY)
                let path = Path(ellipseIn: scaledRect)
                if isFilled { context.fill(path, with: .color(color)) }
                else { context.stroke(path, with: .color(color), lineWidth: 1.5) }
            case .drawTriangle(let p1, let p2, let p3, let color, let isFilled):
                var path = Path()
                path.move(to: CGPoint(x: p1.x * scaleX, y: p1.y * scaleY))
                path.addLine(to: CGPoint(x: p2.x * scaleX, y: p2.y * scaleY))
                path.addLine(to: CGPoint(x: p3.x * scaleX, y: p3.y * scaleY))
                path.closeSubpath()
                if isFilled { context.fill(path, with: .color(color)) }
                else { context.stroke(path, with: .color(color), lineWidth: 1.5) }
            case .drawWideLine(let p1, let p2, let width, let color):
                var path = Path()
                path.move(to: CGPoint(x: p1.x * scaleX, y: p1.y * scaleY))
                path.addLine(to: CGPoint(x: p2.x * scaleX, y: p2.y * scaleY))
                context.stroke(path, with: .color(color), lineWidth: width * scaleX)
            case .drawEllipse(let rect, let color, let isFilled):
                let scaledRect = CGRect(x: rect.origin.x * scaleX, y: rect.origin.y * scaleY, width: rect.width * scaleX, height: rect.height * scaleY)
                let path = Path(ellipseIn: scaledRect)
                if isFilled { context.fill(path, with: .color(color)) }
                else { context.stroke(path, with: .color(color), lineWidth: 1.5) }
            case .drawSecondaryRoundRect(let rect, let radius, let color, let isFilled):
                let scaledRect = CGRect(x: rect.origin.x * scaleX, y: rect.origin.y * scaleY, width: rect.width * scaleX, height: rect.height * scaleY)
                let path = Path(roundedRect: scaledRect, cornerRadius: radius * scaleX)
                if isFilled { context.fill(path, with: .color(color)) }
                else { context.stroke(path, with: .color(color), lineWidth: 1.5) }
            case .drawArc(let center, let radius, let startAngle, let endAngle, let lineWidth, let color):
                let scaledCenter = CGPoint(x: center.x * scaleX, y: center.y * scaleY)
                let scaledRadius = radius * scaleX
                var path = Path()
                path.addArc(center: scaledCenter, radius: scaledRadius, startAngle: startAngle, endAngle: endAngle, clockwise: false)
                context.stroke(path, with: .color(color), lineWidth: lineWidth * scaleX)
            case .drawString(let text, let origin, let color, let fontSize, let xOffset):
                let resolvedFont = Font.system(size: fontSize * scaleY, weight: .heavy, design: .monospaced)
                let correctedOrigin = CGPoint(x: (origin.x - xOffset) * scaleX, y: origin.y * scaleY)
                context.draw(Text(text).font(resolvedFont).foregroundColor(color), at: correctedOrigin, anchor: .topLeading)
            }
        }
    }
    
    private func parseBinaryStream() {
        let bytes = [UInt8](bleManager.rawScreenData)
        guard !bytes.isEmpty else { return }
        
        var offset = 0
        var commands: [TFTCommand] = drawCommands
        
        while offset < bytes.count {
            guard offset < bytes.count else { break }
            
            if bytes[offset] != 0xAA { offset += 1; continue }
            if offset + 2 >= bytes.count { break }
            
            let size = Int(bytes[offset + 1])
            
            guard size > 0 else {
                offset += 1
                continue
            }
            
            let fn = Int(bytes[offset + 2])
            if offset + size > bytes.count { break }
            let startData = offset + 3
            
            switch fn {
            case 99:
                let w = CGFloat(readInt16(bytes, startData))
                let h = CGFloat(readInt16(bytes, startData + 2))
                if w > 0 && h > 0 {
                    DispatchQueue.main.async {
                        self.canvasWidth = w
                        self.canvasHeight = h
                    }
                }
            case 0:
                let color = color565ToSwiftUI(readInt16(bytes, startData))
                commands.removeAll()
                commands.append(TFTCommand(type: .fillScreen(color)))
            case 1, 2:
                let x = CGFloat(readInt16(bytes, startData)); let y = CGFloat(readInt16(bytes, startData + 2))
                let w = CGFloat(readInt16(bytes, startData + 4)); let h = CGFloat(readInt16(bytes, startData + 6))
                let color = color565ToSwiftUI(readInt16(bytes, startData + 8))
                commands.append(TFTCommand(type: .fillRect(CGRect(x: x, y: y, width: w, height: h), color)))
            case 3, 4:
                let x = CGFloat(readInt16(bytes, startData)); let y = CGFloat(readInt16(bytes, startData + 2))
                let w = CGFloat(readInt16(bytes, startData + 4)); let h = CGFloat(readInt16(bytes, startData + 6))
                let r = CGFloat(readInt16(bytes, startData + 8))
                let color = color565ToSwiftUI(readInt16(bytes, startData + 10))
                commands.append(TFTCommand(type: .drawSecondaryRoundRect(CGRect(x: x, y: y, width: w, height: h), r, color, fn == 4)))
            case 5, 6:
                let x = CGFloat(readInt16(bytes, startData)); let y = CGFloat(readInt16(bytes, startData + 2))
                let r = CGFloat(readInt16(bytes, startData + 4))
                let color = color565ToSwiftUI(readInt16(bytes, startData + 6))
                commands.append(TFTCommand(type: .drawCircle(CGRect(x: x - r, y: y - r, width: r * 2, height: r * 2), color, fn == 6)))
            case 7, 8:
                let x1 = CGFloat(readInt16(bytes, startData)); let y1 = CGFloat(readInt16(bytes, startData + 2))
                let x2 = CGFloat(readInt16(bytes, startData + 4)); let y2 = CGFloat(readInt16(bytes, startData + 6))
                let x3 = CGFloat(readInt16(bytes, startData + 8)); let y3 = CGFloat(readInt16(bytes, startData + 10))
                let color = color565ToSwiftUI(readInt16(bytes, startData + 12))
                commands.append(TFTCommand(type: .drawTriangle(CGPoint(x: x1, y: y1), CGPoint(x: x2, y: y2), CGPoint(x: x3, y: y3), color, fn == 8)))
            case 9, 10:
                let x = CGFloat(readInt16(bytes, startData)); let y = CGFloat(readInt16(bytes, startData + 2))
                let rx = CGFloat(readInt16(bytes, startData + 4)); let ry = CGFloat(readInt16(bytes, startData + 6))
                let color = color565ToSwiftUI(readInt16(bytes, startData + 8))
                commands.append(TFTCommand(type: .drawEllipse(CGRect(x: x - rx, y: y - ry, width: rx * 2, height: ry * 2), color, fn == 10)))
            case 11:
                let x = CGFloat(readInt16(bytes, startData)); let y = CGFloat(readInt16(bytes, startData + 2))
                let x1 = CGFloat(readInt16(bytes, startData + 4)); let y1 = CGFloat(readInt16(bytes, startData + 6))
                let color = color565ToSwiftUI(readInt16(bytes, startData + 8))
                commands.append(TFTCommand(type: .drawWideLine(CGPoint(x: x, y: y), CGPoint(x: x1, y: y1), 1.0, color)))
            case 12:
                if startData + 15 < bytes.count {
                    let x = CGFloat(readInt16(bytes, startData)); let y = CGFloat(readInt16(bytes, startData + 2))
                    let r = CGFloat(readInt16(bytes, startData + 4)); let ir = CGFloat(readInt16(bytes, startData + 6))
                    let startAngle = CGFloat(readInt16(bytes, startData + 8)); let endAngle = CGFloat(readInt16(bytes, startData + 10))
                    let color = color565ToSwiftUI(readInt16(bytes, startData + 12))
                    let sa = Angle(degrees: Double(startAngle + 90)); let ea = Angle(degrees: Double(endAngle + 90))
                    commands.append(TFTCommand(type: .drawArc(CGPoint(x: x, y: y), (r + ir) / 2.0, sa, ea, max(r - ir, 1.0), color)))
                }
            case 13:
                let x1 = CGFloat(readInt16(bytes, startData)); let y1 = CGFloat(readInt16(bytes, startData + 2))
                let x2 = CGFloat(readInt16(bytes, startData + 4)); let y2 = CGFloat(readInt16(bytes, startData + 6))
                let wd = CGFloat(readInt16(bytes, startData + 8))
                let color = color565ToSwiftUI(readInt16(bytes, startData + 10))
                commands.append(TFTCommand(type: .drawWideLine(CGPoint(x: x1, y: y1), CGPoint(x: x2, y: y2), wd, color)))
            case 14, 15, 16, 17:
                let x = CGFloat(readInt16(bytes, startData)); let y = CGFloat(readInt16(bytes, startData + 2))
                let scale = CGFloat(readInt16(bytes, startData + 4))
                let fgRaw = readInt16(bytes, startData + 6); let bgRaw = readInt16(bytes, startData + 8)
                let fgColor = color565ToSwiftUI(fgRaw)
                let bgColor = (bgRaw == fgRaw || bgRaw == 0) ? Color.black : color565ToSwiftUI(bgRaw)
                let textLen = size - 13
                if textLen > 0 && startData + 10 + textLen <= bytes.count {
                    let textBytes = Array(bytes[startData + 10..<startData + 10 + textLen])
                    if let text = String(bytes: textBytes.filter { $0 >= 32 && $0 <= 126 }, encoding: .utf8) {
                        let fontW: CGFloat = scale == 3 ? 13.5 : (scale == 2 ? 9.0 : 4.5)
                        let offX: CGFloat = fn == 14 ? (CGFloat(text.count) * fontW) / 2 : (fn == 15 ? CGFloat(text.count) * fontW : 0)
                        commands.append(TFTCommand(type: .fillRect(CGRect(x: x - offX, y: y, width: CGFloat(text.count) * fontW, height: scale * 8.0), bgColor)))
                        commands.append(TFTCommand(type: .drawString(text, CGPoint(x: x, y: y), fgColor, scale == 3 ? 24 : (scale == 2 ? 16 : 12), offX)))
                    }
                }
            case 19:
                if startData + 5 < bytes.count {
                    commands.append(TFTCommand(type: .fillRect(CGRect(x: CGFloat(readInt16(bytes, startData)), y: CGFloat(readInt16(bytes, startData + 2)), width: 1, height: 1), color565ToSwiftUI(readInt16(bytes, startData + 4)))))
                }
            // FIX FÜR ZEILE 339: Variablen sauber extrahiert und gecastet (ohne wirres textBytes Label)
            case 20:
                let x = CGFloat(readInt16(bytes, startData))
                let y = CGFloat(readInt16(bytes, startData + 2))
                let h = CGFloat(readInt16(bytes, startData + 4))
                let color = color565ToSwiftUI(readInt16(bytes, startData + 6))
                commands.append(TFTCommand(type: .drawFastVLine(x, y, h, color)))
            case 21:
                let x = CGFloat(readInt16(bytes, startData))
                let y = CGFloat(readInt16(bytes, startData + 2))
                let w = CGFloat(readInt16(bytes, startData + 4))
                let color = color565ToSwiftUI(readInt16(bytes, startData + 6))
                commands.append(TFTCommand(type: .drawFastHLine(x, y, w, color)))
            default: break
            }
            offset += size
        }
        
        DispatchQueue.main.async {
            if commands.count > 1500 { commands.removeFirst(commands.count - 1500) }
            self.drawCommands = commands
            
            if offset > 0 && offset <= bleManager.rawScreenData.count {
                bleManager.rawScreenData.removeFirst(offset)
            } else if offset > bleManager.rawScreenData.count {
                bleManager.rawScreenData.removeAll()
            }
        }
    }
    
    private func readInt16(_ bytes: [UInt8], _ index: Int) -> Int {
        guard index + 1 < bytes.count else { return 0 }
        let u16 = (UInt16(bytes[index]) << 8) | UInt16(bytes[index + 1])
        return Int(Int16(bitPattern: u16))
    }
    
    private func color565ToSwiftUI(_ c: Int) -> Color {
        return Color(red: Double((c >> 11) & 0x1F) / 31.0, green: Double((c >> 5) & 0x3F) / 63.0, blue: Double(c & 0x1F) / 31.0)
    }
}

// MARK: - 3. Hilfsstrukturen & Buttons
struct TFTCommand: Identifiable {
    let id = UUID()
    let type: CommandType
    enum CommandType {
        case fillScreen(Color), fillRect(CGRect, Color), drawString(String, CGPoint, Color, CGFloat, CGFloat), drawFastHLine(CGFloat, CGFloat, CGFloat, Color), drawFastVLine(CGFloat, CGFloat, CGFloat, Color), drawCircle(CGRect, Color, Bool), drawTriangle(CGPoint, CGPoint, CGPoint, Color, Bool), drawWideLine(CGPoint, CGPoint, CGFloat, Color), drawEllipse(CGRect, Color, Bool), drawSecondaryRoundRect(CGRect, CGFloat, Color, Bool), drawArc(CGPoint, CGFloat, Angle, Angle, CGFloat, Color)
    }
}

struct ControlButton: View {
    let icon: String
    var isAction: Bool = false
    var body: some View {
        Image(systemName: icon).font(.custom("doto", size: 14)).foregroundStyle(isAction ? .black : .white).frame(width: 80, height: 80)
            .background(isAction ? Color.white : Color.white.opacity(0.1)).clipShape(Circle()).overlay(Circle().stroke(Color.white.opacity(0.3), lineWidth: 1))
    }
}

struct RotaryButton: View {
    let icon: String
    let label: String
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon).font(.title).foregroundStyle(.white).frame(width: 70, height: 70)
                .background(.ultraThinMaterial, in: Circle()).overlay(Circle().stroke(Color.white.opacity(0.3), lineWidth: 1))
            Text(label).font(.custom("doto", size: 14)).foregroundStyle(.gray)
        }
    }
}

// 1. Define the Data Structure for Organized Actions
struct ActionItem: Identifiable {
    let id = UUID()
    let title: String
    let subtitle: String
    let icon: String
    let command: String // The standard serial/protocol command sent to Bruce
}

struct CategoryItem: Identifiable {
    let id = UUID()
    let name: String
    let icon: String
    let actions: [ActionItem]
}

struct QuickActionsView: View {
    @ObservedObject var bleManager: BruceBLEManager
    
    // 2. Define the Structured Menu Items (Using standard/defensive examples)
    private let categories = [
        CategoryItem(name: "Wi-Fi", icon: "wifi", actions: [
            ActionItem(title: "Network Scan", subtitle: "Scan for local networks", icon: "magnifyingglass", command: "WIFI_SCAN"),
            ActionItem(title: "Status Check", subtitle: "View current connection info", icon: "info.circle", command: "WIFI_STATUS"),
            ActionItem(title: "Deauth Attack", subtitle: "Perform deauth attack", icon: "bolt.fill", command: "WIFI_DEAUTH")
        ]),
        CategoryItem(name: "Bluetooth", icon: "badge.plus.radiowaves.forward", actions: [
            ActionItem(title: "Device Scan", subtitle: "Scan for BLE advertisements", icon: "antenna.radiowaves.left.and.right", command: "BLE_SCAN"),
            ActionItem(title: "Stop Advertising", subtitle: "Halt active broadcasts", icon: "stop.circle", command: "BLE_STOP"),
            ActionItem(title: "Apple", subtitle: "BLESPAM", icon: "bolt.fill", command: "BLESPAM_APPLE_CONTINUITY"),
            ActionItem(title: "Samsung", subtitle: "BLESPAM", icon: "bolt.fill", command: "BLESPAM_SAMSUNG"),
            ActionItem(title: "Android", subtitle: "BLESPAM", icon: "bolt.fill", command: "BLESPAM_ANDROID"),
            ActionItem(title: "Windows", subtitle: "BLESPAM", icon: "bolt.fill", command: "BLESPAM_WINDOWS")
        ]),
        CategoryItem(name: "IR (Infrared)", icon: "eye.fill", actions: [
            ActionItem(title: "Record Signal", subtitle: "Listen for incoming IR codes", icon: "record.circle", command: "IR_REC"),
            ActionItem(title: "Send Saved", subtitle: "Transmit saved remote code", icon: "paperplane", command: "IR_TX")
        ]),
        CategoryItem(name: "RF (Radio)", icon: "wave.3.right", actions: [
            ActionItem(title: "Frequency Analyzer", subtitle: "Monitor local sub-GHz bands", icon: "chart.bar", command: "RF_ANALYZE"),
            ActionItem(title: "Listen Mode", subtitle: "Receive signal packets", icon: "ear", command: "RF_RX"),
            ActionItem(title: "Record", subtitle: "Records signal packets", icon: "bolt.fill", command: "RF_RECORD"),
            ActionItem(title: "Play Recording", subtitle: "Plays recorded signal packets", icon: "bolt.fill", command: "RF_PLAY")
            
        ]),
        CategoryItem(name: "RFID", icon: "creditcard.and.123", actions: [
            ActionItem(title: "Read Card", subtitle: "Scan high/low frequency tags", icon: "sensor.tag.radiowaves.forward", command: "RFID_READ"),
            ActionItem(title: "Emulate Tag", subtitle: "Broadcast saved UID", icon: "broadcast", command: "RFID_EMU")
        ])
    ]
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 20) {
                    ForEach(categories) { category in
                        VStack(alignment: .leading, spacing: 10) {
                            // Section Header
                            HStack {
                                Image(systemName: category.icon)
                                    .foregroundStyle(.white)
                                Text(category.name)
                                    .font(.custom("doto", size: 16))
                                    .fontWeight(.bold)
                                    .foregroundStyle(.white)
                            }
                            .padding(.horizontal)
                            
                            // Category Options
                            ForEach(category.actions) { action in
                                Button(action: {
                                    sendDeviceCommand(action.command)
                                }) {
                                    ActionRow(
                                        icon: action.icon,
                                        title: action.title,
                                        subtitle: action.subtitle
                                    )
                                }
                                .font(.custom("doto", size: 14))
                            }
                        }
                    }
                }
                .padding()
            }
        }
        .navigationTitle("Quick Actions")
    }
    
    // 3. Command Dispatch Logic
    private func sendDeviceCommand(_ command: String) {
        // Formats the string into raw data (adding a newline characteristic if required by your firmware parser)
        guard let data = "\(command)\n".data(using: .utf8) else { return }
        
        // This relies on your existing BruceBLEManager implementation to write data to the TX characteristic
        // Example logic: bleManager.writeToCharacteristic(data: data)
        print("Sending command to Bruce: \(command)")
    }
}
