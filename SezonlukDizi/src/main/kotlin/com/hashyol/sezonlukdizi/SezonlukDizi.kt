package com.hashyol.sezonlukdizi

import android.util.Log
import kotlinx.coroutines.delay
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore

class SezonlukDizi : MainAPI() {
    override var mainUrl              = "https://sezonlukdizi.cc"
    override var name                 = "SezonlukDizi"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = true
    override val supportedTypes       = setOf(TvType.TvSeries)

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    override val mainPage = mainPageOf(
        "${mainUrl}/diziler.asp?siralama_tipi=id&s="          to "Son Eklenenler",
        "${mainUrl}/diziler.asp?siralama_tipi=id&tur=mini&s=" to "Mini Diziler",
        "${mainUrl}/diziler.asp?siralama_tipi=id&kat=2&s="    to "Yerli Diziler",
        "${mainUrl}/diziler.asp?siralama_tipi=id&kat=1&s="    to "Yabancı Diziler",
        "${mainUrl}/diziler.asp?siralama_tipi=id&kat=3&s="    to "Asya Dizileri",
        "${mainUrl}/diziler.asp?siralama_tipi=id&kat=4&s="    to "Animasyonlar",
        "${mainUrl}/diziler.asp?siralama_tipi=id&kat=5&s="    to "Animeler",
        "${mainUrl}/diziler.asp?siralama_tipi=id&kat=6&s="    to "Belgeseller",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(
            "${request.data}${page}",
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to "${mainUrl}/"
            )
        ).document

