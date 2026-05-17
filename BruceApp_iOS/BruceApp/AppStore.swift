//
//  AppStore.swift
//  BruceApp
//
//  Created by Marlin Schuck on 17.05.26.
//

import SwiftUI
import Combine
import Foundation

// MARK: - DTOs & Models
struct CategoriesResponse: Codable {
    let totalCategories: Int?
    let categories: [StoreCategory]
}

struct CornerRadiusKey: EnvironmentKey {
    static let defaultValue: CGFloat = 16.0
}

struct CategoryDetailResponse: Codable {
    let category: String?
    let slug: String?
    let count: Int?
    let apps: [AppScript]
}

struct StoreCategory: Identifiable, Hashable, Codable {
    var id: String { slug ?? UUID().uuidString }
    let name: String?
    let slug: String?
    let count: Int?
    
    var displayName: String { name ?? "Unbekannt" }
    var displayCount: Int { count ?? 0 }
}

struct AppScript: Identifiable, Hashable, Codable {
    var id: String { s ?? UUID().uuidString }
    let n: String?
    let d: String?
    let v: String?
    let s: String?
    
    var name: String { n ?? "Unbekannte App" }
    var description: String { d ?? "Keine Beschreibung verfügbar." }
    var version: String { v ?? "UNKNOWN" }
    var repoSlug: String { s ?? "" }
    
    var installedVersion: String? = nil
}

// MARK: - View Model
@MainActor
class StoreViewModel: ObservableObject {
    @Published var categories: [StoreCategory] = []
    @Published var availableScripts: [AppScript] = []
    
    @Published var isLoadingCategories: Bool = false
    @Published var isLoadingScripts: Bool = false
    @Published var popupMessage: String? = nil
    @Published var installationProgress: Double = 0.0
    @Published var isInstalling: Bool = false
    
    let baseURL = "http://ghp.iceis.co.uk"
    private var cancellables = Set<AnyCancellable>()
    
