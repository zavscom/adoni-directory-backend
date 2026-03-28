package com.zavscom.adonidirectory.ui.directory

/**
 * Fixed browse categories. [matchTokens] match [com.zavscom.adonidirectory.data.local.entity.BusinessEntity.category]
 * case-insensitively (substring or equality).
 */
data class CatalogEntry(
    val id: String,
    val title: String,
    val emoji: String,
    val matchTokens: Set<String>,
)

object CatalogDefinitions {

    const val MORE_ID = "more"

    val entries: List<CatalogEntry> = listOf(
        CatalogEntry(
            id = "hospitals",
            title = "Hospitals",
            emoji = "🏥",
            matchTokens = setOf("hospital", "hospitals", "nursing"),
        ),
        CatalogEntry(
            id = "doctors",
            title = "Doctors",
            emoji = "🩺",
            matchTokens = setOf("doctor", "doctors", "physician", "dental", "dentist", "dental clinic"),
        ),
        CatalogEntry(
            id = "lawyers",
            title = "Lawyers",
            emoji = "⚖️",
            matchTokens = setOf("lawyer", "lawyers", "legal", "advocate"),
        ),
        CatalogEntry(
            id = "construction",
            title = "Construction",
            emoji = "🏗️",
            matchTokens = setOf("construction", "builder", "contractor", "contractors", "civil"),
        ),
        CatalogEntry(
            id = "retail",
            title = "Retail Shops",
            emoji = "🛍️",
            matchTokens = setOf("shop", "shops", "retail", "store", "stores", "mart", "supermarket"),
        ),
        CatalogEntry(
            id = "schools",
            title = "Schools",
            emoji = "🏫",
            matchTokens = setOf("school", "schools", "college", "colleges", "university", "academy"),
        ),
        CatalogEntry(
            id = "restaurants",
            title = "Restaurants",
            emoji = "🍽️",
            matchTokens = setOf("restaurant", "restaurants", "hotel", "hotels", "cafe", "food"),
        ),
        CatalogEntry(
            id = "factories",
            title = "Factories",
            emoji = "🏭",
            matchTokens = setOf("factory", "factories", "manufacturing", "industrial"),
        ),
        CatalogEntry(
            id = "clinics",
            title = "Clinics",
            emoji = "💊",
            matchTokens = setOf("clinic", "clinics", "health center", "diagnostic"),
        ),
        CatalogEntry(
            id = "banks",
            title = "Banks",
            emoji = "🏦",
            matchTokens = setOf("bank", "banks", "atm", "finance"),
        ),
    )

    fun moreEntry(): CatalogEntry = CatalogEntry(
        id = MORE_ID,
        title = "More",
        emoji = "✨",
        matchTokens = emptySet(),
    )

    fun matchesEntry(businessCategory: String, entry: CatalogEntry): Boolean {
        if (entry.id == MORE_ID) return false
        val c = businessCategory.trim().lowercase()
        if (c.isEmpty()) return false
        return entry.matchTokens.any { tok ->
            c == tok || c.contains(tok)
        }
    }

    /** True if [businessCategory] is not matched by any primary catalog row (for "More" bucket). */
    fun matchesMoreBucket(businessCategory: String): Boolean {
        val c = businessCategory.trim().lowercase()
        if (c.isEmpty()) return true
        return entries.none { matchesEntry(businessCategory, it) }
    }

    fun matchesCatalogId(businessCategory: String, catalogId: String): Boolean {
        if (catalogId == MORE_ID) return matchesMoreBucket(businessCategory)
        val entry = entries.find { it.id == catalogId } ?: return false
        return matchesEntry(businessCategory, entry)
    }

    fun displayTitleForCatalogId(catalogId: String): String {
        if (catalogId == MORE_ID) return moreEntry().title
        return entries.find { it.id == catalogId }?.title ?: catalogId
    }
}
