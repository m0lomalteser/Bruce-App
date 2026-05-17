//
//  ContentView.swift
//  BruceApp
//
//  Created by Marlin Schuck on 28.04.26.
//

import SwiftUI

struct ContentView: View {
    // Angenommen, BruceBLEManager existiert in deinem Projekt
    @StateObject private var bleManager = BruceBLEManager()
    
    init() {
        // Hier ist der Trick für die Schriftart/Größe aller Tab-Items:
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        
        // Schriftart für den normalen Zustand
        if let customFont = UIFont(name: "doto", size: 14) {
            appearance.stackedLayoutAppearance.normal.titleTextAttributes = [.font: customFont]
            appearance.stackedLayoutAppearance.selected.titleTextAttributes = [.font: customFont]
        }
        
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some View {
        TabView {
            MyDevicesView(bleManager: bleManager)
                .tabItem {
                    // Shader entfernt – das funktioniert hier leider nicht direkt
                    Label("", systemImage: "square.grid.2x2.fill")
                }
            
            FilesView(bleManager: bleManager)
                .tabItem {
                    Label("", systemImage: "folder.fill")
                }
            
            AppStoreView(bleManager: bleManager)
                .tabItem {
                    Label("", systemImage: "app.gift")
                }
            
            // SettingsView()
            //    .tabItem {
            //       // .font(...) entfernt, wird jetzt oben im init() global geregelt
            //       Label("", systemImage: "gear")
            //   }
        }
    }
}