    func fetchCategories() async {
        guard categories.isEmpty else { return }
        isLoadingCategories = true
        
        guard let url = URL(string: "\(baseURL)/service/main/releases/categories.json") else { return }
        
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let response = try JSONDecoder().decode(CategoriesResponse.self, from: data)
            self.categories = response.categories.filter { $0.slug != "updates" }
            self.isLoadingCategories = false
        } catch {
            print("Fehler beim Laden der Kategorien: \(error)")
            self.popupMessage = "Fehler beim Verbinden"
            self.isLoadingCategories = false
            clearPopupAfterDelay()
        }
    }
    
    // AKTIVIERTER SD-KARTEN SCANNER: Holt Store-Daten und synct parallel den Hardware-Inhalt
    func fetchAppsAndScanStorage(for category: StoreCategory, bleManager: BruceBLEManager) async {
        isLoadingScripts = true
        self.availableScripts = []
        
        guard let slug = category.slug,
              let url = URL(string: "\(baseURL)/service/main/releases/category-\(slug).min.json") else {
            isLoadingScripts = false
            return
        }
        
        // 1. Store-Inhalte aus dem Web holen
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let response = try JSONDecoder().decode(CategoryDetailResponse.self, from: data)
            var fetchedApps = response.apps
            
            // 2. Speicher-Scan auf dem Bruce-Board via BLE triggern
            if bleManager.connectionState == .paired {
                bleManager.remoteFiles.removeAll()
                bleManager.isFileFolderLoading = true
                
                // Nutze den passenden Kategorienamen aus dem Manifest für das Dateisystem
                let targetCategoryName = response.category ?? category.displayName
                bleManager.sendCommand("FS_LIST /BruceJS/\(targetCategoryName)")
                
                // Kurzer asynchroner Überwachung-Loop, bis die Hardware fertig gestreamt hat (max 3 Sekunden)
                let startTime = Date()
                while bleManager.isFileFolderLoading && Date().timeIntervalSince(startTime) < 3.0 {
                    try? await Task.sleep(nanoseconds: 100_000_000) // 100ms yield
                }
                
                // 3. Abgleich: Wenn der Ordnername auf der SD existiert, setzen wir den Status auf installiert
                let localFolderNames = bleManager.remoteFiles.filter { $0.isDirectory }.map { $0.name }
                
                for i in 0..<fetchedApps.count {
                    if localFolderNames.contains(fetchedApps[i].name) {
                        // Markiert die App als installiert (Nutzt die Manifest-Version als Fallback)
                        fetchedApps[i].installedVersion = fetchedApps[i].version
                    }
                }
            }
            
            self.availableScripts = fetchedApps
            self.isLoadingScripts = false
        } catch {
            print("Fehler: \(error)")
            self.popupMessage = "Fehler beim Laden"
            self.isLoadingScripts = false
            clearPopupAfterDelay()
        }
    }
    
    // NATIVE BRUCE-INSTALLATIONS-ENGINE
    func installApp(script: AppScript, categorySlug: String, bleManager: BruceBLEManager) async {
        guard bleManager.connectionState == .paired else {
            self.popupMessage = "Hardware nicht verbunden!"
            clearPopupAfterDelay()
            return
        }
        
        self.isInstalling = true
        self.popupMessage = "Lade App Metadaten..."
        
        let metadataUrlString = "\(baseURL)/service/main/repositories/\(script.repoSlug.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? "")/metadata.json"
        guard let metadataUrl = URL(string: metadataUrlString) else {
            self.popupMessage = "Metadaten-URL ungültig"
            self.isInstalling = false
            clearPopupAfterDelay()
            return
        }
        
        do {
            let (metaData, metaResponse) = try await URLSession.shared.data(from: metadataUrl)
            guard (metaResponse as? HTTPURLResponse)?.statusCode == 200 else {
                self.popupMessage = "App-Manifest fehlt"
                self.isInstalling = false
                clearPopupAfterDelay()
                return
            }
            
            guard let json = try? JSONSerialization.jsonObject(with: metaData, options: []) as? [String: Any],
                  let files = json["files"] as? [Any],
                  let repoPath = json["path"] as? String,
                  let owner = json["owner"] as? String,
                  let repo = json["repo"] as? String,
                  let commit = json["commit"] as? String,
                  let categoryName = json["category"] as? String else {
                self.popupMessage = "Manifest korrupt"
                self.isInstalling = false
                clearPopupAfterDelay()
                return
            }
            
            let baseDir = "/BruceJS/\(categoryName)"
            let appDir = "\(baseDir)/\(script.name)"
            
            bleManager.sendCommand("FS_MKDIR /BruceJS")
            try await Task.sleep(nanoseconds: 40_000_000)
            bleManager.sendCommand("FS_MKDIR \(baseDir)")
            try await Task.sleep(nanoseconds: 40_000_000)
            bleManager.sendCommand("FS_MKDIR \(appDir)")
            try await Task.sleep(nanoseconds: 40_000_000)
            
            var currentFileIndex = 1
            let totalFilesCount = files.count
            
            for fileElement in files {
                var fileName = ""
                var sourceName = ""
                
                if let fileDict = fileElement as? [String: String] {
                    fileName = fileDict["destination"] ?? ""
                    sourceName = fileDict["source"] ?? ""
                } else if let fileStr = fileElement as? String {
                    fileName = fileStr
                    sourceName = fileStr
                }
                
                fileName = fileName.replacingOccurrences(of: "^/+", with: "", options: .regularExpression)
                sourceName = sourceName.replacingOccurrences(of: "^/+", with: "", options: .regularExpression)
                
                self.popupMessage = "Lade Datei \(currentFileIndex)/\(totalFilesCount)..."
                
                let fullRepoPath = "\(repoPath)\(sourceName)".replacingOccurrences(of: "^/+", with: "", options: .regularExpression)
                let fileUrlString = "\(baseURL)/service/manual/\(owner)/\(repo)/\(commit)/\(fullRepoPath)".addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
                
                guard let fileUrl = URL(string: fileUrlString) else { continue }
                let (fileData, fileResponse) = try await URLSession.shared.data(from: fileUrl)
                
                if (fileResponse as? HTTPURLResponse)?.statusCode == 200 {
                    let destinationFilePath = "\(appDir)/\(fileName)"
                    
                    let hexArray = fileData.map { String(format: "%02hhx", $0) }
                    let hexString = hexArray.joined(separator: "")
                    
                    bleManager.sendCommand("FS_UPLOAD_START \(destinationFilePath) \(fileData.count)")
                    try await Task.sleep(nanoseconds: 60_000_000)
                    
                    let chunkSize = 400
                    var currentIndex = 0
                    
                    while currentIndex < hexString.count {
                        let start = hexString.index(hexString.startIndex, offsetBy: currentIndex)
                        let end = hexString.index(start, offsetBy: min(chunkSize, hexString.count - currentIndex))
                        let chunk = String(hexString[start..<end])
                        
                        bleManager.sendCommand("FS_UPLOAD_CHUNK \(chunk)")
                        currentIndex += chunkSize
                        
                        try await Task.sleep(nanoseconds: 12_000_000)
                    }
                    
                    bleManager.sendCommand("FS_UPLOAD_END")
                    try await Task.sleep(nanoseconds: 40_000_000)
                }
                
                currentFileIndex += 1
                self.installationProgress = Double(currentFileIndex - 1) / Double(totalFilesCount)
            }
            
            if let index = availableScripts.firstIndex(where: { $0.id == script.id }) {
                availableScripts[index].installedVersion = script.version
            }
            
            self.popupMessage = "\(script.name) bereit!"
            self.isInstalling = false
            clearPopupAfterDelay()
            
        } catch {
            print("Fehler: \(error)")
            self.popupMessage = "Installations-Fehler"
            self.isInstalling = false
            clearPopupAfterDelay()
        }
    }
    
    func deleteApp(script: AppScript, categorySlug: String, bleManager: BruceBLEManager) {
        let targetPath = "/BruceJS/\(categorySlug)/\(script.name)"
        bleManager.sendCommand("FS_REMOVE \(targetPath)")
        
        if let index = availableScripts.firstIndex(where: { $0.id == script.id }) {
            availableScripts[index].installedVersion = nil
        }
        
        self.popupMessage = "App gelöscht"
        clearPopupAfterDelay()
    }
    
    private func clearPopupAfterDelay() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            self.popupMessage = nil
            self.installationProgress = 0.0
        }
    }
}

