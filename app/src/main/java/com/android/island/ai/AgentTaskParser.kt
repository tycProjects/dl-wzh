package com.android.island.ai

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Deterministic fast path for a narrow, unambiguous task shape. It skips the initial planning
 * request, but the agent still observes the search-results screen before choosing the next step.
 */
object AgentTaskParser {
    data class AppSearchPlan(val app: String, val query: String, val openFirstResult: Boolean = false)

    private val connector = Regex("\\s+(?:dan setelah itu|setelah itu|lalu|kemudian|terus|dan)\\s+|[,;]\\s*", RegexOption.IGNORE_CASE)
    private val openClause = Regex("(?i)^(?:buka(?:kan|in)?|open|launch|jalankan)\\s+(?:aplikasi\\s+)?(.+)\$")
    private val searchClause = Regex("(?i)^(?:cari(?:kan|in)?|search(?:\\s+for)?)\\s+(.+)\$")
    private val firstResultClause = Regex("(?i)^(?:klik|click|tap|pilih|buka)\\s+(?:(?:video|hasil|result)\\s+(?:(?:yang|yg)\\s+)?(?:pertama|muncul|ditampilkan|teratas)(?:\\s+.*)?|(?:yang|yg)\\s+pertama.*|pertama.*)\$")

    fun parse(text: String): AppSearchPlan? {
        if (text.length !in 12..300) return null
        val clauses = text.trim().split(connector).map { it.trim().trimEnd('.', '!', '?', ',') }
        if (clauses.size !in 3..5) return null
        val first = clauses.first().replace(Regex("(?i)^(?:tolong|mohon|coba|please|pls)\\s+"), "")
        val app = openClause.matchEntire(first)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (app.isBlank() || app.length > 50) return null
        val query = searchClause.matchEntire(clauses[1])?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (query.isBlank() || query.length > 100) return null

        var sawEnter = false
        var sawFirstResult = false
        for (clause in clauses.drop(2)) {
            val lower = clause.lowercase(Locale.ROOT).trim()
            when {
                lower in setOf("enter", "tekan enter", "masukkan", "submit") -> {
                    if (sawEnter) return null
                    sawEnter = true
                }
                firstResultClause.matches(clause) -> {
                    if (sawFirstResult) return null
                    sawFirstResult = true
                }
                else -> return null
            }
        }
        // STAGE 72: "buka X lalu cari Y lalu enter" is the same deterministic shape even without a
        // first-result clause. searchInApp types the query AND submits it (Enter included), so the
        // plan is valid whenever the user either asked for Enter or for the first result. Requiring
        // the first-result clause here pushed exactly these commands into the slow click-"Cari"
        // path, where the model tapped the wrong search affordance and the loop guard aborted.
        // The plan records whether a first result was actually requested, so
        // requestsFirstResult() no longer needs to treat every parseable command as one.
        if (!sawEnter && !sawFirstResult) return null
        return AppSearchPlan(app, query, sawFirstResult)
    }

    fun firstActionEnvelope(plan: AppSearchPlan): String = JSONObject()
        .put("reply", "")
        .put("actions", JSONArray().put(
            JSONObject().put("type", "search_in_app")
                .put("app", plan.app)
                .put("query", plan.query)
        ))
        .put("done", false)
        .toString()

    fun requestsFirstResult(text: String): Boolean =
        Regex("(?i)\\b(?:klik|click|tap|pilih|buka)\\s+(?:video|hasil|result)\\b.*\\b(?:pertama|first|muncul|yg|yang)\\b")
            .containsMatchIn(text) || parse(text)?.openFirstResult == true

    fun isGenericSearchLabel(label: String): Boolean {
        val value = label.trim().lowercase(Locale.ROOT)
        return value in setOf("cari", "search", "pencarian", "search button", "tombol pencarian") ||
            value.startsWith("pencarian ·") || value.startsWith("search ·")
    }
}
