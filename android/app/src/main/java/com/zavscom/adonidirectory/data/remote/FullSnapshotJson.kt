package com.zavscom.adonidirectory.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class FullSnapshotJson(
    val version: Int? = null,
    @SerialName("generatedAt") val generatedAt: String? = null,
    val town: String? = null,
    val businesses: List<BusinessJson> = emptyList(),
)

@Serializable
data class BusinessJson(
    val id: String,
    val name: String,
    val category: String,
    @SerialName("subCategory") val subCategory: String? = null,
    val address: String = "",
    val area: String = "",
    val pincode: String = "",
    val city: String = "",
    val state: String = "",
    val phone: String? = null,
    val whatsapp: String? = null,
    val email: String? = null,
    val website: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val source: String = "",
    @SerialName("lastSeenAt") val lastSeenAt: String = "",
    val extra: JsonObject? = null,
)
