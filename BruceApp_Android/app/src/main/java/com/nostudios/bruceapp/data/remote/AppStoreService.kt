package com.nostudios.bruceapp.data.remote

import com.nostudios.bruceapp.data.model.CategoriesResponse
import com.nostudios.bruceapp.data.model.CategoryDetailResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface AppStoreService {
    @GET("service/main/releases/categories.json")
    suspend fun getCategories(): CategoriesResponse

    @GET("service/main/releases/category-{slug}.min.json")
    suspend fun getCategoryApps(@Path("slug") slug: String): CategoryDetailResponse

    companion object {
        private const val BASE_URL = "http://ghp.iceis.co.uk/"

        fun create(): AppStoreService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AppStoreService::class.java)
        }
    }
}
