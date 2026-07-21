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
        coverUrl = "https://i.scdn.co/image/ab67616d0000b273881297eaa7d7e30a4ff9aa2f",
        streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
    )

    val trendingSongs = listOf(
        // English Hits
        Song(id = "trend_1", title = "Die With A Smile", artist = "Lady Gaga & Bruno Mars", duration = 251000, coverUrl = "https://i.scdn.co/image/ab67616d0000b2736780962657e0081d5854b79b", streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
        Song(id = "trend_2", title = "Birds of a Feather", artist = "Billie Eilish", duration = 210000, coverUrl = "https://i.scdn.co/image/ab67616d0000b27371d2d58edc10de4164ae96cd", streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
        
        // Punjabi Hits
        Song(id = "trend_3", title = "Winning Speech", artist = "Karan Aujla", duration = 184000, coverUrl = "https://i.scdn.co/image/ab67616d0000b273f5509d739818835824c000e3", streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
        Song(id = "trend_4", title = "Softly", artist = "Karan Aujla", duration = 156000, coverUrl = "https://i.scdn.co/image/ab67616d0000b273398930438f4d96030c6a3861", streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
        
        // Hindi / Bollywood Hits
        Song(id = "trend_5", title = "Tauba Tauba", artist = "Karan Aujla", duration = 207000, coverUrl = "https://i.scdn.co/image/ab67616d0000b27357488009230553655df00411", streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"),
        Song(id = "trend_6", title = "O Sajni Re", artist = "Arijit Singh", duration = 170000, coverUrl = "https://i.scdn.co/image/ab67616d0000b2734139965f7c320d93f7737c35", streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3"),
        
        // More Global
        Song(id = "trend_7", title = "Apt.", artist = "ROSÉ & Bruno Mars", duration = 169000, coverUrl = "https://i.scdn.co/image/ab67616d0000b27337424683569a91340a6b7d27", streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3")
    )

    val recentlyPlayed = listOf(
        Song(id = "recent_1", title = "Millionaire", artist = "Yo Yo Honey Singh", duration = 198000, coverUrl = "https://i.scdn.co/image/ab67616d0000b2732952467d3e69f3796d885906", streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"),
        Song(id = "recent_2", title = "Espresso", artist = "Sabrina Carpenter", duration = 175000, coverUrl = "https://i.scdn.co/image/ab67616d0000b2734f6645398d89e472f8832a8a", streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3"),
        Song(id = "recent_3", title = "Husn", artist = "Anuv Jain", duration = 217000, coverUrl = "https://i.scdn.co/image/ab67616d0000b27303f88f1107297e6823528b14", streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3")
    )

    val featuredSongs = listOf(
        Song(id = "feat_1", title = "Heartbreak Avenue", artist = "Sophie Lane", coverUrl = "https://images.unsplash.com/photo-1493225255756-d9584f8606e9?w=800&q=80", duration = 240000),
        Song(id = "feat_2", title = "Midnight Drive", artist = "Mason Mount", coverUrl = "https://images.unsplash.com/photo-1459749411177-042180ce673c?w=800&q=80", duration = 180000),
        Song(id = "feat_3", title = "Soft Vibes", artist = "Emily Bloom", coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800&q=80", duration = 210000)
    )

    val playlists = listOf(
        Playlist(id = "pl_1", name = "Today's Top Hits", songCount = 50, gradientIndex = 0),
        Playlist(id = "pl_2", name = "Punjabi 101", songCount = 45, gradientIndex = 1),
        Playlist(id = "pl_3", name = "Bollywood Butter", songCount = 100, gradientIndex = 2),
        Playlist(id = "pl_4", name = "Global Viral", songCount = 75, gradientIndex = 3),
        Playlist(id = "pl_5", name = "Deep Focus", songCount = 60, gradientIndex = 4)
    )

    val genres = listOf(
        Genre(id = "genre_1", name = "Pop", gradientKey = "pop", string = "#E91E63"),
        Genre(id = "genre_2", name = "Hip-Hop", gradientKey = "hip-hop", string = "#E91E63"),
        Genre(id = "genre_3", name = "Punjabi", gradientKey = "punjabi", string = "#E91E63"),
        Genre(id = "genre_4", name = "Hindi", gradientKey = "hindi", string = "#E91E63"),
        Genre(id = "genre_5", name = "Rock", gradientKey = "rock", string = "#E91E63"),
        Genre(id = "genre_6", name = "Classical", gradientKey = "classical", string = "#E91E63"),
        Genre(id = "genre_7", name = "Jazz", gradientKey = "jazz", string = "#E91E63"),
        Genre(id = "genre_8", name = "Lo-fi", gradientKey = "lo-fi", string = "#E91E63")
    )

    val trendingSearches = listOf(
        "Karan Aujla",
        "Arijit Singh",
        "Lady Gaga",
        "Taylor Swift",
        "Diljit Dosanjh"
    )

    val artists = listOf(
        Artist("1", "Karan Aujla", "https://i.scdn.co/image/ab6761610000e5eb6d8bd04e4a9d62e6e1b69766"),
        Artist("2", "Arijit Singh", "https://i.scdn.co/image/ab6761610000e5eb12816999386c9e01340156d9"),
        Artist("3", "Diljit Dosanjh", "https://i.scdn.co/image/ab6761610000e5eb8a63158b0c3d238ce1b69766"),
        Artist("4", "Lady Gaga", "https://i.scdn.co/image/ab6761610000e5eb05946c10e6a88b56f8f553a1"),
        Artist("5", "Billie Eilish", "https://i.scdn.co/image/ab6761610000e5eb71d2d58edc10de4164ae96cd"),
        Artist("6", "Hasan Raheem", "https://i.scdn.co/image/ab6761610000e5eb259d648b2d49e1a8e1b69766")
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
            title = "New Punjabi Release",
            message = "Karan Aujla just dropped a new single!",
            time = "2h ago",
            type = com.musicstream.app.domain.model.NotificationType.NEW_RELEASE
        ),
        com.musicstream.app.domain.model.Notification(
            id = "2",
            title = "Playlist Updated",
            message = "Your 'Punjabi 101' playlist has 15 new tracks.",
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
