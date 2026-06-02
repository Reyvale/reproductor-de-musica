package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class PartMoshi(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ContentMoshi(
    @Json(name = "parts") val parts: List<PartMoshi>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequestMoshi(
    @Json(name = "contents") val contents: List<ContentMoshi>
)

@JsonClass(generateAdapter = true)
data class CandidateMoshi(
    @Json(name = "content") val content: ContentMoshi
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponseMoshi(
    @Json(name = "candidates") val candidates: List<CandidateMoshi>? = null
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequestMoshi
    ): GenerateContentResponseMoshi
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiService::class.java)
    }

    suspend fun analyzeAudioState(
        presetName: String,
        bands: List<Float>,
        preamp: Float,
        bass: Float,
        treble: Float,
        format: String,
        bitrate: String,
        sampleRate: String,
        filter: String
    ): String {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            return "Error: Clave de API de Gemini no configurada o vacía. Por favor introduce tu clave en el panel de Secrets de AI Studio."
        }

        val prompt = """
            Eres un ingeniero de sonido profesional de alta fidelidad. Analiza con detalle la siguiente configuración actual del reproductor de música:
            
            - Ecualizador: Presets "$presetName"
            - Ganancia de frecuencias (31Hz a 16kHz): ${bands.joinToString { "${it}dB" }}
            - Preamplificador (Preamp): ${preamp}dB
            - Refuerzo de Graves (Bass Boost): $bass%
            - Refuerzo de Agudos (Treble Boost): $treble%
            - Formato de Audio reproducido: $format ($bitrate / $sampleRate)
            - Filtro de Repliegue DAC configurado: $filter
            
            Dame una valoración experta, breve y súper pulida en español explicando:
            1. Cuál es el carácter acústico que tiene esta curva (ej. si es cálida, analítica, con exceso de sub-graves, etc.).
            2. Qué mejoras acústicas sugiere para sacarle todo el partido a este formato lossless.
            3. Una recomendación corta sobre cómo influye el filtro DAC seleccionado ($filter).
            
            Mantén un estilo audiófilo profesional, sin rodeos comerciales exagerados y estructurado en unos tres puntos claros.
        """.trimIndent()

        val request = GenerateContentRequestMoshi(
            contents = listOf(
                ContentMoshi(
                    parts = listOf(PartMoshi(text = prompt))
                )
            )
        )

        return try {
            val response = service.generateContent(key, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No se obtuvo respuesta para el análisis acústico."
        } catch (e: Exception) {
            "Error en el análisis de Gemini: ${e.localizedMessage ?: e.message}"
        }
    }
}
