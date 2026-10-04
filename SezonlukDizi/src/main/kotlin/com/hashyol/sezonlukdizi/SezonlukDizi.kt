package com.hashyol.sezonlukdizi

import android.util.Log
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
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.TvSeries)

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
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Referer" to "${mainUrl}/"
            )
        ).document

        val home = document.select("a.column, div.afis a, div.ui.card a, a[href*='/diziler/']").mapNotNull {
            it.toSearchResult()
        }.distinctBy { it.url }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.description")?.text()?.trim()
            ?: this.selectFirst("span.title")?.text()?.trim()
            ?: this.selectFirst("div.header")?.text()?.trim()
            ?: this.attr("title").removeSuffix(" izle").trim().ifEmpty { null }
            ?: return null

        val href = fixUrlNull(this.attr("href")) ?: return null
        // Skip links that aren't series pages
        if (!href.contains("/diziler/") && !href.contains(".html")) return null

        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("data-src")?.ifEmpty { null }
                ?: this.selectFirst("img")?.attr("src")
        )

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get(
            "${mainUrl}/diziler.asp?adi=${query}",
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Referer" to "${mainUrl}/"
            )
        ).document

        return document.select("a.column, div.afis a, div.ui.card a, a[href*='/diziler/']").mapNotNull {
            it.toSearchResult()
        }.distinctBy { it.url }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(
            url,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Referer" to "${mainUrl}/"
            )
        ).document

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

        val endpoint = url.split("/").last()

        val actorsReq = try {
            app.get(
                "${mainUrl}/oyuncular/${endpoint}",
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "Referer" to url
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
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "Referer" to url
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

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
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
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Referer" to "${mainUrl}/"
            )
        ).document

        val aspData = getAspData()
        val bid = document.selectFirst("div#dilsec")?.attr("data-id") ?: return false
        Log.d("SezonlukDizi", "bid -> $bid")

        // 1 = Altyazı, 0 = Dublaj
        val languages = listOf("1" to "AltYazı", "0" to "Dublaj")

        for ((dilCode, dilName) in languages) {
            try {
                val alternatifResponse = app.post(
                    "${mainUrl}/ajax/dataAlternatif${aspData.alternatif}.asp",
                    headers = mapOf(
                        "X-Requested-With" to "XMLHttpRequest",
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
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

                    try {
                        val veriResponse = app.post(
                            "${mainUrl}/ajax/dataEmbed${aspData.embed}.asp",
                            headers = mapOf(
                                "X-Requested-With" to "XMLHttpRequest",
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to pageUrl
                            ),
                            data = mapOf("id" to "$veriId")
                        ).document

                        val iframeSrc = veriResponse.selectFirst("iframe")?.attr("src") ?: return@forEach
                        if (iframeSrc.contains("reCAPTCHA", ignoreCase = true)) return@forEach

                        val iframe = fixUrl(iframeSrc)
                        Log.d("SezonlukDizi", "$dilName | $veriBaslik -> $iframe")

                        loadExtractor(iframe, "${mainUrl}/", subtitleCallback) { link ->
                            callback.invoke(
                                ExtractorLink(
                                    source = "$dilName - $veriBaslik",
                                    name = "$dilName - $veriBaslik",
                                    url = link.url,
                                    referer = link.referer,
                                    quality = link.quality,
                                    headers = link.headers,
                                    extractorData = link.extractorData,
                                    type = link.type
                                )
                            )
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
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
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
