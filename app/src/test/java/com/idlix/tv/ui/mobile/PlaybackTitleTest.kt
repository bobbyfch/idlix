package com.idlix.tv.ui.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTitleTest {
    @Test fun removesAnimeReleaseMetadata() {
        val x = readablePlaybackTitle("[Erai-raws] Shuiro no Kamen - 01 [1080p CR WEBRip HEVC AAC][MultiSub][59F09659]", true)
        assertEquals("Shuiro no Kamen", x.name)
        assertEquals("Episode 1", x.episodeLabel)
        assertEquals("1080P", x.quality)
    }
    @Test fun keepsMovieNamesWithNumbers() {
        val x = readablePlaybackTitle("2046 [1080p]", false)
        assertEquals("2046", x.name)
        assertEquals(null, x.episodeLabel)
    }
    @Test fun cleansDisneyReleaseFilenameFromScreenshot() {
        val x = readablePlaybackTitle("The.Fairy.2014.1080p.DSNP.WEB-DL.DDP5.1.H.264-EQRTZ", false)
        assertEquals("The Fairy 2014", x.name)
        assertEquals("1080P", x.quality)
    }
    @Test fun readsSeasonEpisodeAndHidesSourceTags() {
        val x = readablePlaybackTitle("Example.Show.S01E03.720p.WEB-DL.mkv", true)
        assertEquals("Example Show", x.name)
        assertEquals("S1 · E3", x.episodeLabel)
    }
    @Test fun preservesNormalTitles() {
        val x = readablePlaybackTitle("Film yang Belum Selesai", false)
        assertEquals("Film yang Belum Selesai", x.name)
    }
    @Test fun avoidsLosingNamesOnEmptyMetadata() {
        val x = readablePlaybackTitle("[1080p]", false)
        assertEquals("[1080p]", x.name)
    }
}
