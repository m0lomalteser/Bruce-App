package com.nostudios.bruceapp.data.model

import com.google.gson.annotations.SerializedName

data class CategoriesResponse(
    @SerializedName("totalCategories") val totalCategories: Int?,
    @SerializedName("categories") val categories: List<StoreCategory>
)

data class CategoryDetailResponse(
    @SerializedName("category") val category: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("count") val count: Int?,
    @SerializedName("apps") val apps: List<AppScript>
)

data class StoreCategory(
    @SerializedName("name") val name: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("count") val count: Int?
) {
    val displayName: String get() = name ?: "Unknown"
    val displayCount: Int get() = count ?: 0
}

data class AppScript(
    @SerializedName("n") val n: String?,
    @SerializedName("d") val d: String?,
    @SerializedName("v") val v: String?,
    @SerializedName("s") val s: String?
) {
    val name: String get() = n ?: "Unknown App"
    val description: String get() = d ?: "No description available."
    val version: String get() = v ?: "UNKNOWN"
    val repoSlug: String get() = s ?: ""

    var installedVersion: String? = null
}
