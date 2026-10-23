package com.zikotv.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zikotv.app.R
import com.zikotv.app.player.PlayerActivity
import java.net.URL
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val MoviesBg = Color(0xFF070908)
private val MoviesAccent = Color(0xFF70E2B5)

private data class Movie(
    val id: Int,
    val title: String,
    val streamUrl: String,
    val posterUrl: String,
    val sourceGroup: String,
    val searchTitle: String
)

private data class MovieCategory(
    val id: String,
    val name: String,
    val movies: List<Movie>,
    val coverUrl: String,
    val description: String
)

private data class BrandDefinition(
    val id: String,
    val name: String,
    val description: String,
    val matches: (Movie) -> Boolean
)

private object MoviePosterCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bitmap: Bitmap) = cache.put(key, bitmap)
}

private fun attribute(line: String, key: String): String =
    Regex("(?<![\\w-])${Regex.escape(key)}=\"([^\"]*)\"")
        .find(line)?.groupValues?.getOrNull(1).orEmpty()

private fun isMetadataComplete(value: String): Boolean {
    var insideQuotes = false
    value.forEach { char ->
        if (char == '"') insideQuotes = !insideQuotes
        else if (char == ',' && !insideQuotes) return true
    }
    return false
}

private fun firstSeparatorComma(value: String): Int {
    var insideQuotes = false
    value.forEachIndexed { index, char ->
        if (char == '"') insideQuotes = !insideQuotes
        else if (char == ',' && !insideQuotes) return index
    }
    return -1
}

private fun parseMoviePlaylist(contents: String): List<Movie> {
    val result = ArrayList<Movie>(34_000)
    val lines = contents.lineSequence().toList()
    var index = 0

    while (index < lines.size) {
        val line = lines[index].trim().removePrefix("\uFEFF")
        if (!line.startsWith("#EXTINF", ignoreCase = true)) {
            index++
            continue
        }

        val builder = StringBuilder(line)
        while (!isMetadataComplete(builder.toString()) && index + 1 < lines.size) {
            index++
            builder.append(' ').append(lines[index].trim())
        }
        val metadata = builder.toString()
        val separator = firstSeparatorComma(metadata)
        val displayTitle = if (separator >= 0) metadata.substring(separator + 1).trim() else ""
        val metadataTitle = attribute(metadata, "tvg-name")
        val poster = attribute(metadata, "tvg-logo")
        val group = attribute(metadata, "group-title").ifBlank { "Uncategorized" }

        // A small number of playlist rows wrap the display name across extra lines.
        // Skip those continuation lines and stop only at a real stream URL.
        var cursor = index + 1
        var streamUrl: String? = null
        val titleContinuations = ArrayList<String>(2)
        while (cursor < lines.size) {
            val candidate = lines[cursor].trim().removePrefix("\uFEFF")
            when {
                candidate.isBlank() -> cursor++
                candidate.startsWith("#EXTINF", ignoreCase = true) -> break
                candidate.startsWith("#") -> cursor++
                candidate.startsWith("http://", ignoreCase = true) ||
                    candidate.startsWith("https://", ignoreCase = true) ||
                    candidate.startsWith("rtmp://", ignoreCase = true) ||
                    candidate.startsWith("rtsp://", ignoreCase = true) -> {
                    streamUrl = candidate
                    break
                }
                else -> {
                    titleContinuations += candidate
                    cursor++
                }
            }
        }

        if (streamUrl != null) {
            val combinedDisplay = (listOf(displayTitle) + titleContinuations).filter { it.isNotBlank() }.joinToString(" ").trim()
            val title = metadataTitle.ifBlank { combinedDisplay }.ifBlank { "Untitled Movie ${result.size + 1}" }
            result += Movie(
                id = result.size,
                title = title,
                streamUrl = streamUrl,
                posterUrl = poster,
                sourceGroup = group,
                searchTitle = normalized(title)
            )
            index = cursor + 1
        } else {
            index++
        }
    }
    return result
}

