//
//  FilesView.swift
//  BruceApp
//
//  Created by Marlin Schuck on 17.05.26.
//

import SwiftUI
import SwiftData
import CoreBluetooth
import UniformTypeIdentifiers

struct FilesView: View {
    @ObservedObject var bleManager: BruceBLEManager
    
    @Query(sort: \BruceDevice.dateAdded) private var savedDevices: [BruceDevice]
    @State private var selectedDeviceID: UUID?
    
    @State private var currentPath: String = "/"
    @State private var showingCreateModal = false
    @State private var isCreatingFolder = true
    @State private var newItemName = ""
    @State private var showingFilePicker = false
    @State private var showingShareSheet = false
    
    // FIX: Komplexe Abfrage ausgelagert, um den SwiftUI Compiler zu entlasten
    private var activeDeviceName: String {
        savedDevices.first(where: { $0.id == selectedDeviceID })?.name ?? "SELECT HW"
    }
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack(spacing: 16) {
                // 1. LIQUID GLASS HEADER
                HStack(spacing: 10) {
                    Menu {
                        ForEach(savedDevices) { device in
                            Button(action: {
                                selectedDeviceID = device.id
                                bleManager.selectActiveDevice(uuid: device.id)
                                currentPath = "/"
                                refreshDirectory()
                            }) {
                                HStack {
                                    Text(device.name)
                                        .font(.custom("doto", size: 14))
                                    if selectedDeviceID == device.id { Image(systemName: "checkmark") }
                                }
                            }
                        }
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "cpu")
                            Text(activeDeviceName)
                                .font(.custom("doto", size: 14)) // Nutzt die bereinigte Eigenschaft
                            Image(systemName: "chevron.down").font(.system(size: 10))
                        }
                        .font(.custom("doto", size: 14)).fontWeight(.bold)
                        .foregroundStyle(.black).padding(.horizontal, 10).padding(.vertical, 6)
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 8))
                    }
                    
                    Text(":").font(.custom("doto", size: 14)).foregroundStyle(.gray)
                    
                    HStack(spacing: 4) {
                        Image(systemName: "folder").font(.caption)
                        Text(currentPath).lineLimit(1).truncationMode(.head)
                    }
                    .font(.custom("doto", size: 14)).foregroundStyle(.white.opacity(0.8))
                    
                    Spacer()
                    
                    if currentPath != "/" {
                        Button(action: goUpOneDirectory) {
                            Image(systemName: "arrow.uturn.backward").font(.caption).foregroundStyle(.white).padding(8)
                                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 8))
                                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.white.opacity(0.3), lineWidth: 1))
                        }
                    }
                }
                .padding(.horizontal).padding(.top, 8)
                
                // 2. DATEI-LISTE
                ScrollView {
                    VStack(spacing: 12) {
                        if bleManager.remoteFiles.isEmpty {
                            VStack(spacing: 12) {
                                if bleManager.isFileFolderLoading {
                                    ProgressView().tint(.white)
                                    Text("Reading storage over BLE...")
                                        .font(.custom("doto", size: 14))
                                } else {
                                    Image(systemName: "tray").font(.system(size: 40, weight: .thin))
                                    Text("Directory empty or offline")
                                        .font(.custom("doto", size: 14))
                                }
                            }
                            .font(.custom("doto", size: 14)).foregroundStyle(.gray).padding(.top, 60)
                        } else {
                            ForEach(bleManager.remoteFiles) { item in
                                Button(action: { if item.isDirectory { enterDirectory(name: item.name) } }) {
                                    HStack(spacing: 16) {
                                        Image(systemName: item.isDirectory ? "folder" : "doc.text").font(.title3).foregroundStyle(.white)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(item.name).font(.custom("doto", size: 14)).fontWeight(.semibold).foregroundStyle(.white).lineLimit(1)
                                            if let size = item.size { Text(size).font(.custom("doto", size: 14)).foregroundStyle(.gray) }
                                        }
                                        Spacer()
                                        if item.isDirectory { Image(systemName: "chevron.right").font(.custom("doto", size: 14)).foregroundStyle(.gray) }
                                    }
                                    .padding().background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
                                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.15), lineWidth: 1))
                                    .contextMenu {
                                        if !item.isDirectory {
                                            Button {
                                                let targetPath = currentPath == "/" ? "/\(item.name)" : "\(currentPath)/\(item.name)"
                                                bleManager.sendCommand("FS_DOWNLOAD_START \(targetPath)")
                                            } label: {
                                                Label("Download to iPhone", systemImage: "square.and.arrow.down")
                                            }
                                        }
                                        
                                        Button(role: .destructive) { deleteItem(name: item.name) } label: {
                                            Label("Delete", systemImage: "trash")
                                        }
                                    }
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding(.horizontal)
                }
                .refreshable { refreshDirectory() }
                
                // 3. ACTION BAR
                HStack(spacing: 16) {
                    Button(action: { isCreatingFolder = true; newItemName = ""; showingCreateModal = true }) {
                        Label("FOLDER", systemImage: "folder.badge.plus").font(.custom("doto", size: 14)).fontWeight(.bold).foregroundStyle(.white).frame(maxWidth: .infinity).padding()
                            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12)).overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.2), lineWidth: 1))
                    }
                    Button(action: { showingFilePicker = true }) {
                        Label("UPLOAD", systemImage: "arrow.up.doc").font(.custom("doto", size: 14)).fontWeight(.bold).foregroundStyle(.black).frame(maxWidth: .infinity).padding()
                            .background(Color.white, in: RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(.horizontal).padding(.bottom, 16)
            }
            
            if bleManager.isDownloading {
                Color.black.opacity(0.6).ignoresSafeArea()
                VStack(spacing: 20) {
                    ProgressView().tint(.white).scaleEffect(1.5)
                    Text("DOWNLOADING...").font(.custom("doto", size: 14)).foregroundStyle(.white)
                    Text(bleManager.downloadingFileName).font(.custom("doto", size: 14)).foregroundStyle(.gray)
                }
                .padding(30).background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 24))
                .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.2), lineWidth: 1))
            }
            
            if showingCreateModal {
                Color.black.opacity(0.6).ignoresSafeArea()
                VStack(spacing: 20) {
                    Text(isCreatingFolder ? "NEW FOLDER" : "NEW FILE")
                        .font(.custom("doto", size: 14))
                        .foregroundStyle(.white)
                        .padding()
                        .background(Color.black.opacity(0.4))
                        .cornerRadius(12) // Macht den Hintergrund rund
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color.white.opacity(0.3), lineWidth: 1)
                        )
                    HStack(spacing: 12) {
                        Button("CANCEL") { showingCreateModal = false }.font(.custom("doto", size: 14)).foregroundStyle(.gray).padding()
                        Spacer()
                        Button("CREATE") { if !newItemName.isEmpty { executeCreate() } }.font(.custom("doto", size: 14)).fontWeight(.bold).foregroundStyle(.black).padding(.horizontal, 20).padding(.vertical, 10).background(Color.white, in: RoundedRectangle(cornerRadius: 10)).disabled(newItemName.isEmpty)
                    }
                }
                .padding(24).background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 24)).overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.2), lineWidth: 1)).padding(.horizontal, 32)
            }
        }
        .navigationTitle("File Manager")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            if let activeID = bleManager.activePeripheral?.identifier { selectedDeviceID = activeID }
            else if let firstSaved = savedDevices.first { selectedDeviceID = firstSaved.id; bleManager.selectActiveDevice(uuid: firstSaved.id) }
            refreshDirectory()
        }
        .onChange(of: bleManager.lastDownloadedFileURL) { _, newValue in
            if newValue != nil { showingShareSheet = true }
        }
        .sheet(isPresented: $showingShareSheet) {
            if let fileURL = bleManager.lastDownloadedFileURL {
                ActivityViewController(activityItems: [fileURL]).preferredColorScheme(.dark)
            }
        }
        .fileImporter(isPresented: $showingFilePicker, allowedContentTypes: [.data, .text], allowsMultipleSelection: false) { result in
            switch result {
            case .success(let urls): if let url = urls.first { uploadLocalFile(at: url) }
            case .failure(let error): print("Picker Error: \(error.localizedDescription)")
            }
        }
    }
    
    private func refreshDirectory() {
        bleManager.remoteFiles.removeAll()
        bleManager.isFileFolderLoading = true
        bleManager.sendCommand("FS_LIST \(currentPath)")
    }
    
    private func enterDirectory(name: String) {
        currentPath = currentPath == "/" ? "/\(name)" : "\(currentPath)/\(name)"
        refreshDirectory()
    }
    
    private func goUpOneDirectory() {
        let components = currentPath.components(separatedBy: "/")
        if components.count <= 2 { currentPath = "/" }
        else {
            let remaining = Array(components.dropLast()).joined(separator: "/")
            currentPath = remaining.isEmpty ? "/" : remaining
        }
        refreshDirectory()
    }
    
    private func executeCreate() {
        showingCreateModal = false
        let fullPath = currentPath == "/" ? "/\(newItemName)" : "\(currentPath)/\(newItemName)"
        if isCreatingFolder { bleManager.sendCommand("FS_MKDIR \(fullPath)") }
        else { bleManager.sendCommand("FS_CREATE \(fullPath)") }
        newItemName = ""
        refreshDirectory()
    }
    
    private func deleteItem(name: String) {
        let targetPath = currentPath == "/" ? "/\(name)" : "\(currentPath)/\(name)"
        bleManager.sendCommand("FS_REMOVE \(targetPath)")
        refreshDirectory()
    }
    
    private func uploadLocalFile(at url: URL) {
        guard url.startAccessingSecurityScopedResource() else { return }
        defer { url.stopAccessingSecurityScopedResource() }
        if let fileData = try? Data(contentsOf: url) {
            let fileName = url.lastPathComponent
            let targetPath = currentPath == "/" ? "/\(fileName)" : "\(currentPath)/\(fileName)"
            let hexArray: [String] = fileData.map { String(format: "%02hhx", $0) }
            let hexString = hexArray.joined(separator: "")
            bleManager.sendCommand("FS_UPLOAD_START \(targetPath) \(fileData.count)")
            let chunkSize = 400
            var index = 0
            while index < hexString.count {
                let start = hexString.index(hexString.startIndex, offsetBy: index)
                let end = hexString.index(start, offsetBy: min(chunkSize, hexString.count - index))
                bleManager.sendCommand("FS_UPLOAD_CHUNK \(String(hexString[start..<end]))")
                index += chunkSize
            }
            bleManager.sendCommand("FS_UPLOAD_END")
            refreshDirectory()
        }
    }
}

struct ActivityViewController: UIViewControllerRepresentable {
    let activityItems: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController { UIActivityViewController(activityItems: activityItems, applicationActivities: nil) }
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
