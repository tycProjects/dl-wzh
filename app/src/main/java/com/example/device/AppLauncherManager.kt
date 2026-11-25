package com.example.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

sealed interface AppLaunchOutcome {
    data class Launched(val app: InstalledApp) : AppLaunchOutcome
    data class Ambiguous(val query: String, val candidates: List<InstalledApp>) : AppLaunchOutcome
    data class NotInstalled(val query: String) : AppLaunchOutcome
    data class Failed(val appLabel: String, val reason: String) : AppLaunchOutcome
}

class AppLauncherManager(private val context: Context) {

    private val bengaliToEnglishAliases: Map<String, List<String>> = mapOf(
        "ফেসবুক" to listOf("facebook", "fb"),
        "ফেইসবুক" to listOf("facebook", "fb"),
        "এফবি" to listOf("facebook", "fb"),
        "ইউটিউব" to listOf("youtube", "yt"),
        "ইউটুব" to listOf("youtube"),
        "মেসেঞ্জার" to listOf("messenger"),
        "ম্যাসেঞ্জার" to listOf("messenger"),
        "হোয়াটসঅ্যাপ" to listOf("whatsapp"),
        "হোয়াটস অ্যাপ" to listOf("whatsapp"),
        "ওয়াটসঅ্যাপ" to listOf("whatsapp"),
        "ইনস্টাগ্রাম" to listOf("instagram", "insta"),
        "ইন্সটাগ্রাম" to listOf("instagram"),
        "টিকটক" to listOf("tiktok"),
        "টিক টক" to listOf("tiktok"),
        "ক্রোম" to listOf("chrome", "google chrome"),
        "গুগল ক্রোম" to listOf("chrome"),
        "গুগল" to listOf("google"),
        "জিমেইল" to listOf("gmail", "mail"),
        "ইমেইল" to listOf("gmail", "mail", "email"),
        "ক্যামেরা" to listOf("camera"),
        "গ্যালারি" to listOf("gallery", "photos", "google photos"),
        "ফটো" to listOf("photos", "gallery"),
        "ছবি" to listOf("photos", "gallery"),
        "প্লে স্টোর" to listOf("play store", "google play store"),
        "প্লেস্টোর" to listOf("play store"),
        "ম্যাপস" to listOf("maps", "google maps"),
        "ম্যাপ" to listOf("maps"),
        "ক্যালকুলেটর" to listOf("calculator", "calc"),
        "সেটিংস" to listOf("settings"),
        "সেটিং" to listOf("settings"),
        "ঘড়ি" to listOf("clock", "alarm"),
        "ক্লক" to listOf("clock"),
        "টেলিগ্রাম" to listOf("telegram"),
        "স্পটিফাই" to listOf("spotify"),
        "ফাইল" to listOf("files", "file manager"),
        "মিউজিক" to listOf("music", "yt music", "youtube music"),
        "ফোন" to listOf("phone", "dialer"),
        "ডায়ালার" to listOf("phone", "dialer"),
        "মেসেজ" to listOf("messages", "messaging"),
        "ক্যালেন্ডার" to listOf("calendar"),
        "নোট" to listOf("notes", "keep", "google keep"),
        "বিকাশ" to listOf("bkash"),
        "নগদ" to listOf("nagad"),
        "রকেট" to listOf("rocket"),
        "উবার" to listOf("uber"),
        "পাঠাও" to listOf("pathao"),
        "দারাজ" to listOf("daraz")
    )

    @Volatile
    private var cachedApps: List<InstalledApp> = emptyList()