private fun normalized(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")
    .lowercase(Locale.ROOT)
    .replace('’', '\'')
    .replace('′', '\'')
    .replace('–', '-')
    .replace('—', '-')

private val normalizedTermCache = ConcurrentHashMap<String, String>()

private fun hasTerm(normalizedTitle: String, terms: List<String>): Boolean = terms.any { rawTerm ->
    val term = normalizedTermCache.getOrPut(rawTerm) { normalized(rawTerm) }
    if (term.isBlank()) return@any false
    if (term.any { !it.isLetterOrDigit() } || term.contains(' ')) {
        normalizedTitle.contains(term)
    } else {
        var matchAt = normalizedTitle.indexOf(term)
        var found = false
        while (matchAt >= 0 && !found) {
            val beforeIsBoundary = matchAt == 0 || !normalizedTitle[matchAt - 1].isLetterOrDigit()
            val endAt = matchAt + term.length
            val afterIsBoundary = endAt >= normalizedTitle.length || !normalizedTitle[endAt].isLetterOrDigit()
            found = beforeIsBoundary && afterIsBoundary
            if (!found) matchAt = normalizedTitle.indexOf(term, matchAt + 1)
        }
        found
    }
}

private fun isDisney(movie: Movie): Boolean {
    if (movie.sourceGroup.contains("DISNEY", ignoreCase = true)) return true
    return hasTerm(movie.searchTitle, listOf(
        "the lion king", "roi lion", "le roi lion", "frozen", "reine des neiges", "aladdin",
        "beauty and the beast", "la belle et la bete", "the little mermaid", "la petite sirene",
        "snow white", "blanche-neige", "cinderella", "cendrillon", "mulan", "tarzan", "bambi",
        "peter pan", "pocahontas", "hercules", "hercule", "moana", "vaiana", "encanto",
        "zootopia", "zootopie", "lilo & stitch", "lilo et stitch", "winnie the pooh", "winnie l'ourson",
        "the jungle book", "le livre de la jungle", "the princess and the frog", "la princesse et la grenouille",
        "tangled", "raiponce", "wreck-it ralph", "les mondes de ralph", "big hero 6", "les nouveaux heros"
    )) || isPixar(movie)
}

private fun isMarvel(movie: Movie): Boolean = movie.sourceGroup.contains("MARVEL", ignoreCase = true) || hasTerm(movie.searchTitle, listOf(
    "avengers", "marvel", "iron man", "captain america", "captain marvel", "thor", "guardians of the galaxy",
    "gardiens de la galaxie", "ant-man", "ant man", "black panther", "doctor strange", "docteur strange",
    "deadpool", "fantastic four", "les 4 fantastiques", "venom", "x-men", "x men", "wolverine", "eternals",
    "spider-man", "spiderman", "spider man", "morbius", "shang-chi", "shang chi", "blade", "daredevil",
    "the punisher", "punisher", "ghost rider", "moon knight", "black widow", "elektra"
))

private fun isDc(movie: Movie): Boolean = movie.sourceGroup.contains("DC COMICS", ignoreCase = true) ||
    movie.sourceGroup.trim().equals("DC", ignoreCase = true) || hasTerm(movie.searchTitle, listOf(
    "batman", "superman", "wonder woman", "justice league", "aquaman", "the flash", "shazam", "green lantern",
    "suicide squad", "joker", "black adam", "blue beetle", "constantine", "birds of prey", "the batman",
    "batgirl", "catwoman", "man of steel", "teen titans", "dc comics"
))

private fun isPixar(movie: Movie): Boolean = movie.sourceGroup.contains("PIXAR", ignoreCase = true) || hasTerm(movie.searchTitle, listOf(
    "toy story", "finding nemo", "finding dory", "inside out", "vice versa", "the incredibles", "les indestructibles",
    "cars", "ratatouille", "wall-e", "walle", "monsters, inc", "monstres et cie", "coco", "soul (2020)",
    "elemental", "luca (2021)", "onward", "en avant", "a bug's life", "1001 pattes", "brave", "rebelle (2012)",
    "turning red", "alerte rouge", "the good dinosaur", "le voyage d'arlo", "lightyear", "buzz l'eclair",
    "monsters university", "monstres academy", "up (2009)", "là-haut", "la-haut"
))

private fun isDreamWorks(movie: Movie): Boolean = movie.sourceGroup.contains("DREAMWORKS", ignoreCase = true) || hasTerm(movie.searchTitle, listOf(
    "shrek", "kung fu panda", "how to train your dragon", "madagascar", "the bad guys", "boss baby",
    "puss in boots", "trolls", "the croods", "les croods", "rise of the guardians", "spirit stallion",
    "megamente", "megamind", "monsters vs aliens", "bee movie", "the prince of egypt", "le prince d'egypte"
))

private fun isStarWars(movie: Movie): Boolean = movie.sourceGroup.contains("STAR WARS", ignoreCase = true) || hasTerm(movie.searchTitle, listOf("star wars", "rogue one", "the mandalorian", "mandalorian", "the force awakens"))
private fun isHarryPotter(movie: Movie): Boolean = movie.sourceGroup.contains("HARRY POTTER", ignoreCase = true) || hasTerm(movie.searchTitle, listOf("harry potter", "fantastic beasts", "bêtes fantastiques", "betes fantastiques"))
private fun isAnime(movie: Movie): Boolean = movie.sourceGroup.contains("ANIME", ignoreCase = true) || hasTerm(movie.searchTitle, listOf(
    "demon slayer", "one piece", "naruto", "dragon ball", "my hero academia", "jujutsu kaisen", "studio ghibli",
    "spirited away", "howl's moving castle", "your name. (2016)", "the last: naruto", "pokemon the movie",
    "pokemon: the", "death note", "attack on titan", "bleach", "detective conan", "suzume (2022)"
))
private fun isNetflix(movie: Movie): Boolean = movie.sourceGroup.contains("NETFLIX", ignoreCase = true)
private fun isJurassic(movie: Movie): Boolean = hasTerm(movie.searchTitle, listOf("jurassic park", "jurassic world"))
private fun isFastFurious(movie: Movie): Boolean = hasTerm(movie.searchTitle, listOf("fast & furious", "fast and furious", "the fast and the furious", "furious 7", "f9: the fast saga"))
private fun isTransformers(movie: Movie): Boolean = hasTerm(movie.searchTitle, listOf("transformers", "bumblebee (2018)"))
private fun isLordOfRings(movie: Movie): Boolean = hasTerm(movie.searchTitle, listOf("the lord of the rings", "lord of the rings", "the hobbit", "le seigneur des anneaux", "bilbo le hobbit"))
private fun isJamesBond(movie: Movie): Boolean = hasTerm(movie.searchTitle, listOf("james bond", "007", "casino royale", "quantum of solace", "skyfall", "spectre (2015)", "no time to die"))

private fun englishGroupName(raw: String): String {
    val upper = raw.uppercase(Locale.ROOT)
    val year = Regex("20\\d{2}").find(upper)?.value
    return when {
        upper.contains("DISNEY") -> "Disney"
        upper.contains("NETFLIX") -> "Netflix Movies"
        upper.contains("FRANCE ANIME") -> "Anime"
        upper.contains("ENGLISH OLD") -> "English Classics"
        upper.contains("ENGLISH MOVIES 23/24") -> "English Movies 2023–2024"
        upper.contains("MULTI SUBTITLE") && year != null -> "$year Multi-Subtitle Movies"
        upper.contains("MULTI SUBTITLE") -> "Multi-Subtitle Movies"
        upper.contains("ENGLISH MOVIES") && year != null -> "English Movies $year"
        upper.contains("INDIA MOVIES 90S") -> "Indian Movies: 1990s"
        upper.contains("INDIA MOVIES 2000") -> "Indian Movies: 2000–2016"
        upper.contains("INDIA MOVIES 2017") -> "Indian Movies: 2017–2018"
        upper.contains("INDIA MOVIES 2019") -> "Indian Movies: 2019–2021"
        upper.contains("INDIA MOVIES 2022") -> "Indian Movies: 2022"
        upper.contains("INDIA MOVIES 2023") -> "Indian Movies: 2023"
        upper.contains("INDIA MOVIES 24/25") -> "Indian Movies: 2024–2025"
        upper.contains("INDIA MOVIES 2026") -> "Indian Movies: 2026"
        upper.contains("INDIAN MOVIES") -> "Indian Movies"
        upper.contains("FRANCE COMEDY") -> "French Comedy"
        upper.contains("FRANCE DRAMA") -> "French Drama"
        upper.contains("FRANCE ACTION") -> "French Action"
        upper.contains("FRANCE DOCUMENT") -> "French Documentaries"
        upper.contains("FRANCE THRILLER") -> "French Thrillers"
        upper.contains("FRANCE HORROR") -> "French Horror"
        upper.contains("FRANCE ROMANCE") -> "French Romance"
        upper.contains("FRANCE FAMILY") -> "French Family Movies"
        upper.contains("FRANCE FANTASY") -> "French Fantasy"
        upper.contains("FRANCE MOVIES") -> "French Movies"
        upper.contains("FRANCE NOUVEAUX") -> "New French Movies"
        upper.contains("ZAMAN") -> "Classic Movies"
        upper.contains("ACTION MOVIES") -> "Action Movies"
        upper.contains("COMEDY MOVIES") -> "Comedy Movies"
        upper.contains("HORROR MOVIES") -> "Horror Movies"
        upper.contains("DRAMA MOVIES") -> "Drama Movies"
        upper.contains("CRIME MOVIES") -> "Crime Movies"
        upper.contains("FAMILY MOVIES") -> "Family Movies"
        upper.contains("FANTASY MOVIES") -> "Fantasy Movies"
        upper.contains("DOC MOVIES") -> "Documentaries"
        upper.contains("KIDS MOVIES") -> "Kids Movies"
        upper.contains("ASIAN MOVIES") -> "Asian Movies"
        upper.contains("ENGLISH MOVIES") -> "English Movies"
        upper.contains("EGYPT MOVIES") -> "Egyptian Movies"
        upper.contains("ARABIC MOVIES") -> "Arabic Movies"
        upper.contains("GULF MOVIES") -> "Gulf Movies"
        upper.contains("TAMIL MOVIES") -> "Tamil Movies"
        upper.contains("TELUGU MOVIES") -> "Telugu Movies"
        upper.contains("KANNADA MOVIES") -> "Kannada Movies"
        upper.contains("MALAYALAM MOVIES") -> "Malayalam Movies"
        upper.contains("MARATHI MOVIES") -> "Marathi Movies"
        upper.contains("PANJABI MOVIES") -> "Punjabi Movies"
        upper.contains("GUJRATI MOVIES") -> "Gujarati Movies"
        upper.contains("BANGLA MOVIES") -> "Bangla Movies"
        upper.contains("PAKISTANI TELEFILMS") -> "Pakistani TV Films"
        upper.contains("PAKISTANI MOVIES") -> "Pakistani Movies"
        upper.contains("PHILIPPINES MOVIES") -> "Philippines Movies"
        upper.contains("TURKISH MOVIES") -> "Turkish Movies"
        upper.contains("KOREA MOVIES") -> "Korean Movies"
        upper.contains("IRAN MOVIES DUB") -> "Iranian Movies (Dubbed)"
        upper.contains("IRAN MOVIES") -> "Iranian Movies"
        upper.contains("THAILAND MOVIES") -> "Thai Movies"
        upper.contains("MOROCCO MOVIES") -> "Moroccan Movies"
        upper.contains("ALGERIA MOVIES") -> "Algerian Movies"
        upper.contains("TUNIS MOVIES") -> "Tunisian Movies"
        upper.contains("WORLD CUP") -> "World Cup"
        upper.contains("THREATER") -> "Theatre"
        upper.contains("UFC") || upper.contains("WWE") -> "UFC & WWE"
        upper.contains("MOVIE SERIES") -> "Movie Series"
        upper.contains("ADEL EMAM") -> "Adel Imam Collection"
        upper.contains("AHMED HELMY") -> "Ahmed Helmy Collection"
        upper.contains("AHMED EL SAKKA") -> "Ahmed El Sakka Collection"
        upper.contains("VAN DAMME") -> "Jean-Claude Van Damme Collection"
        upper.contains("BRAD PITT") -> "Brad Pitt Collection"
        upper.contains("PHILIPPINES") -> "Philippines Movies"
        upper.contains("OLD MOVIES") -> "Classic Movies"
        else -> raw.substringBefore(" - ").trim().replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT).split(' ').joinToString(" ") { word ->
                if (word.isBlank()) word else word.replaceFirstChar { it.uppercase(Locale.ROOT) }
            }.ifBlank { "Other Movies" }
    }
}

