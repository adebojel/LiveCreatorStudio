package id.livecreator.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundPackCatalogTest {
    @Test
    fun catalogIdsAreUniqueAndAllArchivesUseOfficialHttpsHost() {
        assertEquals(SoundPackCatalog.packs.size, SoundPackCatalog.packs.map { it.id }.toSet().size)
        SoundPackCatalog.packs.forEach { pack ->
            assertTrue(pack.archiveUrl.startsWith("https://"))
            assertTrue(pack.sourceUrl.startsWith("https://"))
            assertTrue(
                pack.archiveUrl.contains("kenney.nl/") || pack.archiveUrl.contains("opengameart.org/")
            )
        }
    }

    @Test
    fun starterLibraryIsSmallAndHasThreeCorePacks() {
        assertEquals(3, SoundPackCatalog.starterPacks.size)
        assertEquals(210, SoundPackCatalog.starterPacks.sumOf { it.assetCount })
    }

    @Test
    fun allFifteenCuratedPacksContainAboutThirteenHundredAssets() {
        assertEquals(15, SoundPackCatalog.packs.size)
        assertEquals(1_374, SoundPackCatalog.packs.sumOf { it.assetCount })
    }
}