    suspend fun getLaunchableApps(forceRefresh: Boolean = false): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh && cachedApps.isNotEmpty()) {
                return@withContext cachedApps
            }
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            val apps = resolveInfos
                .distinctBy { it.activityInfo.packageName }
                .filter { it.activityInfo.packageName != context.packageName }
                .map { info ->
                    val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                        .ifEmpty { info.activityInfo.packageName }
                    val icon = runCatching { info.loadIcon(pm) }.getOrNull()
                    InstalledApp(
                        packageName = info.activityInfo.packageName,
                        label = label,
                        icon = icon
                    )
                }
                .sortedBy { it.label.lowercase(Locale.getDefault()) }

            cachedApps = apps
            apps
        }

    fun normalizeQuery(rawQuery: String): List<String> {
        val cleaned = rawQuery
            .lowercase(Locale.getDefault())
            .replace(Regex("[।.,!?\"']"), " ")
            .replace("মায়া", "")
            .replace("maya", "")
            .replace("অ্যাপ", "")
            .replace("app", "")
            .replace("আমার", "")
            .replace("খোলো", "")
            .replace("খুলে দাও", "")
            .replace("খুলো", "")
            .replace("চালু করো", "")
            .replace("ওপেন করো", "")
            .replace("ওপেন", "")
            .replace("open", "")
            .replace("launch", "")
            .replace("start", "")
            .trim()

        if (cleaned.isBlank()) return emptyList()

        val candidates = linkedSetOf(cleaned)
        bengaliToEnglishAliases.forEach { (bn, enList) ->
            if (cleaned == bn || cleaned.contains(bn)) {
                candidates.addAll(enList)
            }
        }
        return candidates.toList()
    }

    suspend fun searchApps(query: String): List<InstalledApp> {
        val all = getLaunchableApps()
        val terms = normalizeQuery(query)
        if (terms.isEmpty()) return all
        return all.filter { app ->
            val labelLower = app.label.lowercase(Locale.getDefault())
            val pkgLower = app.packageName.lowercase(Locale.getDefault())
            terms.any { term ->
                labelLower.contains(term) || pkgLower.contains(term) || similarity(labelLower, term) >= 0.72
            }
        }
    }

    suspend fun findAndLaunch(query: String): AppLaunchOutcome {
        val allApps = getLaunchableApps()
        val terms = normalizeQuery(query)
        if (terms.isEmpty()) {
            return AppLaunchOutcome.NotInstalled(query)
        }

        // 1. Exact label match
        for (term in terms) {
            val exact = allApps.filter { it.label.equals(term, ignoreCase = true) }
            if (exact.size == 1) return launchPackage(exact.first())
            if (exact.size > 1) return AppLaunchOutcome.Ambiguous(query, exact)
        }

        // 2. Starts-with or token match
        val tokenMatches = allApps.filter { app ->
            val lower = app.label.lowercase(Locale.getDefault())
            val pkg = app.packageName.lowercase(Locale.getDefault())
            terms.any { term ->
                lower.startsWith(term) ||
                    lower.split(" ", "-", "_").any { it == term } ||
                    pkg.endsWith(".$term")
            }
        }
        if (tokenMatches.size == 1) return launchPackage(tokenMatches.first())
        if (tokenMatches.size > 1) return AppLaunchOutcome.Ambiguous(query, tokenMatches.take(5))

        // 3. Contains / fuzzy similarity
        val fuzzyMatches = allApps.mapNotNull { app ->
            val lower = app.label.lowercase(Locale.getDefault())
            val bestScore = terms.maxOf { term ->
                when {
                    lower.contains(term) -> 0.9
                    app.packageName.lowercase(Locale.getDefault()).contains(term) -> 0.8
                    else -> similarity(lower, term)
                }
            }
            if (bestScore >= 0.68) app to bestScore else null
        }.sortedByDescending { it.second }.map { it.first }

        return when {
            fuzzyMatches.isEmpty() -> AppLaunchOutcome.NotInstalled(query.trim())
            fuzzyMatches.size == 1 -> launchPackage(fuzzyMatches.first())
            else -> AppLaunchOutcome.Ambiguous(query.trim(), fuzzyMatches.take(5))
        }
    }

    fun launchPackage(packageName: String): AppLaunchOutcome {
        val existing = cachedApps.firstOrNull { it.packageName == packageName }
            ?: InstalledApp(packageName, packageName, null)
        return launchPackage(existing)
    }

    fun launchPackage(app: InstalledApp): AppLaunchOutcome {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent == null) {
                AppLaunchOutcome.NotInstalled(app.label)
            } else {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                AppLaunchOutcome.Launched(app)
            }
        } catch (e: Exception) {
            AppLaunchOutcome.Failed(
                appLabel = app.label,
                reason = e.localizedMessage ?: "অ্যাপ চালু করা যায়নি"
            )
        }
    }

    private fun similarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val maxLen = maxOf(a.length, b.length)
        val dist = levenshtein(a, b)
        return 1.0 - (dist.toDouble() / maxLen.toDouble())
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLen = lhs.length
        val rhsLen = rhs.length
        var cost = IntArray(lhsLen + 1) { it }
        var newCost = IntArray(lhsLen + 1)
        for (i in 1..rhsLen) {
            newCost[0] = i
            for (j in 1..lhsLen) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                newCost[j] = minOf(cost[j] + 1, newCost[j - 1] + 1, cost[j - 1] + match)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLen]
    }
}