// MARK: - REAL Liquid Glass Modifier
struct LiquidGlassModifier: ViewModifier {
    var tintColor: Color? = nil
    var cornerRadius: CGFloat = 16.0
    
    func body(content: Content) -> some View {
        content
            .padding()
            .background(
                ZStack {
                    Rectangle()
                        .fill(.ultraThinMaterial)
                    
                    if let tintColor = tintColor {
                        tintColor.opacity(0.15)
                    }
                }
            )
            .environment(\.colorScheme, .dark)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(LinearGradient(
                        colors: [.white.opacity(0.5), .clear, .white.opacity(0.2)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ), lineWidth: 1.5)
            )
            .shadow(color: .black.opacity(0.3), radius: 10, x: 0, y: 5)
    }
}

extension View {
    func liquidGlass(tint: Color? = nil, cornerRadius: CGFloat = 16.0) -> some View {
        self.modifier(LiquidGlassModifier(tintColor: tint, cornerRadius: cornerRadius))
    }
}

// MARK: - Main View
struct AppStoreView: View {
    @ObservedObject var bleManager: BruceBLEManager
    @StateObject private var viewModel = StoreViewModel()
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()
                
                if viewModel.isLoadingCategories {
                    ProgressView("Loading Categories...")
                        .font(.custom("doto", size: 14))
                        .foregroundColor(.white)
                        .padding()
                        .liquidGlass()
                } else {
                    ScrollView {
                        VStack(spacing: 20) {
                            ForEach(viewModel.categories) { category in
                                NavigationLink(value: category) {
                                    CategoryGlassCard(category: category)
                                }
                                .buttonStyle(PlainButtonStyle())
                            }
                        }
                        .padding()
                    }
                }
                
                if let message = viewModel.popupMessage {
                    VStack(spacing: 12) {
                        Spacer()
                        VStack(spacing: 8) {
                            Text(message)
                                .font(.custom("doto", size: 14))
                                .foregroundColor(.white)
                            
                            if viewModel.isInstalling {
                                ProgressView(value: viewModel.installationProgress, total: 1.0)
                                    .tint(.white)
                                    .frame(width: 180)
                            }
                        }
                        .padding()
                        .liquidGlass(tint: .cyan, cornerRadius: 24)
                        .transition(.scale(scale: 0.8).combined(with: .opacity).combined(with: .move(edge: .bottom)))
                        .animation(.spring(response: 0.4, dampingFraction: 0.7), value: viewModel.popupMessage)
                    }
                    .padding(.bottom, 40)
                    .zIndex(1)
                }
            }
            .navigationTitle("Bruce App Store")
            .navigationDestination(for: StoreCategory.self) { category in
                ScriptListView(category: category, viewModel: viewModel, bleManager: bleManager)
            }
            .task {
                await viewModel.fetchCategories()
            }
        }
        .preferredColorScheme(.dark)
    }
}

