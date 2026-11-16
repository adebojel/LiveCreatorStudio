package id.livecreator.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.net.URI
import org.junit.Test

class SoundPackCatalogTest {
    @Test
    fun catalogIdsAreUniqueAndAllArchivesUseOfficialHttpsHost() {
        assertEquals(SoundPackCatalog.packs.size, SoundPackCatalog.packs.map { it.id }.toSet().size)
        SoundPackCatalog.packs.forEach { pack ->
            assertTrue(pack.archiveUrl.startsWith("https://"))
            assertTrue(pack.sourceUrl.startsWith("https://"))
            val host = URI(pack.archiveUrl).host.lowercase()
            assertTrue(
                "Unexpected catalog host: $host",
                host == "kenney.nl" || host.endsWith(".kenney.nl") ||
                    host == "opengameart.org" || host.endsWith(".opengameart.org") ||
                    host == "sfxmint.com" || host.endsWith(".sfxmint.com")
            )
        }
    }

    @Test
    fun starterLibraryIsSmallAndHasThreeCorePacks() {
        assertEquals(3, SoundPackCatalog.starterPacks.size)
        assertEquals(210, SoundPackCatalog.starterPacks.sumOf { it.assetCount })
    }

    @Test
    fun thirdPartyPackLicensesAreNotAssumedToBeCc0() {
        assertTrue(SoundPackCatalog.packs.filter { it.creator.contains("OpenGameArt") }
            .all { it.license == "See source page" })
        assertTrue(SoundPackCatalog.packs.filter { it.creator.startsWith("Kenney") || it.creator.startsWith("SFXMint") }
            .all { it.license == "CC0-1.0" })
    }

    @Test
    fun allThirtyFourCuratedPacksContainMoreThanSixteenHundredAssets() {
        assertEquals(34, SoundPackCatalog.packs.size)
        assertEquals(1_699, SoundPackCatalog.packs.sumOf { it.assetCount })
    }
}
