package com.musicstream.app.data

import com.musicstream.app.domain.model.Artist
import com.musicstream.app.domain.model.Genre
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.model.User

object MockData {

    val featuredSong = Song(
        id = "featured_1",
        title = "Blinding Lights",
        artist = "The Weeknd",
        album = "After Hours",
        duration = 200000,
        playCount = 4200_000_000,
        gradientIndex = 0,
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
    )

    val trendingSongs = listOf(
        Song(id = "trend_1", title = "Starboy", artist = "The Weeknd", duration = 230000, playCount = 3100_000_000, gradientIndex = 0, streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
        Song(id = "trend_2", title = "Shape of You", artist = "Ed Sheeran", duration = 233000, playCount = 3800_000_000, gradientIndex = 1, streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
        Song(id = "trend_3", title = "Stay", artist = "The Kid LAROI & Justin Bieber", duration = 141000, playCount = 2900_000_000, gradientIndex = 2, streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
        Song(id = "trend_4", title = "As It Was", artist = "Harry Styles", duration = 167000, playCount = 2700_000_000, gradientIndex = 3, streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"),
        Song(id = "trend_5", title = "Flowers", artist = "Miley Cyrus", duration = 200000, playCount = 2100_000_000, gradientIndex = 4, streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3")
    )

    val recentlyPlayed = listOf(
        Song(id = "recent_1", title = "Levitating", artist = "Dua Lipa", duration = 203000, isFavorite = false, gradientIndex = 0, streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3"),
        Song(id = "recent_2", title = "Peaches", artist = "Justin Bieber", duration = 198000, isFavorite = true, gradientIndex = 1, streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"),
        Song(id = "recent_3", title = "Save Your Tears", artist = "The Weeknd", duration = 215000, isFavorite = false, gradientIndex = 2, streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3"),
        Song(id = "recent_4", title = "Heat Waves", artist = "Glass Animals", duration = 238000, isFavorite = false, gradientIndex = 3, streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3"),
        Song(id = "recent_5", title = "Bad Guy", artist = "Billie Eilish", duration = 194000, isFavorite = true, gradientIndex = 4, streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3")
    )

    val playlists = listOf(
        Playlist(id = "pl_1", name = "Today's Top Hits", songCount = 50, gradientIndex = 0),
        Playlist(id = "pl_2", name = "RapCaviar", songCount = 45, gradientIndex = 1),
        Playlist(id = "pl_3", name = "All Out 2010s", songCount = 100, gradientIndex = 2),
        Playlist(id = "pl_4", name = "Rock Classics", songCount = 75, gradientIndex = 3),
        Playlist(id = "pl_5", name = "Deep Focus", songCount = 60, gradientIndex = 4)
    )

    val genres = listOf(
        Genre(id = "genre_1", name = "Pop", gradientKey = "pop"),
        Genre(id = "genre_2", name = "Hip-Hop", gradientKey = "hip-hop"),
        Genre(id = "genre_3", name = "Electronic", gradientKey = "electronic"),
        Genre(id = "genre_4", name = "R&B", gradientKey = "r&b"),
        Genre(id = "genre_5", name = "Rock", gradientKey = "rock"),
        Genre(id = "genre_6", name = "Classical", gradientKey = "classical"),
        Genre(id = "genre_7", name = "Jazz", gradientKey = "jazz"),
        Genre(id = "genre_8", name = "Lo-fi", gradientKey = "lo-fi")
    )

    val trendingSearches = listOf(
        "The Weeknd",
        "Drake",
        "Taylor Swift",
        "Top 50 Global"
    )

    val artists = listOf(
        Artist("1", "Hasan Raheem", "https://i.scdn.co/image/ab6761610000e5eb259d648b2d49e1a8e1b69766"),
        Artist("2", "Jassie Gill", "https://i.scdn.co/image/ab6761610000e5eb9d7008779b69e4f5093a677e"),
        Artist("3", "Dhanju", "https://i.scdn.co/image/ab6761610000e5eb7c8a49339e7c5d79907f1f0a"),
        Artist("4", "Parmish Verma", "https://i.scdn.co/image/ab6761610000e5eb66750059e0a0a1a8e1b69766"),
        Artist("5", "Karan Aujla", "https://i.scdn.co/image/ab6761610000e5eb6d8bd04e4a9d62e6e1b69766"),
        Artist("6", "Diljit Dosanjh", "https://i.scdn.co/image/ab6761610000e5eb8a63158b0c3d238ce1b69766")
    )

    val currentUser = User(
        id = "user_1",
        name = "Alex Rivera",
        email = "alex.rivera@email.com",
        isPremium = true,
        songCount = 247,
        playlistCount = 12,
        followingCount = 89,
        followersCount = 142
    )

    val notifications = listOf(
        com.musicstream.app.domain.model.Notification(
            id = "1",
            title = "New Release",
            message = "The Weeknd just dropped a new single 'Dancing in the Flames'!",
            time = "2h ago",
            type = com.musicstream.app.domain.model.NotificationType.NEW_RELEASE
        ),
        com.musicstream.app.domain.model.Notification(
            id = "2",
            title = "Playlist Updated",
            message = "Your 'Today's Top Hits' playlist has 15 new tracks.",
            time = "5h ago",
            type = com.musicstream.app.domain.model.NotificationType.PLAYLIST_UPDATE
        ),
        com.musicstream.app.domain.model.Notification(
            id = "3",
            title = "Premium Offer",
            message = "Get 3 months of Premium for the price of 1. Limited time offer!",
            time = "1d ago",
            type = com.musicstream.app.domain.model.NotificationType.PROMOTION
        )
    )

    fun formatPlayCount(count: Long): String {
        return when {
            count >= 1_000_000_000 -> "${count / 1_000_000_000}.${(count % 1_000_000_000) / 100_000_000}B"
            count >= 1_000_000 -> "${count / 1_000_000}.${(count % 1_000_000) / 100_000}M"
            count >= 1_000 -> "${count / 1_000}.${(count % 1_000) / 100}K"
            else -> count.toString()
        }
    }
}
