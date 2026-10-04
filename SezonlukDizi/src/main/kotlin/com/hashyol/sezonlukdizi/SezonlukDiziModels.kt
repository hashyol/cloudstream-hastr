package com.hashyol.sezonlukdizi

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Kaynak(
    @JsonProperty("status") val status: String? = null,
    @JsonProperty("data") val data: List<Veri>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Veri(
    @JsonProperty("baslik") val baslik: String? = null,
    @JsonProperty("id") val id: Int? = null,
    @JsonProperty("kalite") val kalite: Int? = null,
)

data class AspData(
    val alternatif: String,
    val embed: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchRoot(
    @JsonProperty("status") val status: String? = null,
    @JsonProperty("results") val results: SearchResults? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchResults(
    @JsonProperty("diziler") val diziler: SearchCategory? = null,
    @JsonProperty("filmler") val filmler: SearchCategory? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchCategory(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("results") val results: List<SearchItem>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchItem(
    @JsonProperty("did") val did: Int? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("url") val url: String? = null,
    @JsonProperty("image") val image: String? = null,
    @JsonProperty("imdb") val imdb: Any? = null
)