private fun isBrandManagedGroup(raw: String): Boolean = listOf(
    "DISNEY", "NETFLIX", "MARVEL", "DC COMICS", "PIXAR", "DREAMWORKS", "STAR WARS", "HARRY POTTER", "ANIME"
).any { raw.contains(it, ignoreCase = true) }

private fun preferredCoverTerms(categoryName: String): List<String> = when (categoryName.lowercase(Locale.ROOT)) {
    "all", "all movies" -> listOf("avengers: endgame", "avengers endgame", "the lion king", "le roi lion", "avatar (2009)", "titanic (1997)", "toy story (1995)")
    "disney" -> listOf("the lion king", "le roi lion", "frozen (2013)", "la reine des neiges", "toy story (1995)", "zootopia (2016)", "aladdin (1992)")
    "marvel" -> listOf("avengers: endgame", "avengers endgame", "avengers infinity war", "iron man (2008)", "spider-man: no way home")
    "dc" -> listOf("the dark knight (2008)", "the batman (2022)", "joker (2019)", "superman (1978)", "justice league (2017)")
    "pixar" -> listOf("toy story (1995)", "finding nemo (2003)", "inside out (2015)", "ratatouille (2007)", "coco (2017)")
    "dreamworks" -> listOf("shrek (2001)", "kung fu panda (2008)", "how to train your dragon (2010)", "madagascar (2005)")
    "star wars" -> listOf("star wars (1977)", "star wars: episode iv", "rogue one")
    "harry potter" -> listOf("harry potter and the philosopher's stone", "harry potter a l'ecole des sorciers", "harry potter à l'école")
    "anime" -> listOf("spirited away", "your name. (2016)", "one piece film: red", "demon slayer")
    "netflix" -> listOf("red notice", "the gray man", "extraction (2020)", "bird box")
    "action movies", "french action" -> listOf("the dark knight", "mission: impossible", "john wick", "mad max: fury road", "kiss of the dragon")
    "comedy movies", "french comedy" -> listOf("home alone", "the mask (1994)", "the hangover", "the new guy (2002)")
    "horror movies", "french horror" -> listOf("the conjuring", "it (2017)", "a nightmare on elm street", "halloween (1978)")
    "family movies", "kids movies" -> listOf("toy story (1995)", "frozen (2013)", "the lion king", "zootopia (2016)")
    "classic movies", "english classics" -> listOf("the godfather", "back to the future", "titanic (1997)", "star wars (1977)")
    "indian movies", "indian movies: 1990s", "indian movies: 2000–2016", "indian movies: 2017–2018", "indian movies: 2019–2021", "indian movies: 2022", "indian movies: 2023", "indian movies: 2024–2025", "indian movies: 2026" -> listOf("rrr (2022)", "baahubali", "dangal (2016)", "kgf", "pushpa", "3 idiots (2009)", "pathaan (2023)")
    "english movies", "english movies 2025", "english movies 2026", "english movies 2023–2024" -> listOf("dune: part two", "oppenheimer (2023)", "barbie (2023)", "deadpool & wolverine", "top gun: maverick", "avatar (2009)", "spider-man: no way home")
    "arabic movies", "egyptian movies", "gulf movies" -> listOf("the blue elephant", "el feel el azraq", "al-mamar", "el mamar", "x large", "omar & salma", "casablanca (2019)")
    "french movies", "new french movies" -> listOf("intouchables", "the intouchables", "taxi (1998)", "amelie", "leon: the professional", "bienvenue chez les chtis")
    "crime movies" -> listOf("the godfather", "pulp fiction", "goodfellas", "se7en", "the departed")
    "asian movies", "korean movies" -> listOf("parasite (2019)", "train to busan", "oldboy (2003)", "the host (2006)")
    "drama movies" -> listOf("the shawshank redemption", "forrest gump", "the pursuit of happyness", "the green mile")
    "documentaries", "french documentaries" -> listOf("free solo", "march of the penguins", "our planet")
    else -> emptyList()
}

