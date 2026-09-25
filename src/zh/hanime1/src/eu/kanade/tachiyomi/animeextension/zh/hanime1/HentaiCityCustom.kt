package eu.kanade.tachiyomi.animeextension.zh.hanime1

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.network.GET
import keiyoushi.utils.AnimeHttpLegacySource
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup

class HentaiCityCustom : AnimeHttpLegacySource() {

    override val name = "Hentai City"
    override val baseUrl = "https://www.hentaicity.com"
    override val lang = "all"
    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
        .add("Referer", "$baseUrl/")

    override val client: OkHttpClient = network.cloudflareClient

    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/videos?page=$page", headers)

    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = Jsoup.parse(response.body.string())
        val animeList = document.select("div.video-item, article, .well div.item").map { element ->
            SAnime.create().apply {
                title = element.select("a.title, h4 a, .video-title").text().trim()
                val href = element.select("a").attr("href")
                url = if (href.startsWith("http")) href else "$baseUrl$href"
                thumbnail_url = element.select("img").attr("src")
            }
        }
        val hasNextPage = document.select("a.next, .pagination-next, li.active + li a").isNotEmpty()
        return AnimesPage(animeList, hasNextPage)
    }

    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/videos?sort=latest&page=$page", headers)
    override fun latestUpdatesParse(response: Response): AnimesPage = popularAnimeParse(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        GET("$baseUrl/search?q=$query&page=$page", headers)
    override fun searchAnimeParse(response: Response): AnimesPage = popularAnimeParse(response)

    override fun animeDetailsParse(response: Response): SAnime {
        val document = Jsoup.parse(response.body.string())
        return SAnime.create().apply {
            title = document.select("h1, .video-title h2").text().trim()
            description = document.select(".description, .video-description").text().trim()
        }
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = Jsoup.parse(response.body.string())
        val episodes = mutableListOf<SEpisode>()
        episodes.add(
            SEpisode.create().apply {
                name = "Full Video"
                url = response.request.url.toString()
                episode_number = 1f
            }
        )
        return episodes
    }

    override fun videoListParse(response: Response): List<Video> {
        val document = Jsoup.parse(response.body.string())
        val videos = mutableListOf<Video>()
        document.select("video source, iframe").forEach { source ->
            val videoUrl = source.attr("src")
            if (videoUrl.isNotEmpty() && videoUrl.startsWith("http")) {
                videos.add(Video(videoUrl, "Default", videoUrl, headers))
            }
        }
        return videos
    }
}
