package eu.kanade.tachiyomi.animeextension.zh.hanime1

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.network.GET
import keiyoushi.utils.AnimeHttpLegacySource
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup

class HentaiCityCustom : AnimeHttpLegacySource() {

    override val name = "HentaiCityCustom"
    override val baseUrl = "https://hanime1.me"
    override val lang = "zh"
    override val supportsLatest = true

    // ============================== Popular ===============================
    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/search?page=$page", headers)

    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = Jsoup.parse(response.body.string())
        val animeList = document.select("div.card, div.home-rows-videos-div").map { element ->
            SAnime.create().apply {
                title = element.select("div.card-title, div.home-rows-videos-title").text().trim()
                val href = element.select("a").attr("href")
                url = if (href.startsWith("http")) href else "$baseUrl$href"
                thumbnail_url = element.select("img").attr("src")
            }
        }
        val hasNextPage = document.select("a.page-link[rel=next]").isNotEmpty()
        return AnimesPage(animeList, hasNextPage)
    }

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/search?genre=latest&page=$page", headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = popularAnimeParse(response)

    // =============================== Search ===============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        GET("$baseUrl/search?query=$query&page=$page", headers)

    override fun searchAnimeParse(response: Response): AnimesPage = popularAnimeParse(response)

    // =========================== Anime Details ============================
    override fun animeDetailsParse(response: Response): SAnime {
        val document = Jsoup.parse(response.body.string())
        return SAnime.create().apply {
            title = document.select("h3.video-details-title, h1").text().trim()
            description = document.select("div.video-details-description").text().trim()
            genre = document.select("div.single-video-tag a").joinToString { it.text() }
        }
    }

    // ============================== Episodes ==============================
    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = Jsoup.parse(response.body.string())
        val episodes = mutableListOf<SEpisode>()
        val epElements = document.select("div.playlist-scroll div.card, div.related-watch-wrap")

        if (epElements.isNotEmpty()) {
            epElements.forEachIndexed { index, element ->
                episodes.add(
                    SEpisode.create().apply {
                        name = element.select("div.card-title, div.related-watch-title").text().trim().ifEmpty { "Episode ${index + 1}" }
                        val href = element.select("a").attr("href")
                        url = if (href.startsWith("http")) href else "$baseUrl$href"
                        episode_number = (index + 1).toFloat()
                    }
                )
            }
        } else {
            episodes.add(
                SEpisode.create().apply {
                    name = "Full Episode"
                    url = response.request.url.toString()
                    episode_number = 1f
                }
            )
        }
        return episodes
    }

    // ============================ Video Links =============================
    override fun videoListParse(response: Response): List<Video> {
        val document = Jsoup.parse(response.body.string())
        val videos = mutableListOf<Video>()

        document.select("video source").forEach { source ->
            val videoUrl = source.attr("src")
            val quality = source.attr("size").let { if (it.isNotEmpty()) "${it}p" else "Default" }
            if (videoUrl.isNotEmpty()) {
                videos.add(Video(videoUrl, quality, videoUrl, headers))
            }
        }
        return videos
    }
}
