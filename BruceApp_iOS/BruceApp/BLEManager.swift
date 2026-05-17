//
//  BLEManager.swift
//  BruceApp
//
//  Created by Marlin Schuck on 16.05.26.
//

import Foundation
import CoreBluetooth
import SwiftUI
import Combine

// MARK: - Globales Dateimodell
struct BruceFileItem: Identifiable, Hashable {
    let id = UUID()
    let name: String
    let isDirectory: Bool
    let size: String?
}

let BRUCE_APP_SERVICE_UUID = CBUUID(string: "B1234567-89AB-CDEF-0123-456789ABCDEF")
let AUTH_CHAR_UUID       = CBUUID(string: "A1234567-89AB-CDEF-0123-456789ABCDEF")
let HW_INFO_CHAR_UUID    = CBUUID(string: "C1234567-89AB-CDEF-0123-456789ABCDEF")
let BATTERY_CHAR_UUID    = CBUUID(string: "D1234567-89AB-CDEF-0123-456789ABCDEF")
let TERM_TX_CHAR_UUID    = CBUUID(string: "E1234567-89AB-CDEF-0123-456789ABCDEF")
let TERM_RX_CHAR_UUID    = CBUUID(string: "F1234567-89AB-CDEF-0123-456789ABCDEF")
let SCREEN_BLE_CHAR_UUID = CBUUID(string: "99999999-89AB-CDEF-0123-456789ABCDEF")

class BruceBLEManager: NSObject, ObservableObject, CBCentralManagerDelegate, CBPeripheralDelegate {
    private var centralManager: CBCentralManager!
    
    @Published var connectedPeripherals: [UUID: CBPeripheral] = [:]
    @Published var activePeripheral: CBPeripheral?
    private var txCharacteristic: CBCharacteristic?
    private var authCharacteristic: CBCharacteristic?
    
    @Published var connectionState: ConnectionState = .disconnected
    @Published var hardwareName: String = "Unknown Device"
    @Published var batteryLevel: Int? = nil
    @Published var terminalOutput: [String] = []
    
    // Live-Puffer für Dateiverzeichnis
    @Published var remoteFiles: [BruceFileItem] = []
    @Published var isFileFolderLoading: Bool = false
    
    // Zustand für den ESP32 -> iPhone Download Stream
    @Published var isDownloading: Bool = false
    @Published var downloadingFileName: String = ""
    @Published var lastDownloadedFileURL: URL? = nil
    private var downloadDataBuffer = Data()
    
    @Published var rawScreenData: Data = Data()
    
    @Published var discoveredDevices: [CBPeripheral] = []
    private var isSearchingForNewDevices = false
    var savedDeviceUUIDs: [UUID] = []
    var savedDevicePins: [UUID: String] = [:]
    
    enum ConnectionState { case disconnected, scanning, connecting, needsPin, authenticating, paired, failed }
    
