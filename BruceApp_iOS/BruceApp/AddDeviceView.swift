//
//  AddDeviceView.swift
//  BruceApp
//
//  Created by Marlin Schuck on 15.05.26.
//

import SwiftUI
import SwiftData
import CoreBluetooth

struct AddDeviceView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var bleManager: BruceBLEManager
    
    @State private var pinInput: String = ""
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color(red: 0.08, green: 0.08, blue: 0.09).ignoresSafeArea()
                
                VStack(spacing: 20) {
                    if bleManager.connectionState == .needsPin || bleManager.connectionState == .authenticating {
                        VStack(spacing: 24) {
                            Spacer()
                            Image(systemName: "lock.shield.fill")
                                .font(.custom("doto", size: 60))
                                .foregroundStyle(.purple)
                            
                            VStack(spacing: 8) {
                                Text("PIN-Pairing successful")
                                    .font(.custom("doto", size: 14))
                                    .foregroundStyle(.white)
                                Text("Your Bruce Device now shows a PIN enter it here:")
                                    .font(.custom("doto", size: 14))
                                    .multilineTextAlignment(.center)
                                    .foregroundStyle(.gray)
                                    .padding(.horizontal, 20)
                            }
                            
                            TextField("PIN", text: $pinInput)
                                .font(.custom("doto", size: 14))
                                .fontWeight(.bold)
                                .multilineTextAlignment(.center)
                                .padding()
                                .background(RoundedRectangle(cornerRadius: 12).fill(Color(white: 0.15)))
                                .foregroundStyle(.white)
                                .keyboardType(.numberPad)
                                .frame(maxWidth: 200)
                            
                            if bleManager.connectionState == .authenticating {
                                ProgressView("Checking PIN...").tint(.purple).foregroundStyle(.gray)
                            } else {
                                Button(action: {
                                    if !pinInput.isEmpty {
                                        bleManager.submitPin(pinInput)
                                    }
                                }) {
                                    Text("Pair")
                                        .font(.custom("doto", size: 14))
                                        .fontWeight(.bold)
                                        .foregroundColor(.white)
                                        .frame(maxWidth: 160)
                                        .padding()
                                        .background(Color.purple)
                                        .cornerRadius(12)
                                }
                                .disabled(pinInput.isEmpty)
                            }
                            Spacer()
                        }
                        
                    } else if bleManager.discoveredDevices.isEmpty {
                        VStack(spacing: 20) {
                            ProgressView().tint(.white).scaleEffect(1.2)
                            Text("Searching for Bruce Hardware...")
                                .font(.custom("doto", size: 14))
                                .foregroundStyle(.gray)
                        }
                        .padding(.top, 60)
                    } else {
                        ScrollView {
                            VStack(spacing: 16) {
                                ForEach(bleManager.discoveredDevices, id: \.identifier) { peripheral in
                                    Button {
                                        bleManager.connectToDevice(peripheral)
                                    } label: {
                                        HStack(spacing: 16) {
                                            Image(systemName: "cpu").font(.title).foregroundStyle(.white)
                                            VStack(alignment: .leading, spacing: 4) {
                                                Text(peripheral.name ?? "Unknown Board")
                                                    .font(.custom("doto", size: 14)).fontWeight(.bold).foregroundStyle(.white)
                                                Text("Ready for Pairing").font(.custom("doto", size: 14)).foregroundStyle(.green)
                                            }
                                            Spacer()
                                            Image(systemName: "plus.circle.fill").foregroundStyle(.white).font(.title2)
                                        }
                                        .padding()
                                        .background(RoundedRectangle(cornerRadius: 16).fill(Color(white: 0.15)))
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .padding()
                        }
                    }
                    Spacer()
                }
            }
            .navigationTitle("New Device")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        bleManager.stopExplicitScan()
                        dismiss()
                    }
                    .foregroundStyle(.white)
                }
            }
            .onAppear { bleManager.startExplicitScan() }
            .onDisappear { bleManager.stopExplicitScan() }
            .onChange(of: bleManager.connectionState) { _, newState in
                if newState == .paired {
                    if let activePeripheral = bleManager.activePeripheral {
                        let newDevice = BruceDevice(
                            id: activePeripheral.identifier,
                            name: bleManager.hardwareName,
                            category: "Paired Hardware",
                            savedPin: pinInput // PIN abspeichern
                        )
                        modelContext.insert(newDevice)
                        try? modelContext.save()
                    }
                    dismiss()
                }
            }
        }
    }
}
