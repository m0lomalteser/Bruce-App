//
//  SupportedDevicesData.swift
//  BruceApp
//
//  Created by Marlin Schuck on 15.05.26.
//

import Foundation

struct SupportedDevice: Identifiable, Hashable {
    let id = UUID()
    let name: String
    let category: String
    let requiresExternalModules: Bool
    let isLegacy: Bool
}

struct BruceFirmwareData {
    static let categories = [
        "M5Stack Hardware",
        "LilyGo Hardware",
        "Dedicated Dev Boards & Community PCB"
    ]
    
    static let allDevices: [SupportedDevice] = [
        // M5Stack Hardware
        SupportedDevice(name: "M5Stack Cardputer", category: "M5Stack Hardware", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "M5Stack Cardputer Adv", category: "M5Stack Hardware", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "M5Stack StickC-Plus (v1.1)", category: "M5Stack Hardware", requiresExternalModules: false, isLegacy: true),
        SupportedDevice(name: "M5Stack StickC-Plus 2", category: "M5Stack Hardware", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "M5Stack Core", category: "M5Stack Hardware", requiresExternalModules: false, isLegacy: true),
        SupportedDevice(name: "M5Stack Core2", category: "M5Stack Hardware", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "M5StickS3", category: "M5Stack Hardware", requiresExternalModules: false, isLegacy: false),
        
        // LilyGo Hardware
        SupportedDevice(name: "LilyGo T-Deck", category: "LilyGo Hardware", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "LilyGo T-Embed", category: "LilyGo Hardware", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "LilyGo T-Embed CC1101", category: "LilyGo Hardware", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "LilyGo T-LoRa Pager", category: "LilyGo Hardware", requiresExternalModules: false, isLegacy: false),
        
        // Dedicated Dev Boards & Community PCB
        SupportedDevice(name: "Bruce RF Reaper (Official Open-Source PCB)", category: "Dedicated Dev Boards & Community PCB", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "CYD / ESP32-2432S028 (Cheap Yellow Display)", category: "Dedicated Dev Boards & Community PCB", requiresExternalModules: true, isLegacy: false),
        SupportedDevice(name: "Marauder Mini v4", category: "Dedicated Dev Boards & Community PCB", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "Marauder Mini v6", category: "Dedicated Dev Boards & Community PCB", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "Marauder Mini v6.x", category: "Dedicated Dev Boards & Community PCB", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "Marauder Mini v7", category: "Dedicated Dev Boards & Community PCB", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "AWOK Touch v2", category: "Dedicated Dev Boards & Community PCB", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "Awok Mini v2", category: "Dedicated Dev Boards & Community PCB", requiresExternalModules: false, isLegacy: false),
        SupportedDevice(name: "ESP32-S3-DevKitC-1 (Headless / WebUI Mode)", category: "Dedicated Dev Boards & Community PCB", requiresExternalModules: true, isLegacy: false),
        SupportedDevice(name: "Other (Custom Builds / Other)", category: "Undefined Type", requiresExternalModules: false, isLegacy: false)
    ]
}
