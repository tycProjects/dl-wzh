package com.example.autocorrect

object AutoCorrectEngine {

    // Common typo fixes & contractions
    private val typoCorrections = mapOf(
        "teh" to "the",
        "taht" to "that",
        "thsi" to "this",
        "recieve" to "receive",
        "seperate" to "separate",
        "definately" to "definitely",
        "untill" to "until",
        "occured" to "occurred",
        "tommorrow" to "tomorrow",
        "alot" to "a lot",
        "beleive" to "believe",
        "wierd" to "weird",
        "accommodate" to "accommodate",
        "truely" to "truly",
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "im" to "I'm",
        "ive" to "I've",
        "id" to "I'd",
        "youre" to "you're",
        "theyre" to "they're",
        "weve" to "we've",
        "isnt" to "isn't",
        "arent" to "aren't",
        "wasnt" to "wasn't",
        "werent" to "weren't",
        "hasnt" to "hasn't",
        "havent" to "haven't",
        "hadnt" to "hadn't",
        "doesnt" to "doesn't",
        "didnt" to "didn't",
        "couldnt" to "couldn't",
        "shouldnt" to "shouldn't",
        "wouldnt" to "wouldn't",
        "thats" to "that's",
        "whats" to "what's",
        "wheres" to "where's",
        "hows" to "how's",
        "theres" to "there's",
        "heres" to "here's",
        "lets" to "let's"
    )

    // Common text abbreviations / shortcuts
    private val shortcuts = mapOf(
        "omw" to "On my way!",
        "brb" to "Be right back!",
        "idk" to "I don't know",
        "tbh" to "To be honest",
        "imo" to "In my opinion",
        "np" to "No problem!",
        "ty" to "Thank you!",
        "tysm" to "Thank you so much!",
        "btw" to "By the way",
        "fyi" to "For your information",
        "asap" to "as soon as possible",
        "gl" to "Good luck!",
        "gg" to "Good game!"
    )

    // Emoji keywords
    private val emojiKeywordMap = mapOf(
        "love" to "❤️",
        "heart" to "💖",
        "fire" to "🔥",
        "lit" to "🔥",
        "happy" to "😊",
        "smile" to "😄",
        "laugh" to "😂",
        "cool" to "😎",
        "star" to "⭐",
        "rocket" to "🚀",
        "party" to "🎉",
        "ok" to "👌",
        "yes" to "👍",
        "thumbs" to "👍",
        "no" to "👎",
        "clap" to "👏",
        "pray" to "🙏",
        "mind" to "🤯",
        "coffee" to "☕",
        "music" to "🎵",
        "check" to "✅",
        "crown" to "👑",
        "money" to "💰",
        "sparkles" to "✨",
        "magic" to "✨"
    )

    // High frequency English words corpus
    private val commonWords = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        "great", "keyboard", "android", "liquid", "glass", "custom", "theme", "color",
        "awesome", "beautiful", "fast", "speed", "super", "perfect", "smart", "typing",
        "message", "friend", "today", "tomorrow", "tonight", "morning", "night", "always",
        "never", "something", "everything", "nothing", "please", "thank", "thanks", "welcome",
        "phone", "mobile", "design", "style", "modern", "aesthetic", "clean", "simple"
    )

    /**
     * Get suggestions based on currently typed word prefix
     */
    fun getSuggestions(prefix: String, autoCorrect: Boolean): List<String> {
        if (prefix.isBlank()) {
            return listOf("I", "The", "Thanks", "Hello")
        }

        val clean = prefix.trim()
        val lower = clean.lowercase()
        val isCapitalized = clean.first().isUpperCase()

        val results = mutableListOf<String>()

        // 1. Check exact shortcut expansion
        shortcuts[lower]?.let {
            results.add(it)
        }

        // 2. Check exact typo correction
        if (autoCorrect) {
            typoCorrections[lower]?.let { correction ->
                val formatted = if (isCapitalized) correction.replaceFirstChar { it.uppercase() } else correction
                if (!results.contains(formatted)) results.add(formatted)
            }
        }

        // 3. Emoji match if applicable
        emojiKeywordMap[lower]?.let { emoji ->
            results.add("$clean $emoji")
        }

        // 4. Prefix matching from vocabulary
        val matches = commonWords.filter { it.startsWith(lower) }
            .take(5)
            .map { word ->
                if (isCapitalized) word.replaceFirstChar { it.uppercase() } else word
            }

        for (m in matches) {
            if (!results.contains(m) && m != clean) {
                results.add(m)
            }
            if (results.size >= 4) break
        }

        // If prefix is itself a valid word, ensure it or a candidate appears
        if (results.isEmpty()) {
            results.add(clean)
        } else if (!results.contains(clean)) {
            // Keep the exact typed word as an option so user can tap it to reject correction
            results.add(0, clean)
        }

        return results.distinct().take(4)
    }

    /**
     * Check if a word should be auto-corrected when space or punctuation is pressed
     */
    fun checkAutoCorrection(word: String): String {
        val lower = word.lowercase()
        // Check shortcut
        shortcuts[lower]?.let { return it }
        // Check typo
        typoCorrections[lower]?.let {
            return if (word.first().isUpperCase()) it.replaceFirstChar { c -> c.uppercase() } else it
        }
        return word
    }
}
