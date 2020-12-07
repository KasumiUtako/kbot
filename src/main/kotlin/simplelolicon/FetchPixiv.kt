package simplelolicon


import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

private const val LOLICON_API = "https://api.lolicon.app/setu/"
private val API_KEY = File("APIKEY").readText()

@Serializable
data class Setu(
    val pid: Int = 0,
    val p: Int = 0,
    val uid: Int = 0,
    val title: String = "",
    val author: String = "",
    val url: String = "",
    val r18: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val tags: List<String> = emptyList()
)

@Serializable
data class LoliconData(
    val code: Int = 0,
    val msg: String = "",
    val quota: Int = 0,
    val quota_min_ttl: Int = 0,
    val count: Int = 0,
    val data: List<Setu> = emptyList()
)

suspend fun fetchRemote(keyword: String = "") = withContext(Dispatchers.IO) {
    val content = HttpClient().use { client ->
        client.get<ByteArray>(LOLICON_API) {
            parameter(
                key = "apikey",
                value = API_KEY
            )
            parameter(
                key = "r18",
                value = 2
            )
            parameter(
                key = "keyword",
                value = keyword
            )
        }.decodeToString()
    }

    val lolicon = Json.decodeFromString<LoliconData>(content)

    if (lolicon.code != 0) {
        error(lolicon.msg)
    } else {
        lolicon.data
    }
}