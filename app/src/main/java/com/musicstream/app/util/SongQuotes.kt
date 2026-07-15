package com.musicstream.app.util

import com.musicstream.app.domain.model.Song

object SongQuotes {
    private val genericQuotes = listOf(
        "A limitless soundscape driving your vibe beyond every horizon. 🎧",
        "Where words fail, music speaks. Let this melody guide your soul. ✨",
        "Life is a song, love is the music. Enjoy every beat. ❤️",
        "Music is the shorthand of emotion. Feel the rhythm of your heart. 🎵",
        "Turn up the volume and let the world fade away. 🌊",
        "Every song has a story. What's yours today? 📖",
        "Music is the strongest form of magic. ✨",
        "Lost in the melody, found in the rhythm. 🌀",
        "Good music, good mood, good life. 🌟",
        "Let the music take you to places you've never been. 🚀",
        "One good thing about music, when it hits you, you feel no pain. 🎸",
        "Music is the moonlight in the gloomy night of life. 🌙",
        "Your life's soundtrack is playing right now. 🎹",
        "Dance like nobody's watching, listen like nobody's judging. 💃",
        "Music is life itself. What would we be without it? 🌍",
        "The rhythm of the heart is the melody of life. ❤️",
        "Music can change the world because it can change people. 🌎",
        "A song can bring back a thousand memories. 🕰️",
        "Let your heart sing along with the rhythm. 💓",
        "Music: the only universal language. 🗣️",
        "Soul meets sound. ⚡",
        "Find your frequency. 📻",
        "Vibrate higher with every note. 📈",
        "Echoes of your imagination. 💭",
        "Harmonize your day. 🌈"
    )

    fun getQuoteForSong(song: Song?): String {
        if (song == null) return genericQuotes[0]
        
        val title = song.title.lowercase()
        val artist = song.artist.lowercase()

        // Some keyword based custom quotes for better implementation
        return when {
            title.contains("love") || title.contains("heart") -> "Love is the greatest melody of all. ❤️"
            title.contains("night") || title.contains("midnight") -> "The night is alive with the sound of music. 🌙"
            title.contains("dance") || title.contains("party") -> "Keep the rhythm going and never stop dancing! 💃"
            title.contains("sad") || title.contains("cry") || title.contains("lonely") -> "Music heals what the heart can't say. 🕯️"
            title.contains("happy") || title.contains("joy") || title.contains("smile") -> "Spread the joy through the power of music! 😊"
            artist.contains("lofi") || title.contains("relax") || title.contains("chill") -> "Chill vibes only. Let the stress melt away. ☕"
            else -> {
                // Consistent selection based on ID
                val seed = song.id.hashCode().let { if (it < 0) -it else it }
                val index = seed % genericQuotes.size
                genericQuotes[index]
            }
        }
    }
}
