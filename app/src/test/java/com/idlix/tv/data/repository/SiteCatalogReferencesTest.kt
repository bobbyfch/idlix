package com.idlix.tv.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteCatalogReferencesTest {
    @Test fun collectionsAreFranchisesNotHomepageMovieSuggestions() {
        val titles = SiteCatalogReferences.collectionTitles
        assertEquals(12, titles.size)
        assertTrue(titles.contains("Marvel Cinematic Universe"))
        assertTrue(titles.contains("Toy Story"))
        assertFalse(titles.contains("The Concubine"))
        assertTrue(SiteCatalogReferences.collections.all { it.slug.isNotBlank() })
    }
    @Test fun exactTitleLookupIgnoresPunctuationButNotSubstrings() {
        assertTrue(SiteCatalogReferences.isExactTitle("D.P.", "DP"))
        assertFalse(SiteCatalogReferences.isExactTitle("Better Days Ahead", "Better Days"))
        assertTrue(SiteCatalogReferences.isExactTitle("Fangs of Fortune", "Fangs of Fortune"))
    }
    @Test fun everyExposedNetworkHasSearchSeeds() {
        listOf("netflix", "hbo", "prime-video", "disney-plus", "apple-tv-plus")
            .forEach { key ->
                assertTrue(SiteCatalogReferences.networkTitleSeeds[key].orEmpty().size >= 5)
                assertTrue(SiteCatalogReferences.networkAliases(key).isNotEmpty())
            }
    }
}
