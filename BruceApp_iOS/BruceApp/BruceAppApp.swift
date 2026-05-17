//
//  BruceAppApp.swift
//  BruceApp
//
//  Created by Marlin Schuck on 15.05.26.
//

import SwiftUI
import SwiftData

@main
struct BruceAppApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        // Initialize SwiftData for the BruceDevice model
        .modelContainer(for: BruceDevice.self)
    }
}
