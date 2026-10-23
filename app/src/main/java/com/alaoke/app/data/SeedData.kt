package com.alaoke.app.data

/**
 * A small starter library so the Songs screen isn't empty on first launch.
 * These are title/artist references only (no bundled audio/video) — tapping one
 * routes the user to YouTube Search prefilled with this title+artist so they can
 * pick and play the official/legal karaoke video for it.
 */
object SeedData {
    val starterSongs = listOf(
        Song(title = "Perfect", artist = "Ed Sheeran", eventTags = "wedding,party"),
        Song(title = "My Way", artist = "Frank Sinatra", eventTags = "birthday,party"),
        Song(title = "Dancing Queen", artist = "ABBA", eventTags = "party,newyear"),
        Song(title = "All I Want for Christmas Is You", artist = "Mariah Carey", eventTags = "christmas"),
        Song(title = "Happy Birthday to You", artist = "Traditional", eventTags = "birthday"),
        Song(title = "A Thousand Years", artist = "Christina Perri", eventTags = "wedding"),
        Song(title = "Auld Lang Syne", artist = "Traditional", eventTags = "newyear"),
        Song(title = "Jingle Bell Rock", artist = "Bobby Helms", eventTags = "christmas"),
        Song(title = "I Gotta Feeling", artist = "The Black Eyed Peas", eventTags = "party,newyear"),
        Song(title = "Ikaw", artist = "Yeng Constantino", eventTags = "wedding,party"),
        Song(title = "Perfect Duet", artist = "Ed Sheeran & Beyonce", eventTags = "wedding"),
        Song(title = "Celebration", artist = "Kool & The Gang", eventTags = "birthday,party,newyear")
    )
}
