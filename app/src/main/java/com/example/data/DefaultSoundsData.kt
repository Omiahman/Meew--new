package com.example.data

object DefaultSoundsData {
    val CATEGORIES = listOf(
        "All",
        "Funny",
        "Troll",
        "Reaction",
        "Laugh",
        "Surprise",
        "Gaming",
        "Voice",
        "Effects"
    )

    fun getInitialSounds(): List<MemeSoundEntity> = listOf(
        // Funny
        MemeSoundEntity(
            soundKey = "bruh",
            name = "Bruh Sound #2",
            category = "Funny",
            iconName = "sentiment_very_dissatisfied",
            durationMs = 1200,
            isFavorite = true,
            isQuickAccess = true,
            quickAccessOrder = 1,
            waveformType = "bruh"
        ),
        MemeSoundEntity(
            soundKey = "sad_trombone",
            name = "Sad Trombone Wah-Wah",
            category = "Funny",
            iconName = "music_note",
            durationMs = 2800,
            isFavorite = false,
            waveformType = "sad_trombone"
        ),
        MemeSoundEntity(
            soundKey = "boing",
            name = "Cartoon Boing Spring",
            category = "Funny",
            iconName = "sports_gymnastics",
            durationMs = 900,
            isFavorite = false,
            waveformType = "boing"
        ),
        MemeSoundEntity(
            soundKey = "quack",
            name = "Rubber Duck Squeak",
            category = "Funny",
            iconName = "cruelty_free",
            durationMs = 800,
            isFavorite = false,
            waveformType = "quack"
        ),

        // Troll
        MemeSoundEntity(
            soundKey = "coffin_dance",
            name = "Coffin Dance Astronomia",
            category = "Troll",
            iconName = "celebration",
            durationMs = 3200,
            isFavorite = true,
            isQuickAccess = true,
            quickAccessOrder = 2,
            waveformType = "coffin_dance"
        ),
        MemeSoundEntity(
            soundKey = "weide_troll",
            name = "Directed by Weide",
            category = "Troll",
            iconName = "movie",
            durationMs = 2600,
            isFavorite = false,
            waveformType = "weide_troll"
        ),
        MemeSoundEntity(
            soundKey = "emotional_damage",
            name = "Emotional Damage!",
            category = "Troll",
            iconName = "heart_broken",
            durationMs = 1400,
            isFavorite = true,
            waveformType = "emotional_damage"
        ),

        // Reaction
        MemeSoundEntity(
            soundKey = "vine_boom",
            name = "Vine Boom Sub-Bass",
            category = "Reaction",
            iconName = "local_fire_department",
            durationMs = 1300,
            isFavorite = true,
            isQuickAccess = true,
            quickAccessOrder = 3,
            waveformType = "vine_boom"
        ),
        MemeSoundEntity(
            soundKey = "dramatic_dun",
            name = "Dramatic Dun-Dun-Dun",
            category = "Reaction",
            iconName = "campaign",
            durationMs = 2400,
            isFavorite = false,
            waveformType = "dramatic_dun"
        ),
        MemeSoundEntity(
            soundKey = "anime_wow",
            name = "Anime Kawaii Wow!",
            category = "Reaction",
            iconName = "star",
            durationMs = 1100,
            isFavorite = true,
            waveformType = "anime_wow"
        ),

        // Laugh
        MemeSoundEntity(
            soundKey = "wheeze_cackle",
            name = "Wheeze Laugh Cackle",
            category = "Laugh",
            iconName = "sentiment_very_satisfied",
            durationMs = 2200,
            isFavorite = true,
            isQuickAccess = true,
            quickAccessOrder = 4,
            waveformType = "wheeze_cackle"
        ),
        MemeSoundEntity(
            soundKey = "evil_laugh",
            name = "Evil Gamer Supervillain",
            category = "Laugh",
            iconName = "mood_bad",
            durationMs = 2500,
            isFavorite = false,
            waveformType = "evil_laugh"
        ),
        MemeSoundEntity(
            soundKey = "goofy_giggle",
            name = "Goofy Duck Giggle",
            category = "Laugh",
            iconName = "child_care",
            durationMs = 1600,
            isFavorite = false,
            waveformType = "goofy_giggle"
        ),

        // Surprise
        MemeSoundEntity(
            soundKey = "metal_pipe",
            name = "Metal Pipe Falling",
            category = "Surprise",
            iconName = "build",
            durationMs = 1800,
            isFavorite = true,
            isQuickAccess = true,
            quickAccessOrder = 5,
            waveformType = "metal_pipe"
        ),
        MemeSoundEntity(
            soundKey = "win_error",
            name = "Windows Critical Error",
            category = "Surprise",
            iconName = "warning",
            durationMs = 700,
            isFavorite = false,
            waveformType = "win_error"
        ),
        MemeSoundEntity(
            soundKey = "xfiles_alarm",
            name = "Illuminati Mystery Chime",
            category = "Surprise",
            iconName = "visibility",
            durationMs = 2500,
            isFavorite = false,
            waveformType = "xfiles_alarm"
        ),

        // Gaming
        MemeSoundEntity(
            soundKey = "booyah",
            name = "Free Fire Booyah Fanfare",
            category = "Gaming",
            iconName = "military_tech",
            durationMs = 2600,
            isFavorite = true,
            isQuickAccess = true,
            quickAccessOrder = 6,
            waveformType = "booyah"
        ),
        MemeSoundEntity(
            soundKey = "headshot",
            name = "Snip-Snap Headshot Kill",
            category = "Gaming",
            iconName = "track_changes",
            durationMs = 950,
            isFavorite = true,
            waveformType = "headshot"
        ),
        MemeSoundEntity(
            soundKey = "tactical_nuke",
            name = "Tactical Nuke Incoming",
            category = "Gaming",
            iconName = "warning_amber",
            durationMs = 3000,
            isFavorite = false,
            waveformType = "tactical_nuke"
        ),
        MemeSoundEntity(
            soundKey = "game_over",
            name = "8-Bit Retro Game Over",
            category = "Gaming",
            iconName = "sports_esports",
            durationMs = 2100,
            isFavorite = false,
            waveformType = "game_over"
        ),

        // Voice
        MemeSoundEntity(
            soundKey = "fbi_open_up",
            name = "FBI Open The Door!",
            category = "Voice",
            iconName = "door_front",
            durationMs = 2200,
            isFavorite = true,
            waveformType = "fbi_open_up"
        ),
        MemeSoundEntity(
            soundKey = "sheesh",
            name = "High Pitched Sheesh!",
            category = "Voice",
            iconName = "record_voice_over",
            durationMs = 1500,
            isFavorite = false,
            waveformType = "sheesh"
        ),
        MemeSoundEntity(
            soundKey = "running_voice",
            name = "Why Are You Running",
            category = "Voice",
            iconName = "directions_run",
            durationMs = 2000,
            isFavorite = false,
            waveformType = "running_voice"
        ),

        // Effects
        MemeSoundEntity(
            soundKey = "airhorn",
            name = "Triple MLG Airhorn",
            category = "Effects",
            iconName = "volume_up",
            durationMs = 1900,
            isFavorite = true,
            waveformType = "airhorn"
        ),
        MemeSoundEntity(
            soundKey = "heavy_punch",
            name = "Anime Heavy Punch Hit",
            category = "Effects",
            iconName = "sports_mma",
            durationMs = 850,
            isFavorite = false,
            waveformType = "heavy_punch"
        ),
        MemeSoundEntity(
            soundKey = "laser_pew",
            name = "Sci-Fi Blaster Pew-Pew",
            category = "Effects",
            iconName = "flare",
            durationMs = 800,
            isFavorite = false,
            waveformType = "laser_pew"
        )
    )
}
