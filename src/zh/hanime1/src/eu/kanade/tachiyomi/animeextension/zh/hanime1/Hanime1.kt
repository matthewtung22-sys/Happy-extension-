package eu.kanade.tachiyomi.animeextension.all.hentaicitycustom

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import keiyoushi.utils.absUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response

class HentaiCityCustom : AnimeHttpSource() {

    override val name = "Hentai City"

    override val baseUrl = "https://hentai.city"

    override val lang = "all"

    override val supportsLatest = true

    // ============================== Popular ==============================

    override fun popularAnimeRequest(page: Int): Request {
        return GET("$baseUrl/popular?page=$page", headers)
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        val animeList = document.select("div.anime-card, div.video-item, article.anime, div.anime-item").mapNotNull { element ->
            val aTag = element.selectFirst("a") ?: return@mapNotNull null
            SAnime.create().apply {
                title = element.selectFirst("h2, h3, .title")?.text() ?: aTag.attr("title")
                setUrlWithoutDomain(aTag.absUrl("href"))
                thumbnail_url = element.selectFirst("img")?.let { img ->
                    val src = img.attr("abs:data-src")
                    if (src.isNotEmpty()) src else img.attr("abs:src")
                }
            }
        }

        val hasNextPage = document.selectFirst("a[rel=next], .pagination .next, a.next-page") != null
        return AnimesPage(animeList, hasNextPage)
    }

    // ============================== Latest ==============================

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/latest?page=$page", headers)
    }

    override fun latestUpdatesParse(response: Response): AnimesPage = popularAnimeParse(response)

    // ============================== Search ==============================

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val url = "$baseUrl/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("page", page.toString())

        return GET(url.build().toString(), headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = popularAnimeParse(response)

    // ============================== Details ==============================

    override fun animeDetailsParse(response: Response): SAnime {
        val document = response.asJsoup()
        return SAnime.create().apply {
            title = document.selectFirst("h1.entry-title, h1.title, h1")?.text() ?: ""
            genre = document.select("div.genres a, div.tags a, .genre a").joinToString { it.text() }
            description = document.selectFirst("div.entry-content, div.description, .summary")?.text()
            status = SAnime.COMPLETED
        }
    }

    // ============================== Episodes ==============================

    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        val episodeElements = document.select("ul.episodes-list li, div.episode-item, .eplister li, ul.episodelist li")

        if (episodeElements.isNotEmpty()) {
            return episodeElements.mapNotNull { element ->
                val aTag = element.selectFirst("a") ?: return@mapNotNull null
                SEpisode.create().apply {
                    name = element.selectFirst(".ep-title, .title")?.text() ?: aTag.text()
                    setUrlWithoutDomain(aTag.absUrl("href"))
                }
            }.reversed()
        }

        // Standalone single video page fallback
        val singleEpisode = SEpisode.create().apply {
            name = document.selectFirst("h1.entry-title, h1.title, h1")?.text() ?: "Episode 1"
            setUrlWithoutDomain(response.request.url.toString())
        }
        return listOf(singleEpisode)
    }

    // ============================== Video Extraction ==============================

    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()
        val videoList = mutableListOf<Video>()

        // Direct HTML5 video sources
        val videoSources = document.select("video source, video[src]")
        for (source in videoSources) {
            val src = source.attr("abs:src").ifEmpty { source.attr("abs:data-src") }
            if (src.isNotEmpty()) {
                val quality = source.attr("res").ifEmpty { source.attr("label") }.ifEmpty { "Default" }
                videoList.add(Video(src, quality, src))
            }
        }

        // Fallback for embedded iframe players
        if (videoList.isEmpty()) {
            document.select("iframe[src]").forEach { iframe ->
                val iframeUrl = iframe.attr("abs:src")
                if (iframeUrl.isNotEmpty() && !iframeUrl.contains("facebook") && !iframeUrl.contains("twitter")) {
                    videoList.add(Video(iframeUrl, "Embed Stream", iframeUrl))
                }
            }
        }

        return videoList
    }
}