private fun chooseCategoryCover(categoryName: String, entries: List<Movie>): String {
    val terms = preferredCoverTerms(categoryName)
    for (term in terms) {
        entries.firstOrNull { it.posterUrl.isNotBlank() && it.searchTitle.contains(normalizedTermCache.getOrPut(term) { normalized(term) }) }
            ?.let { return it.posterUrl }
    }
    return entries.firstOrNull { it.posterUrl.isNotBlank() }?.posterUrl.orEmpty()
}

private fun buildMovieCategories(movies: List<Movie>): List<MovieCategory> {
    if (movies.isEmpty()) return emptyList()
    val definitions = listOf(
        BrandDefinition("disney", "Disney", "Animation, family and Disney classics", ::isDisney),
        BrandDefinition("marvel", "Marvel", "Superheroes from the Marvel universe", ::isMarvel),
        BrandDefinition("dc", "DC", "DC superheroes and comic-book movies", ::isDc),
        BrandDefinition("pixar", "Pixar", "Pixar animation collection", ::isPixar),
        BrandDefinition("dreamworks", "DreamWorks", "DreamWorks animation collection", ::isDreamWorks),
        BrandDefinition("starwars", "Star Wars", "The Star Wars movie universe", ::isStarWars),
        BrandDefinition("harrypotter", "Harry Potter", "Wizarding World movies", ::isHarryPotter),
        BrandDefinition("anime", "Anime", "Anime films and Japanese animation", ::isAnime),
        BrandDefinition("netflix", "Netflix", "Movies grouped under Netflix in the source", ::isNetflix),
        BrandDefinition("jurassic", "Jurassic", "Jurassic Park and Jurassic World", ::isJurassic),
        BrandDefinition("fastfurious", "Fast & Furious", "The Fast & Furious collection", ::isFastFurious),
        BrandDefinition("transformers", "Transformers", "Transformers movie collection", ::isTransformers),
        BrandDefinition("lordoftherings", "The Lord of the Rings", "Middle-earth movie collection", ::isLordOfRings),
        BrandDefinition("jamesbond", "James Bond", "The 007 movie collection", ::isJamesBond)
    )

    val categories = ArrayList<MovieCategory>()
    categories += MovieCategory(
        id = "all",
        name = "All",
        movies = movies,
        coverUrl = chooseCategoryCover("All", movies),
        description = "Every movie in your playlist"
    )

    definitions.forEach { definition ->
        val entries = movies.filter(definition.matches)
        if (entries.isNotEmpty()) {
            categories += MovieCategory(definition.id, definition.name, entries, chooseCategoryCover(definition.name, entries), definition.description)
        }
    }

    val sourceGroups = movies.groupBy { it.sourceGroup }
        .filterKeys { raw -> raw.isNotBlank() && !isBrandManagedGroup(raw) }
        .map { (raw, entries) ->
            val name = englishGroupName(raw)
            MovieCategory(
                id = "source:${raw.lowercase(Locale.ROOT)}",
                name = name,
                movies = entries,
                coverUrl = chooseCategoryCover(name, entries),
                description = raw.substringAfter(" - ", raw).trim()
            )
        }
        .sortedWith(
            compareBy<MovieCategory> {
                when (it.name) {
                    "Action Movies" -> 0
                    "Comedy Movies" -> 1
                    "Horror Movies" -> 2
                    "Drama Movies" -> 3
                    "English Classics" -> 4
                    "English Movies 2026" -> 5
                    "English Movies 2025" -> 6
                    "Indian Movies" -> 7
                    "Arabic Movies" -> 8
                    else -> 20
                }
            }.thenByDescending { it.movies.size }.thenBy { it.name.lowercase(Locale.ROOT) }
        )

    categories += sourceGroups
    return categories
}

