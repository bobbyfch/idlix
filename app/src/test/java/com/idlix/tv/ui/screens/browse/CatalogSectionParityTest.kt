package com.idlix.tv.ui.screens.browse

import com.idlix.tv.data.repository.SiteCatalogReferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSectionParityTest {
    @Test fun sharedExploreTaxonomyHasNoDuplicateCollectionsSection() {
        val ids = catalogSections.map { it.value }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(listOf("genre", "country", "year", "network", "shorts"), ids)
        assertFalse(ids.contains("collections"))
    }

    @Test fun websiteNetworksAreAvailableInSharedTvAndMobileBrowse() {
        val networks = choicesFor("network")
        assertEquals(32, networks.size)
        assertEquals(networks.size, networks.map { it.value }.toSet().size)
        assertTrue(networks.any { it.value == "crunchyroll" })
        assertTrue(networks.any { it.value == "tvn" })
    }

    @Test fun websiteCollectionsBelongToSearchDiscoveryNotExploreFilters() {
        assertTrue(choicesFor("collections").isEmpty())
        assertTrue(SiteCatalogReferences.collectionTitles.isNotEmpty())
        assertTrue(SiteCatalogReferences.collections.all { it.slug.isNotBlank() })
    }
}
