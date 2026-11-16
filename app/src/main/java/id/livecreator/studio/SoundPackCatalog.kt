package id.livecreator.studio

/**
 * Curated sound packs from Kenney.nl. The upstream asset pages and each pack's
 * included License.txt identify these packs as Creative Commons Zero (CC0 1.0).
 * Keep the original source and license visible in the app and repository.
 */
internal data class SoundPack(
    val id: String,
    val title: String,
    val assetCount: Int,
    val archiveUrl: String,
    val sourceUrl: String,
    val includedInStarter: Boolean = false
)

internal object SoundPackCatalog {
    const val LICENSE = "CC0-1.0"
    const val CREATOR = "Kenney (kenney.nl)"

    val packs = listOf(
        SoundPack(
            "ui-audio", "UI Audio", 50,
            "https://kenney.nl/media/pages/assets/ui-audio/490d233f68-1677590494/kenney_ui-audio.zip",
            "https://kenney.nl/assets/ui-audio", true
        ),
        SoundPack(
            "interface-sounds", "Interface Sounds", 100,
            "https://kenney.nl/media/pages/assets/interface-sounds/fa43c1dd4d-1677589452/kenney_interface-sounds.zip",
            "https://kenney.nl/assets/interface-sounds", true
        ),
        SoundPack(
            "digital-audio", "Digital Audio", 60,
            "https://kenney.nl/media/pages/assets/digital-audio/216eac4753-1677590265/kenney_digital-audio.zip",
            "https://kenney.nl/assets/digital-audio", true
        ),
        SoundPack(
            "sci-fi-sounds", "Sci-Fi Sounds", 70,
            "https://kenney.nl/media/pages/assets/sci-fi-sounds/6b296f9ecf-1677589334/kenney_sci-fi-sounds.zip",
            "https://kenney.nl/assets/sci-fi-sounds"
        ),
        SoundPack(
            "impact-sounds", "Impact Sounds", 130,
            "https://kenney.nl/media/pages/assets/impact-sounds/87b4ddecda-1677589768/kenney_impact-sounds.zip",
            "https://kenney.nl/assets/impact-sounds"
        ),
        SoundPack(
            "rpg-audio", "RPG Audio", 50,
            "https://kenney.nl/media/pages/assets/rpg-audio/8e99002d76-1677590336/kenney_rpg-audio.zip",
            "https://kenney.nl/assets/rpg-audio"
        ),
        SoundPack(
            "casino-audio", "Casino Audio", 50,
            "https://kenney.nl/media/pages/assets/casino-audio/2472606a04-1721639069/kenney_casino-audio.zip",
            "https://kenney.nl/assets/casino-audio"
        ),
        SoundPack(
            "music-jingles", "Music Jingles", 85,
            "https://kenney.nl/media/pages/assets/music-jingles/f37e530b9e-1677590399/kenney_music-jingles.zip",
            "https://kenney.nl/assets/music-jingles"
        ),
        SoundPack(
            "voiceover-pack", "Voiceover Pack", 90,
            "https://kenney.nl/media/pages/assets/voiceover-pack/3f7f168698-1677589897/kenney_voiceover-pack.zip",
            "https://kenney.nl/assets/voiceover-pack"
        ),
        SoundPack(
            "voiceover-pack-fighter", "Voiceover Pack Fighter", 45,
            "https://kenney.nl/media/pages/assets/voiceover-pack-fighter/6ceb77c6f1-1677589837/kenney_voiceover-pack-fighter.zip",
            "https://kenney.nl/assets/voiceover-pack-fighter"
        )
    )

    val starterPacks: List<SoundPack> = packs.filter { it.includedInStarter }
}
