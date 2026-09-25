package eu.kanade.tachiyomi.animeextension.all.hentaicity

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class HentaiCity : AnimeHttpSource() {

    override val name = "Hentai City"
    override val baseUrl = "https://www.hentaicity.com"
    override val lang = "all"
    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
        .add("Referer", "$baseUrl/")

    // ============================== Popular ===============================
    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/videos?page=$page", headers)

    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        val animeList = document.select("div.video-item, article, .thumb-block").mapNotNull { element ->
            val titleEl = element.selectFirst("a.title, h4 a, .video-title a") ?: return@mapNotNull null
            val titleText = titleEl.text().trim()
            if (titleText.isEmpty()) return@mapNotNull null

            val href = titleEl.attr("href") ?: return@mapNotNull null

            SAnime.create().apply {
                title = titleText
                setUrlWithoutDomain(href)
                thumbnail_url = element.selectFirst("img")?.let { 
                    it.attr("data-src").ifEmpty { null } ?: it.attr("src")
                }
            }
        }
        val hasNextPage = document.selectFirst("a.next, .pagination-next, li.active + li a") != null
        return AnimesPage(animeList, hasNextPage)
    }

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/videos?sort=latest&page=$page", headers)
    override fun latestUpdatesParse(response: Response): AnimesPage = popularAnimeParse(response)

    // =============================== Search ===============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        GET("$baseUrl/search?q=$query&page=$page", headers)
    override fun searchAnimeParse(response: Response): AnimesPage = popularAnimeParse(response)

    // =========================== Anime Details ============================
    override fun animeDetailsParse(response: Response): SAnime {
        val document = response.asJsoup()
        return SAnime.create().apply {
            title = document.selectFirst("h1, .video-title")?.text()?.trim().orEmpty()
            description = document.selectFirst(".description, .video-description")?.text()?.trim().orEmpty()
        }
    }

    // ============================== Seasons ===============================
    override fun seasonListParse(response: Response): List<SAnime> = emptyList()

    // ============================== Episodes ==============================
    override fun episodeListParse(response: Response): List<SEpisode> {
        return listOf(
            SEpisode.create().apply {
                name = "Full Video"
                setUrlWithoutDomain(response.request.url.toString())
                episode_number = 1f
            }
        )
    }

    // ============================== Hosters ===============================
    override fun hosterListParse(response: Response): List<Hoster> {
        return listOf(Hoster("Default", response.request.url.toString()))
    }

    // ============================ Video Links =============================
    override fun videoListParse(response: Response, hoster: Hoster): List<Video> {
        val document = response.asJsoup()
        val videos = mutableListOf<Video>()
        document.select("video source, iframe").forEach { source ->
            val videoUrl = source.attr("src")
            if (videoUrl.isNotEmpty() && videoUrl.startsWith("http")) {
                videos.add(Video(videoUrl, "Default", videoUrl, headers = headers))
            }
        }
        return videos
    }

    private fun Response.asJsoup(): Document = Jsoup.parse(body.string(), request.url.toString())
}
