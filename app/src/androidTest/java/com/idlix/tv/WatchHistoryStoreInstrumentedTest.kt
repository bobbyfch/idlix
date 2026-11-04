package com.idlix.tv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.idlix.tv.data.local.WatchHistoryStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** On-device regression: cleared watch history must stay cleared in memory. */
@RunWith(AndroidJUnit4::class)
class WatchHistoryStoreInstrumentedTest {
    @Test fun clearAllRemovesCachedEntriesWithoutRelaunch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = WatchHistoryStore.getInstance(context)
        store.clearAll()
        try {
            store.recordStartPlayback("audit-test-film", "movie", "Test Film")
            store.updateProgress("audit-test-film", 10_000L, 120_000L)
            assertEquals(1, store.getAllHistory().size)
            store.clearAll()
            assertTrue(store.getAllHistory().isEmpty())
            assertTrue(store.getUnfinishedItems().isEmpty())
            assertEquals(0L, store.getSavedPosition("audit-test-film"))
        } finally {
            store.clearAll()
        }
    }
}
