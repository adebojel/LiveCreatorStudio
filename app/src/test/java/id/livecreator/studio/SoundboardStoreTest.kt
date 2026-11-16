package id.livecreator.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoundboardStoreTest {
    @Test
    fun classifiesPackFilesAndImportedDocumentUris() {
        assertEquals(true, isPackSoundUri("file:///data/user/0/id.livecreator.studio/files/sound.ogg"))
        assertEquals(false, isPackSoundUri("content://media/audio/1"))
    }

    @Test
    fun encodeAndDecodePreserveUriAndTitle() {
        val expected = listOf(
            SoundItem("content://media/audio/1", "Intro music"),
            SoundItem("content://media/audio/2", "Alert | special")
        )
        assertEquals(expected.sortedBy { it.title.lowercase() }, SoundboardStore.decode(SoundboardStore.encode(expected)))
    }

    @Test
    fun encodeSanitizesTabsAndNewlinesInTitles() {
        val encoded = SoundboardStore.encode(
            listOf(SoundItem("content://media/audio/9", "Line one\tLine two\nLine three"))
        )
        assertEquals(
            listOf(SoundItem("content://media/audio/9", "Line one Line two Line three")),
            SoundboardStore.decode(encoded)
        )
    }

    @Test
    fun decodeSupportsLegacyPipeDelimitedEntries() {
        val decoded = SoundboardStore.decode(setOf("content://media/audio/3|Legacy title"))
        assertEquals(listOf(SoundItem("content://media/audio/3", "Legacy title")), decoded)
    }

    @Test
    fun decodeDropsMalformedAndDuplicateEntries() {
        val decoded = SoundboardStore.decode(
            setOf(
                "malformed",
                "|missing uri",
                "content://media/audio/4|First",
                "content://media/audio/4|Duplicate"
            )
        )
        assertEquals(1, decoded.size)
        assertEquals("content://media/audio/4", decoded.single().uri)
    }
}