// MARK: - Category List Card
struct CategoryGlassCard: View {
    let category: StoreCategory
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 5) {
                Text(category.displayName)
                    .font(.custom("doto", size: 14))
                    .fontWeight(.bold)
                    .foregroundColor(category.displayName == "Updates" ? .orange : .white)
                
                Text("\(category.displayCount) App(s)")
                    .font(.custom("doto", size: 14))
                    .foregroundColor(.white.opacity(0.7))
            }
            Spacer()
            Image(systemName: "chevron.right")
                .foregroundColor(.white.opacity(0.5))
        }
        .liquidGlass()
    }
}

// MARK: - Script List View (Apps Liste)
struct ScriptListView: View {
    let category: StoreCategory
    @ObservedObject var viewModel: StoreViewModel
    @ObservedObject var bleManager: BruceBLEManager
    @State private var selectedScript: AppScript?
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            if viewModel.isLoadingScripts {
                ProgressView("Scaning SD-Card...")
                    .font(.custom("doto", size: 14))
                    .foregroundColor(.white)
                    .padding()
                    .liquidGlass()
            } else {
                ScrollView {
                    VStack(spacing: 16) {
                        if viewModel.availableScripts.isEmpty {
                            Text("No Apps in this Categorie.")
                                .font(.custom("doto", size: 14))
                                .foregroundColor(.white)
                                .padding()
                        } else {
                            ForEach(viewModel.availableScripts) { script in
                                ScriptGlassCard(script: script)
                                    .onTapGesture { selectedScript = script }
                            }
                        }
                    }
                    .padding()
                }
            }
        }
        .navigationTitle(category.displayName)
        // HIER WIRD DER AUTOMATISCHE SPEICHER-ABGLEICH BEIM ÖFFNEN GETRIGGERT
        .task {
            await viewModel.fetchAppsAndScanStorage(for: category, bleManager: bleManager)
        }
        .confirmationDialog("Select Action", isPresented: Binding(
            get: { selectedScript != nil },
            set: { if !$0 { selectedScript = nil } }
        ), titleVisibility: .visible) {
            if let script = selectedScript {
                if let _ = script.installedVersion {
                    Button("Update / Reinstall") {
                        Task { await viewModel.installApp(script: script, categorySlug: category.slug ?? "generic", bleManager: bleManager) }
                    }
                    .font(.custom("doto", size: 14))
                    Button("Delete", role: .destructive) {
                        viewModel.deleteApp(script: script, categorySlug: category.slug ?? "generic", bleManager: bleManager)
                    }
                    .font(.custom("doto", size: 14))
                } else {
                    Button("Install") {
                        Task { await viewModel.installApp(script: script, categorySlug: category.slug ?? "generic", bleManager: bleManager) }
                    }
                    .font(.custom("doto", size: 14))
                }
                Button("Cancel", role: .cancel) {}
                    .font(.custom("doto", size: 14))
            }
        }
    }
}

// MARK: - Script Item Card
struct ScriptGlassCard: View {
    let script: AppScript
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(script.name)
                    .font(.custom("doto", size: 14))
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                Spacer()
                statusBadge
            }
            
            Text(script.description)
                .font(.custom("doto", size: 14))
                .foregroundColor(.gray)
                .lineLimit(3)
            
            HStack {
                Text("Version: \(script.version)")
                    .font(.custom("doto", size: 14))
                    .foregroundColor(.gray)
                Spacer()
                if let installed = script.installedVersion {
                    Text("Installed")
                        .font(.custom("doto", size: 14))
                        .foregroundColor(.green)
                }
            }
        }
        .liquidGlass()
    }
    
    @ViewBuilder
    var statusBadge: some View {
        if let installed = script.installedVersion {
            if installed != script.version {
                Text("UPDATE")
                    .font(.custom("doto", size: 14))
                    .bold()
                    .padding(.horizontal, 8).padding(.vertical, 4)
                    .liquidGlass(tint: .orange, cornerRadius: 20)
                    .foregroundColor(.white)
            } else {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
            }
        } else {
            Image(systemName: "arrow.down.circle")
                .foregroundColor(.white)
        }
    }
}