    override init() {
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: nil)
    }
    
    func startExplicitScan() {
        discoveredDevices.removeAll()
        isSearchingForNewDevices = true
        connectionState = .scanning
        if centralManager.state == .poweredOn {
            centralManager.scanForPeripherals(withServices: [BRUCE_APP_SERVICE_UUID], options: [CBCentralManagerScanOptionAllowDuplicatesKey: false])
        }
    }
    
    func stopExplicitScan() {
        isSearchingForNewDevices = false
        centralManager.stopScan()
        if connectionState == .scanning { connectionState = .disconnected }
    }
    
    func monitorSavedDevices(uuids: [UUID]) {
        self.savedDeviceUUIDs = uuids
        guard centralManager.state == .poweredOn else { return }
        
        for uuid in uuids {
            if connectedPeripherals[uuid] == nil {
                if let peripheral = centralManager.retrievePeripherals(withIdentifiers: [uuid]).first {
                    self.connectedPeripherals[uuid] = peripheral
                    peripheral.delegate = self
                    centralManager.connect(peripheral, options: nil)
                }
            }
        }
    }
    
    func connectToDevice(_ peripheral: CBPeripheral) {
        stopExplicitScan()
        self.connectedPeripherals[peripheral.identifier] = peripheral
        activePeripheral = peripheral
        activePeripheral?.delegate = self
        connectionState = .connecting
        centralManager.connect(peripheral, options: nil)
    }
    
    func selectActiveDevice(uuid: UUID) {
        if let peripheral = connectedPeripherals[uuid] {
            activePeripheral = peripheral
            activePeripheral?.delegate = self
            
            if peripheral.state == .disconnected {
                centralManager.connect(peripheral, options: nil)
            } else if peripheral.state == .connected {
                if peripheral.services == nil {
                    peripheral.discoverServices([BRUCE_APP_SERVICE_UUID])
                } else {
                    for service in peripheral.services ?? [] {
                        for char in service.characteristics ?? [] {
                            if char.uuid == AUTH_CHAR_UUID {
                                self.authCharacteristic = char
                                peripheral.readValue(for: char)
                            } else if char.uuid == TERM_TX_CHAR_UUID {
                                self.txCharacteristic = char
                            } else if char.uuid == BATTERY_CHAR_UUID {
                                peripheral.readValue(for: char)
                            }
                        }
                    }
                }
            }
        }
    }
    
    func disconnectActiveDevice() {
        if let peripheral = activePeripheral {
            centralManager.cancelPeripheralConnection(peripheral)
        }
    }
    
    func submitPin(_ pin: String) {
        guard let peripheral = activePeripheral,
              let authChar = authCharacteristic,
              peripheral.state == .connected else { return }
        
        DispatchQueue.main.async { self.connectionState = .authenticating }
        let data = pin.data(using: .utf8)!
        peripheral.writeValue(data, for: authChar, type: .withResponse)
    }
    
    func sendCommand(_ command: String) {
        guard connectionState == .paired,
              let peripheral = activePeripheral,
              let tx = txCharacteristic,
              peripheral.state == .connected else { return }
        
        let data = command.data(using: .utf8)!
        peripheral.writeValue(data, for: tx, type: .withoutResponse)
        
        if !command.contains("BTN_") && !command.contains("SCREEN_") && !command.contains("FS_") {
            terminalOutput.append("> \(command)")
        }
    }
    
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state == .poweredOn {
            if isSearchingForNewDevices { startExplicitScan() } else { monitorSavedDevices(uuids: savedDeviceUUIDs) }
        }
    }
    
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        if isSearchingForNewDevices {
            if !savedDeviceUUIDs.contains(peripheral.identifier) && !discoveredDevices.contains(where: { $0.identifier == peripheral.identifier }) {
                DispatchQueue.main.async { self.discoveredDevices.append(peripheral) }
            }
        }
    }
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        DispatchQueue.main.async {
            self.connectedPeripherals[peripheral.identifier] = peripheral
            if self.activePeripheral == nil { self.activePeripheral = peripheral }
        }
        peripheral.delegate = self
        peripheral.discoverServices([BRUCE_APP_SERVICE_UUID])
    }
    
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        DispatchQueue.main.async {
            self.connectedPeripherals.removeValue(forKey: peripheral.identifier)
            if peripheral.identifier == self.activePeripheral?.identifier {
                self.connectionState = .disconnected
                self.batteryLevel = nil
                self.rawScreenData = Data()
                self.remoteFiles.removeAll()
                self.activePeripheral = nil
            }
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let services = peripheral.services else { return }
        for service in services { peripheral.discoverCharacteristics(nil, for: service) }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard let chars = service.characteristics else { return }
        for char in chars {
            if char.uuid == AUTH_CHAR_UUID {
                self.authCharacteristic = char
                peripheral.setNotifyValue(true, for: char)
                peripheral.readValue(for: char)
            }
            if char.uuid == HW_INFO_CHAR_UUID { peripheral.readValue(for: char) }
            if char.uuid == TERM_TX_CHAR_UUID { self.txCharacteristic = char }
            if char.uuid == BATTERY_CHAR_UUID {
                peripheral.setNotifyValue(true, for: char)
                peripheral.readValue(for: char)
            }
            if char.uuid == TERM_RX_CHAR_UUID { peripheral.setNotifyValue(true, for: char) }
            if char.uuid == SCREEN_BLE_CHAR_UUID { peripheral.setNotifyValue(true, for: char) }
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        guard let data = characteristic.value else { return }
        
        switch characteristic.uuid {
        case AUTH_CHAR_UUID:
            if let statusString = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines) {
                DispatchQueue.main.async {
                    if statusString == "LOCKED" || statusString == "AUTH_REQUIRED" {
                        if let autoPin = self.savedDevicePins[peripheral.identifier] {
                            self.submitPin(autoPin)
                        } else {
                            self.connectionState = .needsPin
                        }
                    } else if statusString == "SUCCESS" || statusString == "UNLOCKED" {
                        self.connectionState = .paired
                        if !self.savedDeviceUUIDs.contains(peripheral.identifier) {
                            self.savedDeviceUUIDs.append(peripheral.identifier)
                        }
                    } else if statusString == "WRONG_PIN" || statusString == "FAILED" {
                        self.connectionState = .needsPin
                    }
                }
            }
            
        case TERM_RX_CHAR_UUID:
            if let packetString = String(data: data, encoding: .utf8) {
                let lines = packetString.components(separatedBy: "\n")
                for line in lines {
                    let cleanLine = line.trimmingCharacters(in: .whitespacesAndNewlines)
                    
                    if cleanLine.hasPrefix("FILE_ITEM:") {
                        let payload = cleanLine.replacingOccurrences(of: "FILE_ITEM:", with: "")
                        let parts = payload.components(separatedBy: "|")
                        if parts.count >= 3 {
                            let name = parts[0]
                            let isDir = parts[1] == "1"
                            let bytesSize = Int(parts[2]) ?? 0
                            
                            let readableSize: String? = isDir ? nil : (bytesSize >= 1048576 ? String(format: "%.1f MB", Double(bytesSize) / 1048576.0) : (bytesSize >= 1024 ? "\(bytesSize / 1024) KB" : "\(bytesSize) B"))
                            
                            DispatchQueue.main.async {
                                if !self.remoteFiles.contains(where: { $0.name == name }) {
                                    self.remoteFiles.append(BruceFileItem(name: name, isDirectory: isDir, size: readableSize))
                                }
                            }
                        }
                    } else if cleanLine == "FS_LIST_DONE" {
                        DispatchQueue.main.async { self.isFileFolderLoading = false }
                    } else if cleanLine.hasPrefix("FS_DOWNLOAD_INIT:") {
                        let payload = cleanLine.replacingOccurrences(of: "FS_DOWNLOAD_INIT:", with: "")
                        let parts = payload.components(separatedBy: "|")
                        if parts.count >= 1 {
                            DispatchQueue.main.async {
                                self.downloadingFileName = parts[0]
                                self.downloadDataBuffer = Data()
                                self.lastDownloadedFileURL = nil
                                self.isDownloading = true
                            }
                        }
                    } else if cleanLine.hasPrefix("FS_DOWNLOAD_CHUNK:") {
                        let hex = cleanLine.replacingOccurrences(of: "FS_DOWNLOAD_CHUNK:", with: "")
                        if let chunkData = dataFromHexString(hex) {
                            self.downloadDataBuffer.append(chunkData)
                        }
                    } else if cleanLine == "FS_DOWNLOAD_END" {
                        let fileName = self.downloadingFileName
                        let finalData = self.downloadDataBuffer
                        DispatchQueue.main.async {
                            let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
                            try? finalData.write(to: tempURL)
                            self.lastDownloadedFileURL = tempURL
                            self.isDownloading = false
                        }
                    } else if !cleanLine.isEmpty {
                        DispatchQueue.main.async { self.terminalOutput.append(cleanLine) }
                    }
                }
            }
            
        case SCREEN_BLE_CHAR_UUID:
            DispatchQueue.main.async {
                self.rawScreenData.append(data)
                if self.rawScreenData.count > 60000 {
                    self.rawScreenData = self.rawScreenData.suffix(10000)
                }
            }
            
        case HW_INFO_CHAR_UUID:
            if let name = String(data: data, encoding: .utf8) {
                DispatchQueue.main.async { self.hardwareName = name.trimmingCharacters(in: .whitespacesAndNewlines) }
            }
        case BATTERY_CHAR_UUID:
            if let bat = data.first { DispatchQueue.main.async { self.batteryLevel = Int(bat) } }
        default: break
        }
    }
    
    private func dataFromHexString(_ hex: String) -> Data? {
        var data = Data()
        var hexStr = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        while hexStr.count > 0 {
            let subIndex = hexStr.index(hexStr.startIndex, offsetBy: 2)
            guard subIndex <= hexStr.endIndex else { break }
            let c = String(hexStr[..<subIndex])
            hexStr = String(hexStr[subIndex...])
            if let b = UInt8(c, radix: 16) { data.append(b) } else { return nil }
        }
        return data
    }
}
