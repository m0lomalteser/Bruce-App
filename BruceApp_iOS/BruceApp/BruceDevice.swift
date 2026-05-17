//
//  BruceDevice.swift
//  BruceApp
//
//  Created by Marlin Schuck on 15.05.26.
//

import Foundation
import SwiftData

@Model
final class BruceDevice {
    // Unique-Schutz bleibt im Hintergrund aktiv, um Duplikate zu verhindern
    @Attribute(.unique) var id: UUID
    var name: String
    var category: String
    var dateAdded: Date
    var savedPin: String? // Speichert die PIN für den Auto-Login
    
    // Deine originale Bildlogik! (Computed Property wird nicht in der DB gespeichert, sondern live berechnet)
    var imageName: String {
        return name.lowercased()
            .replacingOccurrences(of: " ", with: "_")
            .replacingOccurrences(of: "-", with: "_")
            .replacingOccurrences(of: "(", with: "")
            .replacingOccurrences(of: ")", with: "")
            .replacingOccurrences(of: "\"", with: "")
    }
    
    init(id: UUID, name: String, category: String = "Detected Hardware", dateAdded: Date = .now, savedPin: String? = nil) {
        self.id = id
        self.name = name
        self.category = category
        self.dateAdded = dateAdded
        self.savedPin = savedPin
    }
}
