package eu.kanade.tachiyomi.animeextension.zh.hanime1

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class HentaiCityCustom : ParsedAnimeHttpSource() {

    override val name = "HentaiCityCustom"
    override val baseUrl = "https://hanime1.me"
    override val lang = "zh"
    override val supportsLatest = true

    // Popular Anime
    override fun popularAnimeRequest(page: Int): Request = Request.Builder().url("$baseUrl/search?page=$page").build()
    override fun popularAnimeSelector(): String = "div.card"
    override fun popularAnimeFromElement(element: Element): SAnime = SAnime.create().apply {
        title = element.select("div.card-title").text()
        url = element.select("a").attr("href").removePrefix(baseUrl)
        thumbnail_url = element.select("img").attr("src")
    }
    override fun popularAnimeNextPageSelector(): String? = "a.page-link[rel=next]"

    // Latest Updates
    override fun latestUpdatesRequest(page: Int): Request = Request.Builder().url("$baseUrl/latest?page=$page").build()
    override fun latestUpdatesSelector(): String = popularAnimeSelector()
    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)
    override fun latestUpdatesNextPageSelector(): String? = popularAnimeNextPageSelector()

    // Search Anime
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        Request.Builder().url("$baseUrl/search?query=$query&page=$page").build()
    override fun searchAnimeSelector(): String = popularAnimeSelector()
    override fun searchAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)
    override fun searchAnimeNextPageSelector(): String? = popularAnimeNextPageSelector()

    // Details Parsing
    override fun animeDetailsParse(document: Document): SAnime = SAnime.create().apply {
        title = document.select("h1").text()
        description = document.select("div.description").text()
    }

    // Episode Overrides
    override fun episodeListSelector(): String = "div.episode-item"
    override fun episodeFromElement(element: Element): SEpisode = SEpisode.create().apply {
        name = element.text()
        url = element.select("a").attr("href").removePrefix(baseUrl)
    }
    override fun episodeNextPageSelector(): String? = null

    // Season Overrides
    override fun seasonListSelector(): String = "div.episode-item"
    override fun seasonFromElement(element: Element): SAnime = SAnime.create().apply {
        title = element.text()
        url = element.select("a").attr("href").removePrefix(baseUrl)
    }
    override fun seasonNextPageSelector(): String? = null

    // Hoster & Video Overrides
    override fun hosterListParse(response: Response): List<Hoster> = emptyList()
    override fun videoUrlParse(response: Response): String = ""
}
