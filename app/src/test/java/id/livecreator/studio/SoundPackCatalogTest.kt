package id.livecreator.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundPackCatalogTest {
    @Test
    fun catalogIdsAreUniqueAndAllArchivesUseOfficialHttpsHost() {
        assertEquals(SoundPackCatalog.packs.size, SoundPackCatalog.packs.map { it.id }.toSet().size)
        SoundPackCatalog.packs.forEach { pack ->
            assertTrue(pack.archiveUrl.startsWith("https://kenney.nl/media/"))
            assertTrue(pack.sourceUrl.startsWith("https://kenney.nl/assets/"))
        }
    }

    @Test
    fun starterLibraryIsSmallAndHasThreeCorePacks() {
        assertEquals(3, SoundPackCatalog.starterPacks.size)
        assertEquals(210, SoundPackCatalog.starterPacks.sumOf { it.assetCount })
    }

    @Test
    fun allTenCuratedPacksContainAboutSevenHundredAssets() {
        assertEquals(10, SoundPackCatalog.packs.size)
        assertEquals(730, SoundPackCatalog.packs.sumOf { it.assetCount })
    }
}
