//
//  DeviceDetailView.swift
//  BruceApp
//
//  Created by Marlin Schuck on 16.05.26.
//

import SwiftUI
import SwiftData
import CoreBluetooth

struct DeviceDetailView: View {
    let device: BruceDevice
    @ObservedObject var bleManager: BruceBLEManager
    
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    private var isCurrentlyConnected: Bool {
        if let peripheral = bleManager.connectedPeripherals[device.id] {
            return peripheral.state == .connected && bleManager.connectionState == .paired
        }
        return false
    }
    
    var body: some View {
        ZStack {
            Color(red: 0.08, green: 0.08, blue: 0.09).ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 24) {
                    VStack(spacing: 16) {
                        Image(device.imageName)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 150, height: 150)
                            .padding(20)
                            .background(Circle().fill(Color(white: 0.12)))
                            .font(.system(size: 60))
                            .foregroundStyle(.white.opacity(0.3))
                        
                        Text(device.name)
                            .font(.custom("doto", size: 14))
                            .fontWeight(.black)
                            .foregroundStyle(.white)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    .padding(.top, 20)
                    
                    HStack(spacing: 40) {
                        StatItem(
                            icon: "battery.100",
                            label: "PWR",
                            value: isCurrentlyConnected ? (bleManager.batteryLevel.map { "\($0)%" } ?? "--%") : "--%"
                        )
                        .font(.custom("doto", size: 14))
                        StatItem(
                            icon: isCurrentlyConnected ? "link" : "link.badge.plus",
                            label: "STATUS",
                            value: isCurrentlyConnected ? "SECURE" : "N/A"
                        )
                        .font(.custom("doto", size: 14))
                        StatItem(
                            icon: "antenna.radiowaves.left.and.right",
                            label: "BLE",
                            value: isCurrentlyConnected ? "ON" : "OFF"
                        )
                        .font(.custom("doto", size: 14))
                    }
                    .padding(.vertical, 20)
                    .frame(maxWidth: .infinity)
                    .background(RoundedRectangle(cornerRadius: 20).fill(Color(white: 0.12)))
                    .padding(.horizontal)
                    
                    VStack(spacing: 16) {
                        NavigationLink(destination: RemoteControlView(bleManager: bleManager)) {
                            ActionRow(icon: "display", title: "Navigation", subtitle: "Live Screen & Control")
                        }
                        .font(.custom("doto", size: 14))
                        .disabled(!isCurrentlyConnected)
                        .opacity(isCurrentlyConnected ? 1.0 : 0.5)
                        
                        NavigationLink(destination: TerminalView(bleManager: bleManager)) {
                            ActionRow(icon: "terminal", title: "Terminal", subtitle: "Serial Command Session")
                        }
                        .font(.custom("doto", size: 14))
                        .disabled(!isCurrentlyConnected)
                        .opacity(isCurrentlyConnected ? 1.0 : 0.5)
                        
                        NavigationLink(destination: QuickActionsView(bleManager: bleManager)) {
                            ActionRow(icon: "bolt.fill", title: "Quick Actions", subtitle: "BadUSB, BLE Spam, etc.")
                        }
                        .font(.custom("doto", size: 14))
                        .disabled(!isCurrentlyConnected)
                        .opacity(isCurrentlyConnected ? 1.0 : 0.5)
                    }
                    .padding(.horizontal)
                    
                    Button(action: removeDevice) {
                        HStack {
                            Image(systemName: "trash")
                            Text("Remove Device")
                        }
                        .font(.custom("doto", size: 14))
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(RoundedRectangle(cornerRadius: 16).stroke(Color.red.opacity(0.3), lineWidth: 2))
                    }
                    .padding(.horizontal)
                    .padding(.top, 20)
                }
                .padding(.bottom, 40)
            }
            .blur(radius: (bleManager.connectionState == .needsPin || bleManager.connectionState == .authenticating) ? 10 : 0)
            
            if bleManager.connectionState == .needsPin || bleManager.connectionState == .authenticating {
                Color.black.opacity(0.5).ignoresSafeArea()
                PinEntryOverlay(bleManager: bleManager)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .onAppear {
            bleManager.selectActiveDevice(uuid: device.id)
        }
    }
    
    private func removeDevice() {
        bleManager.disconnectActiveDevice()
        bleManager.connectedPeripherals.removeValue(forKey: device.id)
        bleManager.savedDeviceUUIDs.removeAll(where: { $0 == device.id })
        modelContext.delete(device)
        try? modelContext.save()
        dismiss()
    }
}

struct PinEntryOverlay: View {
    @ObservedObject var bleManager: BruceBLEManager
    @State private var pinInput: String = ""
    
    var body: some View {
        VStack(spacing: 24) {
            // Changed from .purple to .white
            Image(systemName: "lock.shield.fill").font(.system(size: 50)).foregroundStyle(.white)
            VStack(spacing: 8) {
                Text("Lock active").font(.custom("doto", size: 14)).foregroundStyle(.white)
                Text("The Device shows a PIN for unlock.").font(.custom("doto", size: 14)).multilineTextAlignment(.center).foregroundStyle(.gray)
            }
            TextField("PIN", text: $pinInput)
                .font(.custom("doto", size: 14)).fontWeight(.bold).multilineTextAlignment(.center)
                .padding().background(RoundedRectangle(cornerRadius: 12).fill(Color(white: 0.2))).foregroundStyle(.white).keyboardType(.numberPad).frame(maxWidth: 150)
            Button(action: { if !pinInput.isEmpty { bleManager.submitPin(pinInput) } }) {
                Text(bleManager.connectionState == .authenticating ? "CHECKING..." : "UNLOCK")
                    // Changed background from .purple to .white, text from .white to .black for contrast
                    .font(.custom("doto", size: 14)).fontWeight(.bold).foregroundColor(.black).frame(maxWidth: 160).padding().background(Color.white).cornerRadius(12)
            }
            .disabled(pinInput.isEmpty || bleManager.connectionState == .authenticating)
        }
        .padding(30).background(RoundedRectangle(cornerRadius: 24).fill(Color(white: 0.1)).shadow(radius: 20)).padding(.horizontal, 40)
    }
}

struct StatItem: View {
    let icon: String
    let label: String
    let value: String
    
    var body: some View {
        VStack(spacing: 6) {
            // Changed from .purple to .white
            Image(systemName: icon).foregroundStyle(.white).font(.title3)
            Text(value).font(.custom("doto", size: 14)).fontWeight(.bold).foregroundStyle(.white)
            Text(label).font(.custom("doto", size: 14)).foregroundStyle(Color(white: 0.5))
        }
    }
}

struct ActionRow: View {
    let icon: String
    let title: String
    let subtitle: String
    
    var body: some View {
        HStack(spacing: 20) {
            // Changed from .purple to .white
            Image(systemName: icon).font(.title2).foregroundStyle(.white).frame(width: 30)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.custom("doto", size: 14)).fontWeight(.bold).foregroundStyle(.white)
                Text(subtitle).font(.custom("doto", size: 14)).foregroundStyle(Color(white: 0.6))
            }
            Spacer()
            Image(systemName: "chevron.right").foregroundStyle(Color(white: 0.3))
        }
        .padding(20).background(RoundedRectangle(cornerRadius: 16).fill(Color(white: 0.12)))
    }
}
