package com.idlix.tv.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TitleLogoRepositoryTest {
    @Test fun readsMovieLogoFromCinemetaFormat() {
        val payload = """{"meta":{"id":"tt0133093","name":"The Matrix","poster":"https://example.com/poster.jpg","logo":"https://images.metahub.space/logo/medium/tt0133093/img"}}"""
        assertEquals(
            "https://images.metahub.space/logo/medium/tt0133093/img",
            parseCinemetaTitleLogo(payload)
        )
    }

    @Test fun readsSeriesLogoWithoutConfusingBackdrop() {
        val payload = """{"meta":{"id":"tt0944947","background":"https://example.org/bg.jpg","logo":"https://images.metahub.space/logo/medium/tt0944947/img"}}"""
        assertEquals(
            "https://images.metahub.space/logo/medium/tt0944947/img",
            parseCinemetaTitleLogo(payload)
        )
    }

    @Test fun rejectedOrMissingArtworksLeaveReadableTitleFallback() {
        assertNull(parseCinemetaTitleLogo("""{"meta":{"poster":"https://example.org/poster.jpg"}}"""))
        assertNull(parseCinemetaTitleLogo("""{"meta":{"logo":"http://images.metahub.space/logo/1"}}"""))
        assertNull(parseCinemetaTitleLogo("""{"meta":{"logo":"https://localhost/private"}}"""))
        assertNull(parseCinemetaTitleLogo("malformed-json"))
    }
}