        val seriesElements = document.select("a.column[href*='/diziler/'], div.afis a[href*='/diziler/']")
        val home = if (seriesElements.isNotEmpty()) {
            seriesElements.mapNotNull { it.toSearchResult() }.distinctBy { it.url }
        } else {
            // Homepage card fallback: convert episode card link to series page link
            document.select("div.ui.card.golgever a.image, div.ui.card a[href*='bolum']").mapNotNull { card ->
                val title = card.selectFirst("span.title")?.text()?.trim()
                    ?: card.selectFirst("img")?.attr("alt")?.substringBefore(".Bölüm")?.substringBefore(".Sezon")?.trim()
                    ?: return@mapNotNull null
                val epHref = card.attr("href")
                val slug = epHref.trim('/').split('/').firstOrNull() ?: return@mapNotNull null
                val seriesHref = fixUrl("/diziler/$slug.html")
                val posterUrl = fixUrlNull(card.selectFirst("img")?.attr("src"))

                newTvSeriesSearchResponse(title, seriesHref, TvType.TvSeries) {
                    this.posterUrl = posterUrl
                }
            }.distinctBy { it.url }
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.description")?.text()?.trim()
            ?: this.selectFirst("span.title")?.text()?.trim()
            ?: this.selectFirst("div.header")?.text()?.trim()
            ?: this.attr("title").removeSuffix(" izle").trim().ifEmpty { null }
            ?: return null

        val rawHref = this.attr("href")
        val href = if (!rawHref.contains("/diziler/") && rawHref.contains("bolum")) {
            val slug = rawHref.trim('/').split('/').firstOrNull() ?: return null
            fixUrl("/diziler/$slug.html")
        } else {
            fixUrlNull(rawHref) ?: return null
        }

        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("data-src")?.ifEmpty { null }
                ?: this.selectFirst("img")?.attr("src")
        )

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchRoot = try {
            app.post(
                "${mainUrl}/ajax/arama.asp",
                headers = mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "User-Agent" to USER_AGENT,
                    "Referer" to "${mainUrl}/"
                ),
                data = mapOf("q" to query)
            ).parsedSafe<SearchRoot>()
        } catch (e: Exception) {
            Log.e("SezonlukDizi", "Search error: ${e.message}")
            null
        }

        val results = searchRoot?.results?.diziler?.results ?: emptyList()

        return results.mapNotNull { item ->
            val title = item.title ?: return@mapNotNull null
            val url = fixUrlNull(item.url) ?: return@mapNotNull null
            val posterUrl = fixUrlNull(item.image)

            newTvSeriesSearchResponse(title, url, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val actualUrl = fixUrl(url)
        val initialDoc = app.get(
            actualUrl,
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to "${mainUrl}/"
            )
        ).document

        // If an episode page was opened directly, resolve to the full series page
        val (document, seriesUrl) = if (!actualUrl.contains("/diziler/")) {
            val seriesHref = initialDoc.selectFirst("a[href*='/diziler/']")?.attr("href")
            if (seriesHref != null) {
                val fullSeriesUrl = fixUrl(seriesHref)
                try {
                    app.get(
                        fullSeriesUrl,
                        headers = mapOf(
                            "User-Agent" to USER_AGENT,
                            "Referer" to "${mainUrl}/"
                        )
                    ).document to fullSeriesUrl
                } catch (e: Exception) {
                    initialDoc to actualUrl
                }
            } else {
                initialDoc to actualUrl
            }
        } else {
            initialDoc to actualUrl
        }

        val title = document.selectFirst("div.header")?.text()?.trim()
            ?: document.selectFirst("h1")?.text()?.trim()
            ?: return null

        val poster = fixUrlNull(
            document.selectFirst("div.image img")?.attr("data-src")?.ifEmpty { null }
                ?: document.selectFirst("div.image img")?.attr("src")
                ?: document.selectFirst("img[data-src*='/dizi/']")?.attr("data-src")
        )

        val year = document.selectFirst("div.extra span")?.text()?.trim()?.split("-")?.first()?.toIntOrNull()
        val description = document.selectFirst("span#tartismayorum-konu")?.text()?.trim()
        val tags = document.select("div.labels a[href*='tur']").mapNotNull { it.text().trim() }
        val rating = document.selectFirst("div.dizipuani a div, .dizipuani")?.text()?.trim()?.replace(",", ".")
        val duration = document.selectXpath("//span[contains(text(), 'Dk.')]").text().trim().substringBefore(" Dk.").toIntOrNull()

        val endpoint = seriesUrl.split("/").last()

        val actorsReq = try {
            app.get(
                "${mainUrl}/oyuncular/${endpoint}",
                headers = mapOf(
                    "User-Agent" to USER_AGENT,
                    "Referer" to seriesUrl
                )
            ).document
        } catch (e: Exception) {
            null
        }

        val actors = actorsReq?.select("div.doubling div.ui, div.card")?.mapNotNull {
            val name = it.selectFirst("div.header")?.text()?.trim() ?: return@mapNotNull null
            val img = fixUrlNull(it.selectFirst("img")?.attr("src"))
            Actor(name, img)
        } ?: emptyList()

        val episodes = mutableListOf<Episode>()

        try {
            val episodesReq = app.get(
                "${mainUrl}/bolumler/${endpoint}",
                headers = mapOf(
                    "User-Agent" to USER_AGENT,
                    "Referer" to seriesUrl
                )
            ).document

            for (sezon in episodesReq.select("table.unstackable, table")) {
                for (bolum in sezon.select("tbody tr, tr")) {
                    val linkElem = bolum.select("td a[href*='bolum.html']").lastOrNull() ?: continue
                    val epName = linkElem.text().trim()
                    val epHref = fixUrlNull(linkElem.attr("href")) ?: continue
                    val epSeason = bolum.select("td:nth-of-type(2)").text().substringBefore(".Sezon").trim().toIntOrNull()
                    val epEpisode = bolum.select("td:nth-of-type(3)").text().substringBefore(".Bölüm").trim().toIntOrNull()

                    episodes.add(newEpisode(epHref) {
                        this.name = epName
                        this.season = epSeason
                        this.episode = epEpisode
                    })
                }
            }
        } catch (e: Exception) {
            Log.e("SezonlukDizi", "Error loading episodes: ${e.message}")
        }

        return newTvSeriesLoadResponse(title, seriesUrl, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
            this.tags = tags
            this.duration = duration
            if (!rating.isNullOrBlank()) {
                addScore(rating, 10)
            }
            if (actors.isNotEmpty()) {
                addActors(actors)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val pageUrl = fixUrl(data)
        Log.d("SezonlukDizi", "loadLinks data -> $pageUrl")

        val document = app.get(
            pageUrl,
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to "${mainUrl}/"
            )
        ).document

        val aspData = getAspData()
        val bid = document.selectFirst("div#dilsec")?.attr("data-id")
            ?: Regex("""data-id=["'](\d+)["']""").find(document.html())?.groupValues?.get(1)
            ?: return false
        Log.d("SezonlukDizi", "bid -> $bid")

        // 1 = Altyazı, 0 = Dublaj
        val languages = listOf("1" to "AltYazı", "0" to "Dublaj")

        for ((dilCode, dilName) in languages) {
            try {
                delay(150)
                val alternatifResponse = app.post(
                    "${mainUrl}/ajax/dataAlternatif${aspData.alternatif}.asp",
                    headers = mapOf(
                        "X-Requested-With" to "XMLHttpRequest",
                        "User-Agent" to USER_AGENT,
                        "Referer" to pageUrl
                    ),
                    data = mapOf(
                        "bid" to bid,
                        "dil" to dilCode
                    )
                ).parsedSafe<Kaynak>()

                alternatifResponse?.data?.forEach { veri ->
                    val veriId = veri.id ?: return@forEach
                    val veriBaslik = veri.baslik ?: "Alternatif"

                    // Skip internal captcha-protected player
                    if (veriBaslik.contains("Pixel", ignoreCase = true)) return@forEach

                    try {
                        delay(150)
                        val veriResponse = app.post(
                            "${mainUrl}/ajax/dataEmbed${aspData.embed}.asp",
                            headers = mapOf(
                                "X-Requested-With" to "XMLHttpRequest",
                                "User-Agent" to USER_AGENT,
                                "Referer" to pageUrl
                            ),
                            data = mapOf("id" to "$veriId")
                        ).document

                        val iframeSrc = veriResponse.selectFirst("iframe")?.attr("src") ?: return@forEach
                        if (iframeSrc.contains("reCAPTCHA", ignoreCase = true)) return@forEach

                        var iframe = fixUrl(iframeSrc)
                        if (iframe.startsWith("//")) {
                            iframe = "https:$iframe"
                        }
                        Log.d("SezonlukDizi", "$dilName | $veriBaslik -> $iframe")

                        if (iframe.contains("odnoklassniki.ru")) {
                            val okUrl = iframe.replace("odnoklassniki.ru", "ok.ru")
                            loadExtractor(okUrl, subtitleCallback, callback)
                        } else if (iframe.contains("bysejikuar.com") || iframe.contains("byse")) {
                            loadExtractor(iframe, subtitleCallback, callback)
                            val filemoonUrl = iframe.replace(Regex("""https://[^/]+/(e|d)/"""), "https://filemoon.sx/e/")
                            if (filemoonUrl != iframe) {
                                loadExtractor(filemoonUrl, subtitleCallback, callback)
                            }
                        } else {
                            loadExtractor(iframe, subtitleCallback, callback)
                        }
                    } catch (e: Exception) {
                        Log.e("SezonlukDizi", "Error parsing embed $veriId: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SezonlukDizi", "Error loading language $dilName: ${e.message}")
            }
        }

        return true
    }

    private suspend fun getAspData(): AspData {
        return try {
            val js = app.get(
                "${this.mainUrl}/js/site.min.js",
                headers = mapOf(
                    "User-Agent" to USER_AGENT,
                    "Referer" to "${mainUrl}/"
                )
            ).text
            val dataAlternatifAsp = Regex("""dataAlternatif(.*?).asp""").find(js)?.groupValues?.get(1) ?: "22"
            val dataEmbedAsp = Regex("""dataEmbed(.*?).asp""").find(js)?.groupValues?.get(1) ?: "22"
            AspData(dataAlternatifAsp, dataEmbedAsp)
        } catch (e: Exception) {
            AspData("22", "22")
        }
    }
}