@Composable
fun MoviesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var categories by remember { mutableStateOf<List<MovieCategory>>(emptyList()) }
    var isLoadingLibrary by remember { mutableStateOf(true) }

    LaunchedEffect(context) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val parsed = context.assets.open("all_movies.m3u8").bufferedReader().use { reader ->
                    parseMoviePlaylist(reader.readText())
                }
                parsed to buildMovieCategories(parsed)
            }.getOrNull()
        }
        movies = loaded?.first.orEmpty()
        categories = loaded?.second.orEmpty()
        isLoadingLibrary = false
    }

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }

    val selectedCategory = remember(categories, selectedCategoryId) {
        categories.firstOrNull { it.id == selectedCategoryId }
    }
    val query = search.trim()
    val searching = query.isNotEmpty()
    val visibleMovies = remember(movies, selectedCategory, query) {
        val source = if (searching) movies else selectedCategory?.movies.orEmpty()
        if (searching) source.filter { it.title.contains(query, ignoreCase = true) }
        else source
    }

    BackHandler {
        when {
            search.isNotEmpty() -> search = ""
            selectedCategoryId != null -> selectedCategoryId = null
            else -> onBack()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black, MoviesBg, Color(0xFF0B100D)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(126.dp)) {
                BackButton(
                    onClick = {
                        when {
                            search.isNotEmpty() -> search = ""
                            selectedCategoryId != null -> selectedCategoryId = null
                            else -> onBack()
                        }
                    },
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 17.dp, top = 18.dp)
                )
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(top = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ziko_logo),
                        contentDescription = "Ziko TV logo",
                        modifier = Modifier.size(53.dp).clip(RoundedCornerShape(17.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(5.dp))
                    Text("MOVIES", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 3.sp)
                }
            }

            MovieSearchBox(value = search, onValueChange = { search = it }, modifier = Modifier.padding(horizontal = 17.dp))
            Spacer(Modifier.height(13.dp))

            if (isLoadingLibrary) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = painterResource(R.drawable.ic_movies), contentDescription = null, tint = MoviesAccent, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(13.dp))
                        Text("PREPARING YOUR MOVIE LIBRARY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(7.dp))
                        Text("Reading titles and collections...", color = Color.White.copy(alpha = 0.44f), fontSize = 11.sp)
                    }
                }
            } else if (movies.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
                        Icon(painter = painterResource(R.drawable.ic_movies), contentDescription = null, tint = MoviesAccent, modifier = Modifier.size(38.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("MOVIE LIST UNAVAILABLE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Check that all_movies.m3u8 is included in the app assets.", color = Color.White.copy(alpha = 0.48f), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            } else if (!searching && selectedCategory == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 19.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("EXPLORE COLLECTIONS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                        Text("${formatCount(movies.size)} titles · ${categories.size - 1} collections", color = Color.White.copy(alpha = 0.46f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Box(Modifier.size(7.dp).clip(CircleShape).background(MoviesAccent))
                }
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 3.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        MovieCategoryCard(category = category, onClick = { selectedCategoryId = category.id })
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 19.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (searching) "SEARCH RESULTS" else selectedCategory?.name.orEmpty().uppercase(Locale.ROOT),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = if (searching) "${formatCount(visibleMovies.size)} matching movies" else "${formatCount(visibleMovies.size)} movies",
                            color = Color.White.copy(alpha = 0.46f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Text(
                        text = if (searching) "SEARCHING ALL" else "COLLECTION",
                        color = MoviesAccent.copy(alpha = 0.80f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.height(9.dp))
                if (visibleMovies.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(30.dp)) {
                            Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(38.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No movies found", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Try another title or clear the search.", color = Color.White.copy(alpha = 0.48f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 3.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(visibleMovies, key = { index, movie -> "${movie.id}-$index" }) { _, movie ->
                            MovieCard(movie = movie, onClick = {
                                context.startActivity(PlayerActivity.createIntent(context, movie.streamUrl, movie.title))
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieSearchBox(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
            .background(Color.White.copy(alpha = 0.065f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(17.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(R.drawable.ic_search), contentDescription = "Search movies", tint = MoviesAccent.copy(alpha = 0.83f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Search by movie title...", color = Color.White.copy(alpha = 0.42f), fontSize = 13.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                cursorBrush = SolidColor(MoviesAccent),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Text("CLEAR", color = MoviesAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onValueChange("") }.padding(start = 8.dp, end = 2.dp, top = 5.dp, bottom = 5.dp))
        }
    }
}

@Composable
private fun MovieCategoryCard(category: MovieCategory, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.78f).clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF26352E), Color(0xFF101411), Color(0xFF070807))))
            .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        PosterImage(
            url = category.coverUrl,
            title = category.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.02f),
                    0.42f to Color.Black.copy(alpha = 0.05f),
                    1f to Color.Black.copy(alpha = 0.92f)
                )
            )
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 13.dp, vertical = 14.dp)) {
            Text(
                text = category.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "${formatCount(category.movies.size)} MOVIES",
                color = MoviesAccent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.48f)).padding(horizontal = 9.dp, vertical = 6.dp)
        ) {
            Text(if (category.id == "all") "ALL" else "VIEW", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun MovieCard(movie: Movie, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .border(1.dp, Color.White.copy(alpha = 0.075f), RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .padding(7.dp)
    ) {
        PosterImage(
            url = movie.posterUrl,
            title = movie.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.69f).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = movie.title,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 9.dp, bottom = 4.dp)
        )
        Text(
            text = englishGroupName(movie.sourceGroup).uppercase(Locale.ROOT),
            color = MoviesAccent.copy(alpha = 0.72f),
            fontSize = 8.sp,
            letterSpacing = 0.55.sp,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 5.dp)
        )
    }
}

@Composable
private fun PosterImage(url: String, title: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    var bitmap by remember(url) { mutableStateOf(if (url.isBlank()) null else MoviePosterCache.get(url)) }
    var finishedLoading by remember(url) { mutableStateOf(url.isBlank() || MoviePosterCache.get(url) != null) }

    LaunchedEffect(url) {
        if (url.isBlank() || bitmap != null) {
            finishedLoading = true
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                URL(url).openConnection().apply {
                    connectTimeout = 6500
                    readTimeout = 6500
                    setRequestProperty("User-Agent", "ZikoTV/1.0 Android")
                }.getInputStream().use { input ->
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeStream(input, null, options)
                }
            }.getOrNull()?.also { MoviePosterCache.put(url, it) }
        }
        bitmap = loaded
        finishedLoading = true
    }

    Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(Color(0xFF18221D), Color(0xFF080A09)))),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "$title poster",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                Icon(painter = painterResource(R.drawable.ic_movies), contentDescription = null, tint = MoviesAccent.copy(alpha = 0.72f), modifier = Modifier.size(29.dp))
                Spacer(Modifier.height(7.dp))
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (finishedLoading) "COVER UNAVAILABLE" else "LOADING POSTER",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 7.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

private fun formatCount(value: Int): String = String.format(Locale.US, "%,d", value)
