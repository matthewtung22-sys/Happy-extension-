package eu.kanade.tachiyomi.animeextension.zh.hanime1

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.Hoster
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

    // Popular
    override fun popularAnimeRequest(page: Int): Request = Request.Builder().url("$baseUrl/search?page=$page").build()
    override fun popularAnimeSelector(): String = "div.card"
    override fun popularAnimeFromElement(element: Element): SAnime = SAnime.create().apply {
        title = element.select("div.card-title").text()
        url = element.select("a").attr("href").removePrefix(baseUrl)
        thumbnail_url = element.select("img").attr("src")
    }
    override fun popularAnimeNextPageSelector(): String = "a.page-link[rel=next]"

    // Latest
    override fun latestUpdatesRequest(page: Int): Request = Request.Builder().url("$baseUrl/latest?page=$page").build()
    override fun latestUpdatesSelector(): String = popularAnimeSelector()
    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)
    override fun latestUpdatesNextPageSelector(): String = popularAnimeNextPageSelector()

    // Search
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        Request.Builder().url("$baseUrl/search?query=$query&page=$page").build()
    override fun searchAnimeSelector(): String = popularAnimeSelector()
    override fun searchAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)
    override fun searchAnimeNextPageSelector(): String = popularAnimeNextPageSelector()

    // Details
    override fun animeDetailsParse(document: Document): SAnime = SAnime.create().apply {
        title = document.select("h1").text()
        description = document.select("div.description").text()
    }

    // Season list overrides expected by your core source
    override fun seasonListSelector(): String = "div.episode-item"
    override fun seasonFromElement(element: Element): SAnime = SAnime.create().apply {
        title = element.text()
        url = element.select("a").attr("href").removePrefix(baseUrl)
    }

    // Hoster & Video overrides
    override fun hosterListParse(response: Response): List<Hoster> = emptyList()
    override fun videoUrlParse(response: Response): String = ""
}
