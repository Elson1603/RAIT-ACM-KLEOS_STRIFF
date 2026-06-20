package com.safepulse.data.repository

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import com.safepulse.domain.riskmap.CrimeHotspot
import com.safepulse.domain.riskmap.CrimeRiskZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class LiveRiskFeedResult(
    val crimeZones: List<CrimeRiskZone>,
    val fetchedAtMillis: Long = System.currentTimeMillis()
)

class LiveRiskFeedClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(java.time.Duration.ofSeconds(12))
        .build()
) {
    private val gdeltService: GdeltDocService = Retrofit.Builder()
        .baseUrl("https://api.gdeltproject.org/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GdeltDocService::class.java)

    suspend fun fetchLiveRiskFeeds(): LiveRiskFeedResult {
        return LiveRiskFeedResult(crimeZones = fetchCrimeZones())
    }

    private suspend fun fetchCrimeZones(): List<CrimeRiskZone> = withContext(Dispatchers.IO) {
        runCatching {
            gdeltService.searchArticles(
                query = LIVE_CRIME_QUERY,
                maxRecords = 150
            ).articles
                .asSequence()
                .mapNotNull { it.toLiveIncident() }
                .groupBy { it.geoHint.name }
                .map { (_, incidents) -> incidents.toCrimeRiskZone() }
                .sortedByDescending { it.crimeRiskScore }
                .take(80)
                .toList()
        }.getOrElse { emptyList() }
    }

    private fun GdeltArticle.toLiveIncident(): LiveCrimeIncident? {
        val searchable = listOf(title, sourceCountry, domain, url)
            .joinToString(" ")
            .lowercase(Locale.US)
        val geoHint = INDIA_CITY_HINTS.firstOrNull { hint ->
            hint.aliases.any { alias -> searchable.contains(alias.lowercase(Locale.US)) }
        } ?: return null

        val severity = severityFor(searchable)
        val recencyBoost = recencyBoostFor(seenDate)
        val riskScore = (severity + recencyBoost).coerceIn(0.35f, 0.95f)
        return LiveCrimeIncident(
            title = title.ifBlank { "Recent safety incident" },
            geoHint = geoHint,
            riskScore = riskScore,
            crimeType = crimeTypeFor(searchable)
        )
    }

    private fun List<LiveCrimeIncident>.toCrimeRiskZone(): CrimeRiskZone {
        val hint = first().geoHint
        val risk = (maxOf { it.riskScore } + (size - 1) * 0.04f).coerceIn(0.35f, 0.98f)
        val violentCount = count { it.riskScore >= 0.7f }
        return CrimeRiskZone(
            city = hint.name,
            state = "Live News / Public Reports",
            location = hint.location,
            totalCrimes = size,
            violentCrimes = violentCount,
            crimeRiskScore = risk,
            violentCrimeRatio = if (isNotEmpty()) violentCount.toFloat() / size else 0f,
            radiusMeters = (2_000f + size * 350f).coerceIn(2_000f, 8_000f),
            dominantCrimes = map { it.crimeType }.distinct().take(4),
            hotspots = take(8).mapIndexed { index, incident ->
                CrimeHotspot(
                    location = hint.location.jitter(index),
                    risk = incident.riskScore,
                    label = incident.title.take(96)
                )
            }
        )
    }

    private fun severityFor(text: String): Float {
        return when {
            "rape" in text || "sexual assault" in text || "murder" in text || "kidnap" in text -> 0.82f
            "molestation" in text || "assault" in text || "stabbing" in text || "robbery" in text -> 0.72f
            "harassment" in text || "stalking" in text || "snatching" in text -> 0.62f
            "theft" in text || "burglary" in text || "crime" in text -> 0.52f
            else -> 0.42f
        }
    }

    private fun crimeTypeFor(text: String): String {
        return when {
            "rape" in text || "sexual assault" in text -> "Sexual assault"
            "murder" in text -> "Murder"
            "kidnap" in text -> "Kidnapping"
            "molestation" in text -> "Molestation"
            "assault" in text || "stabbing" in text -> "Assault"
            "robbery" in text -> "Robbery"
            "harassment" in text || "stalking" in text -> "Harassment"
            "snatching" in text -> "Snatching"
            "theft" in text || "burglary" in text -> "Theft/Burglary"
            else -> "Public safety report"
        }
    }

    private fun recencyBoostFor(seenDate: String): Float {
        val parsed = runCatching {
            val formatter = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            formatter.parse(seenDate)?.time
        }.getOrNull() ?: return 0f
        val ageHours = ((System.currentTimeMillis() - parsed) / 3_600_000L).coerceAtLeast(0L)
        return when {
            ageHours <= 24 -> 0.12f
            ageHours <= 72 -> 0.08f
            ageHours <= 168 -> 0.04f
            else -> 0f
        }
    }

    private fun LatLng.jitter(index: Int): LatLng {
        if (index == 0) return this
        val direction = if (index % 2 == 0) 1 else -1
        val amount = 0.003 * ((index % 5) + 1)
        return LatLng(latitude + direction * amount, longitude - direction * amount / 2.0)
    }

    private interface GdeltDocService {
        @GET("api/v2/doc/doc")
        suspend fun searchArticles(
            @Query("query") query: String,
            @Query("mode") mode: String = "ArtList",
            @Query("format") format: String = "json",
            @Query("maxrecords") maxRecords: Int,
            @Query("sort") sort: String = "HybridRel"
        ): GdeltDocResponse
    }

    private data class GdeltDocResponse(
        @SerializedName("articles") val articles: List<GdeltArticle> = emptyList()
    )

    private data class GdeltArticle(
        @SerializedName("title") val title: String = "",
        @SerializedName("url") val url: String = "",
        @SerializedName("domain") val domain: String = "",
        @SerializedName("sourceCountry") val sourceCountry: String = "",
        @SerializedName("seendate") val seenDate: String = ""
    )

    private data class LiveCrimeIncident(
        val title: String,
        val geoHint: GeoHint,
        val riskScore: Float,
        val crimeType: String
    )

    private data class GeoHint(
        val name: String,
        val location: LatLng,
        val aliases: List<String> = listOf(name)
    )

    companion object {
        private const val LIVE_CRIME_QUERY =
            "(crime OR assault OR robbery OR theft OR murder OR rape OR \"sexual assault\" OR harassment OR stalking OR kidnapping OR molestation OR \"chain snatching\") sourceCountry:IN sourcelang:english"

        private val INDIA_CITY_HINTS = listOf(
            GeoHint("Navi Mumbai", LatLng(19.0330, 73.0297), listOf("navi mumbai", "nerul", "vashi", "turbhe", "belapur", "kharghar", "panvel")),
            GeoHint("Mumbai", LatLng(19.0760, 72.8777), listOf("mumbai", "andheri", "bandra", "dadar", "kurla", "borivali", "thane")),
            GeoHint("Thane", LatLng(19.2183, 72.9781), listOf("thane", "kalyan", "dombivli", "bhiwandi")),
            GeoHint("Pune", LatLng(18.5204, 73.8567), listOf("pune", "pimpri", "chinchwad")),
            GeoHint("Delhi", LatLng(28.6139, 77.2090), listOf("delhi", "new delhi", "dwarka", "rohini")),
            GeoHint("Noida", LatLng(28.5355, 77.3910), listOf("noida", "greater noida")),
            GeoHint("Gurugram", LatLng(28.4595, 77.0266), listOf("gurugram", "gurgaon")),
            GeoHint("Bengaluru", LatLng(12.9716, 77.5946), listOf("bengaluru", "bangalore")),
            GeoHint("Hyderabad", LatLng(17.3850, 78.4867), listOf("hyderabad", "secunderabad")),
            GeoHint("Chennai", LatLng(13.0827, 80.2707), listOf("chennai")),
            GeoHint("Kolkata", LatLng(22.5726, 88.3639), listOf("kolkata", "howrah")),
            GeoHint("Ahmedabad", LatLng(23.0225, 72.5714), listOf("ahmedabad")),
            GeoHint("Jaipur", LatLng(26.9124, 75.7873), listOf("jaipur")),
            GeoHint("Lucknow", LatLng(26.8467, 80.9462), listOf("lucknow")),
            GeoHint("Nagpur", LatLng(21.1458, 79.0882), listOf("nagpur")),
            GeoHint("Indore", LatLng(22.7196, 75.8577), listOf("indore")),
            GeoHint("Bhopal", LatLng(23.2599, 77.4126), listOf("bhopal")),
            GeoHint("Patna", LatLng(25.5941, 85.1376), listOf("patna")),
            GeoHint("Surat", LatLng(21.1702, 72.8311), listOf("surat")),
            GeoHint("Kochi", LatLng(9.9312, 76.2673), listOf("kochi", "ernakulam"))
        )
    }
}
