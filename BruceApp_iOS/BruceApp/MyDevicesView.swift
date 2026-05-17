//
//  MyDevicesView.swift
//  BruceApp
//
//  Created by Marlin Schuck on 15.05.26.
//

import SwiftUI
import SwiftData
import CoreBluetooth

struct MyDevicesView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \BruceDevice.dateAdded) private var savedDevices: [BruceDevice]
    
    @ObservedObject var bleManager: BruceBLEManager
    @State private var showingAddSheet = false

    let backgroundColor = Color(red: 0.08, green: 0.08, blue: 0.09)

    var body: some View {
        NavigationStack {
            ZStack {
                backgroundColor.ignoresSafeArea()
                
                VStack {
                    if let newDevice = bleManager.discoveredDevices.first {
                        Button {
                            bleManager.connectToDevice(newDevice)
                            showingAddSheet = true
                        } label: {
                            HStack {
                                Image(systemName: "cpu")
                                Text("New Device: \(newDevice.name ?? "Bruce")")
                                Spacer()
                                Text("PAIR").fontWeight(.bold)
                                    .font(.custom("doto", size: 14))
                            }
                            .padding()
                            .background(Color(white: 0.15))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .foregroundStyle(.white)
                            .padding(.horizontal)
                        }
                    }
                    
                    if savedDevices.isEmpty {
                        ContentUnavailableView("Keine Hardware", systemImage: "square.grid.2x2", description: Text("Schalte ein Bruce-Gerät ein, um es zu koppeln."))
                            .foregroundStyle(.white)
                            .font(.custom("doto", size: 14))
                    } else {
                        ScrollView {
                            VStack(spacing: 20) {
                                let chunkedDevices = savedDevices.chunked(into: 2)
                                
                                ForEach(0..<chunkedDevices.count, id: \.self) { rowIndex in
                                    DeviceRowView(rowDevices: chunkedDevices[rowIndex], bleManager: bleManager, modelContext: modelContext)
                                }
                            }
                            .padding(.horizontal, 16)
                            .padding(.top, 10)
                        }
                    }
                }
            }
            .navigationTitle("Dashboard")
                .font(.custom("doto", size: 14))
            .toolbarBackground(.hidden, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingAddSheet = true }) {
                        Image(systemName: "plus")
                            .fontWeight(.bold)
                            .foregroundStyle(.white)
                    }
                }
            }
            .onAppear {
                let uuids = savedDevices.map { $0.id }
                bleManager.monitorSavedDevices(uuids: uuids)
                
                // Lade Auto-Login PINs
                for device in savedDevices {
                    if let pin = device.savedPin {
                        bleManager.savedDevicePins[device.id] = pin
                    }
                }
            }
            .sheet(isPresented: $showingAddSheet) {
                AddDeviceView(bleManager: bleManager)
                    .preferredColorScheme(.dark)
            }
        }
        .preferredColorScheme(.dark)
    }
}

struct DeviceRowView: View {
    let rowDevices: [BruceDevice]
    @ObservedObject var bleManager: BruceBLEManager
    let modelContext: ModelContext
    
    var body: some View {
        HStack(spacing: 16) {
            ForEach(rowDevices) { device in
                NavigationLink(destination: DeviceDetailView(device: device, bleManager: bleManager)) {
                    DeviceTileView(device: device, bleManager: bleManager)
                }
                .buttonStyle(.plain)
                .contextMenu {
                    Button(role: .destructive) {
                        modelContext.delete(device)
                    } label: {
                        Label("Entfernen", systemImage: "trash")
                            .font(.custom("doto", size: 14))
                    }
                }
            }
            
            if rowDevices.count == 1 {
                Spacer()
                    .frame(maxWidth: .infinity)
            }
        }
    }
}

struct DeviceTileView: View {
    let device: BruceDevice
    @ObservedObject var bleManager: BruceBLEManager
    
    var isConnected: Bool {
        if let peripheral = bleManager.connectedPeripherals[device.id] {
            return peripheral.state == .connected && bleManager.connectionState == .paired
        }
        return false
    }
    
    var batteryLevel: Int? {
        isConnected && bleManager.activePeripheral?.identifier == device.id ? bleManager.batteryLevel : nil
    }
    
    private var verifiedImage: Image {
        if UIImage(named: device.imageName) != nil {
            return Image(device.imageName)
        } else {
            return Image("bruce")
        }
    }
    
    var body: some View {
        VStack(spacing: 12) {
            ZStack(alignment: .topTrailing) {
                RoundedRectangle(cornerRadius: 24)
                    .fill(Color(white: 0.12))
                
                batteryIcon(level: batteryLevel)
                    .foregroundStyle(.white.opacity(0.7))
                    .font(.system(size: 14))
                    .padding(16)
                    .zIndex(1)
                
                verifiedImage
                    .resizable()
                    .scaledToFit()
                    .padding(24)
            }
            .aspectRatio(1.0, contentMode: .fit)
            
            HStack(spacing: 6) {
                Circle()
                    .fill(isConnected ? Color.green : Color.red)
                    .frame(width: 8, height: 8)
                
                Text(device.name)
                    .font(.custom("doto", size: 14))
                    .fontWeight(.semibold)
                    .foregroundStyle(.white)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity)
    }
    
    @ViewBuilder
    private func batteryIcon(level: Int?) -> some View {
        if let level = level {
            if level > 80 { Image(systemName: "battery.100") }
            else if level > 40 { Image(systemName: "battery.50") }
            else { Image(systemName: "battery.25") }
        } else {
            Image(systemName: "batteryblock")
                .overlay(Image(systemName: "questionmark").font(.system(size: 8, weight: .bold)).foregroundStyle(.black))
        }
    }
}
