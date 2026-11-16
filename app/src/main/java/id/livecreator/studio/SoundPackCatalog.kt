package id.livecreator.studio

/**
 * Curated CC0 sound packs from Kenney.nl and reviewed OpenGameArt archive sources.
 * The upstream asset pages/source index identify these packs as Creative Commons Zero (CC0 1.0).
 * Keep the original source and license visible in the app and repository.
 */
internal data class SoundPack(
    val id: String,
    val title: String,
    val assetCount: Int,
    val archiveUrl: String,
    val sourceUrl: String,
    val includedInStarter: Boolean = false,
    val creator: String = "Kenney (kenney.nl)"
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
        ),
        SoundPack(
            "oga-512-retro", "Retro 8-bit Effects", 512,
            "https://opengameart.org/sites/default/files/The%20Essential%20Retro%20Video%20Game%20Sound%20Effects%20Collection%20%5B512%20sounds%5D.zip",
            "https://opengameart.org/content/512-sound-effects-8-bit-style",
            creator = "SubspaceAudio (OpenGameArt)"
        ),
        SoundPack(
            "oga-rpg-pack", "OpenGameArt RPG Sound Pack", 96,
            "https://opengameart.org/sites/default/files/rpg_sound_pack.zip",
            "https://opengameart.org/content/rpg-sound-pack",
            creator = "artisticdude (OpenGameArt)"
        ),
        SoundPack(
            "oga-zombies", "Zombie Sounds", 24,
            "https://opengameart.org/sites/default/files/zombies.zip",
            "https://opengameart.org/content/zombies-sound-pack",
            creator = "artisticdude (OpenGameArt)"
        ),
        SoundPack(
            "oga-footsteps", "Footsteps Surfaces", 8,
            "https://opengameart.org/sites/default/files/%5Bkdd%5DDifferentSteps_0.zip",
            "https://opengameart.org/content/different-steps-on-wood-stone-leaves-gravel-and-mud",
            creator = "TinyWorlds (OpenGameArt)"
        ),
        SoundPack(
            "oga-battle", "Battle Sound Effects", 4,
            "https://opengameart.org/sites/default/files/battle_sound_effects_0.zip",
            "https://opengameart.org/content/battle-sound-effects",
            creator = "Ogrebane (OpenGameArt)"
        ),
        SoundPack(
            "oga-retro-ui", "Retro UI Sound Pack", 70,
            "https://opengameart.org/sites/default/files/archive_3.zip",
            "https://opengameart.org/content/sound-pack",
            creator = "StumpyStrust (OpenGameArt)"
        ),
        SoundPack(
            "oga-sound-effects-pack-2", "Retro Arcade SFX Pack 2", 110,
            "https://opengameart.org/sites/default/files/Sound%20effects%20Pack%202.zip",
            "https://opengameart.org/content/sound-effects-pack-2",
            creator = "phoenix1291 / SwissArcadeGameEntertainment (OpenGameArt)"
        ),
        SoundPack(
            "oga-30-weird-sfx", "30 Weird & Funny SFX", 30,
            "https://opengameart.org/sites/default/files/30-cc0-weird-sfx.zip",
            "https://opengameart.org/content/30-weird-cc0-sfx",
            creator = "rubberduck (OpenGameArt)"
        ),
        SoundPack("sfx-kiss-cartoon", "Cartoon Kiss Pop", 1,
            "https://sfxmint.com/dl/kiss-03.mp3", "https://sfxmint.com/sounds/kiss-03",
            creator = "SFXMint (CC0 1.0; AI-generated)"),
        SoundPack("sfx-kiss-hand", "Hand Kiss", 1,
            "https://sfxmint.com/dl/kiss-07.mp3", "https://sfxmint.com/sounds/kiss-07",
            creator = "SFXMint (CC0 1.0; AI-generated)"),
        SoundPack("sfx-kiss-rapid", "Rapid Peck Kisses", 1,
            "https://sfxmint.com/dl/kiss-09.mp3", "https://sfxmint.com/sounds/kiss-09",
            creator = "SFXMint (CC0 1.0; AI-generated)"),
        SoundPack("sfx-laugh-villain", "Slow Villain Laugh", 1,
            "https://sfxmint.com/dl/laugh-01.mp3", "https://sfxmint.com/sounds/laugh-01",
            creator = "SFXMint (CC0 1.0; AI-generated)"),
        SoundPack("sfx-laugh-man", "Hearty Man Laugh", 1,
            "https://sfxmint.com/dl/laugh-02.mp3", "https://sfxmint.com/sounds/laugh-02",
            creator = "SFXMint (CC0 1.0; AI-generated)"),
        SoundPack("sfx-laugh-woman", "Warm Woman Laugh", 1,
            "https://sfxmint.com/dl/laugh-03.mp3", "https://sfxmint.com/sounds/laugh-03",
            creator = "SFXMint (CC0 1.0; AI-generated)"),
        SoundPack("sfx-laugh-giggle", "Silly Giggle", 1,
            "https://sfxmint.com/dl/laugh-05.mp3", "https://sfxmint.com/sounds/laugh-05",
            creator = "SFXMint (CC0 1.0; AI-generated)"),
        SoundPack("sfx-laugh-child", "Child Belly Laugh", 1,
            "https://sfxmint.com/dl/laugh-06.mp3", "https://sfxmint.com/sounds/laugh-06",
            creator = "SFXMint (CC0 1.0; AI-generated)"),
        SoundPack("sfx-laugh-mocking", "Mocking Laugh", 1,
            "https://sfxmint.com/dl/laugh-08.mp3", "https://sfxmint.com/sounds/laugh-08",
            creator = "SFXMint (CC0 1.0; AI-generated)"),
        SoundPack("sfx-gunshot", "Short Gunshot", 1,
            "https://sfxmint.com/dl/growth-65de718ff53302fbf797166e-retry.mp3",
            "https://sfxmint.com/sounds/short-gunshot",
            creator = "SFXMint (CC0 1.0; AI-generated)")
    )

    val starterPacks: List<SoundPack> = packs.filter { it.includedInStarter }
}
