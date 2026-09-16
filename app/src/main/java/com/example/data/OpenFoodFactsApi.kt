package com.example.data

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import java.io.IOException

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OFFResponse(
    @param:Json(name = "products") val products: List<OFFProduct>?
)

@JsonClass(generateAdapter = true)
data class OFFProduct(
    @param:Json(name = "product_name") val product_name: String? = null,
    @param:Json(name = "brands") val brands: String? = null,
    @param:Json(name = "serving_quantity") val serving_quantity: Any? = null,
    @param:Json(name = "serving_size") val serving_size: String? = null,
    @param:Json(name = "nutriments") val nutriments: OFFNutriments? = null
)

@JsonClass(generateAdapter = true)
data class OFFNutriments(
    @param:Json(name = "energy-kcal_100g") val energy_kcal_100g: Any? = null,
    @param:Json(name = "energy-kj_100g") val energy_kj_100g: Any? = null,
    @param:Json(name = "energy_100g") val energy_100g: Any? = null,
    @param:Json(name = "proteins_100g") val proteins_100g: Any? = null,
    @param:Json(name = "carbohydrates_100g") val carbohydrates_100g: Any? = null,
    @param:Json(name = "fat_100g") val fat_100g: Any? = null
)

@JsonClass(generateAdapter = true)
data class OFFBarcodeResponse(
    @param:Json(name = "product") val product: OFFProduct?,
    @param:Json(name = "status") val status: Int?
)

interface OpenFoodFactsApi {
    @GET("cgi/search.pl?search_simple=1&action=process&json=1&page_size=20&sort_by=unique_scans_n&fields=product_name,brands,serving_quantity,serving_size,nutriments")
    suspend fun searchFoods(
        @Query("search_terms") query: String
    ): OFFResponse

    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): OFFBarcodeResponse

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"

        fun create(cacheDir: File? = null): OpenFoodFactsApi {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val clientBuilder = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "FoodLab/1.0 Android (contact: https://github.com/ManuRoma02/FoodLab)")
                        .cacheControl(CacheControl.Builder().maxAge(15, TimeUnit.MINUTES).build())
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor { chain ->
                    val request = chain.request()
                    try {
                        chain.proceed(request)
                    } catch (networkError: IOException) {
                        chain.proceed(
                            request.newBuilder()
                                .cacheControl(
                                    CacheControl.Builder()
                                        .onlyIfCached()
                                        .maxStale(7, TimeUnit.DAYS)
                                        .build()
                                )
                                .build()
                        )
                    }
                }

            cacheDir?.let { directory ->
                clientBuilder.cache(Cache(File(directory, "open_food_facts_http"), 25L * 1024L * 1024L))
                clientBuilder.addNetworkInterceptor { chain ->
                    val response = chain.proceed(chain.request())
                    response.newBuilder()
                        .removeHeader("Pragma")
                        .removeHeader("Cache-Control")
                        .header("Cache-Control", "public, max-age=86400")
                        .build()
                }
            }

            val okHttpClient = clientBuilder.build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(OpenFoodFactsApi::class.java)
        }
    }
}
