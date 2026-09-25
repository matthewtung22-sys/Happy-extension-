package eu.kanade.tachiyomi.animeextension.zh.hanime1

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class HentaiCityCustom : AnimeHttpSource() {

    override val name = "HentaiCityCustom"
    override val baseUrl = "https://hanime1.me"
    override val lang = "zh"
    override val supportsLatest = true

    override val client: OkHttpClient = network.client

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
        .add("Referer", "$baseUrl/")

    // ============================== Popular ===============================
    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/search?page=$page", headers)

    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        val animeList = document.select("div.card, div.home-rows-videos-div").mapNotNull { element ->
            val titleEl = element.selectFirst("div.card-title, div.home-rows-videos-title") ?: return@mapNotNull null
            val titleText = titleEl.text().trim()
            if (titleText.isEmpty()) return@mapNotNull null

            val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null

            SAnime.create().apply {
                title = titleText
                setUrlWithoutDomain(href)
                val imgEl = element.selectFirst("img")
                thumbnail_url = imgEl?.attr("data-src")?.ifEmpty { null }
                    ?: imgEl?.attr("data-lazy-src")?.ifEmpty { null }
                    ?: imgEl?.attr("src")
            }
        }
        val hasNextPage = document.selectFirst("a.page-link[rel=next], li.next:not(.disabled) a") != null
        return AnimesPage(animeList, hasNextPage)
    }

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/search?s=created_at&page=$page", headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = popularAnimeParse(response)

    // =============================== Search ===============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        GET("$baseUrl/search?query=$query&page=$page", headers)

    override fun searchAnimeParse(response: Response): AnimesPage = popularAnimeParse(response)

    // =========================== Anime Details ============================
    override fun animeDetailsParse(response: Response): SAnime {
        val document = response.asJsoup()
        return SAnime.create().apply {
            title = document.selectFirst("h3.video-details-title, h1")?.text()?.trim().orEmpty()
            description = document.selectFirst("div.video-details-description")?.text()?.trim().orEmpty()
            genre = document.select("div.single-video-tag a").joinToString { it.text().trim() }
            thumbnail_url = document.selectFirst("video#player")?.attr("poster")
                ?: document.selectFirst("div.player-container img")?.attr("src")
        }
    }

    // ============================== Episodes ==============================
    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        val episodes = mutableListOf<SEpisode>()
        val epElements = document.select("div.playlist-scroll div.card, div.related-watch-wrap")

        if (epElements.isNotEmpty()) {
            epElements.forEachIndexed { index, element ->
                val titleText = element.selectFirst("div.card-title, div.related-watch-title")?.text()?.trim()
                    ?.ifEmpty { "Episode ${index + 1}" } ?: "Episode ${index + 1}"
                val href = element.selectFirst("a")?.attr("href") ?: return@forEachIndexed

                episodes.add(
                    SEpisode.create().apply {
                        name = titleText
                        setUrlWithoutDomain(href)
                        episode_number = (index + 1).toFloat()
                    }
                )
            }
        } else {
            episodes.add(
                SEpisode.create().apply {
                    name = "Full Episode"
                    setUrlWithoutDomain(response.request.url.toString())
                    episode_number = 1f
                }
            )
        }
        return episodes.reversed()
    }

    // ============================ Video Links =============================
    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()
        val videos = mutableListOf<Video>()

        document.select("video source, video#player source").forEach { source ->
            val videoUrl = source.attr("src")
            val quality = source.attr("size").let { if (it.isNotEmpty()) "${it}p" else "Default" }
            if (videoUrl.isNotEmpty()) {
                videos.add(Video(videoUrl, quality, videoUrl, headers = headers))
            }
        }

        if (videos.isEmpty()) {
            val directSrc = document.selectFirst("video#player")?.attr("src")
            if (!directSrc.isNullOrEmpty()) {
                videos.add(Video(directSrc, "Default", directSrc, headers = headers))
            }
        }

        return videos
    }

    private fun Response.asJsoup(): Document = Jsoup.parse(body.string(), request.url.toString())
}
