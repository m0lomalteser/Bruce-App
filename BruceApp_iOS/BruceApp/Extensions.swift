//
//  Extensions.swift
//  BruceApp
//
//  Created by Marlin Schuck on 17.05.26.
//

import Foundation

extension Array {
    func chunked(into size: Int) -> [[Element]] {
        stride(from: 0, to: count, by: size).map {
            Array(self[$0..<Swift.min($0 + size, count)])
        }
    }
}
