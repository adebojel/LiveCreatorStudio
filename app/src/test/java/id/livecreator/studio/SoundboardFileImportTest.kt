package id.livecreator.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundboardFileImportTest {
    @Test
    fun acceptsAudioMimeTypeEvenWhenProviderHasNoExtension() {
        assertTrue(SoundboardFileImport.isSupportedAudio("audio/x-custom", "stream"))
    }

    @Test
    fun acceptsKnownAudioExtensionWhenMimeTypeIsMissing() {
        assertTrue(SoundboardFileImport.isSupportedAudio(null, "intro.MP3"))
        assertTrue(SoundboardFileImport.isSupportedAudio("application/octet-stream", "effect.wav"))
    }

    @Test
    fun rejectsNonAudioDocuments() {
        assertFalse(SoundboardFileImport.isSupportedAudio("video/mp4", "clip.mp4"))
        assertFalse(SoundboardFileImport.isSupportedAudio(null, "notes.txt"))
    }

    @Test
    fun derivesSafeKnownExtensionFromMimeType() {
        assertEquals("wav", SoundboardFileImport.extensionFor("audio/x-wav", "recording"))
        assertEquals("mp3", SoundboardFileImport.extensionFor("audio/mpeg", "recording"))
        assertEquals("ogg", SoundboardFileImport.extensionFor("audio/ogg", "recording"))
    }
}
