package com.hashyol.sezonlukdizi

import com.fasterxml.jackson.annotation.JsonProperty

data class Kaynak(
    @JsonProperty("status") val status: String? = null,
    @JsonProperty("data") val data: List<Veri>? = null,
)

data class Veri(
    @JsonProperty("baslik") val baslik: String? = null,
    @JsonProperty("id") val id: Int? = null,
    @JsonProperty("kalite") val kalite: Int? = null,
)

data class AspData(
    val alternatif: String,
    val embed: String
)
