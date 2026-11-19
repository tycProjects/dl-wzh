package com.xkeyboard.app

import android.content.Context

class Predictor(ctx: Context) {
    private val p = ctx.getSharedPreferences("xkb_learn", Context.MODE_PRIVATE)
    private val baseEn = BASE_EN.trim().split(Regex("\s+"))
    private val baseBn = BASE_BN.trim().split(Regex("\s+"))

    fun complete(prefix: String, lang: String = "en"): List<String> {
        if (prefix.isEmpty()) return emptyList()
        val lp = prefix.lowercase()
        val base = if (lang == "bn") baseBn else baseEn
        val score = HashMap<String, Int>()
        base.forEachIndexed { i, w ->
            if (w.length > lp.length && w.startsWith(lp)) score[w] = 1000 - i
        }
        for ((k, v) in p.all) {
            if (k.startsWith("w_") && v is Int) {
                val w = k.substring(2)
                if (w.length > lp.length && w.startsWith(lp)) score[w] = (score[w] ?: 0) + 400 + v * 100
            }
        }
        return score.entries.sortedByDescending { it.value }.take(3).map {
            if (lang == "bn") it.key else shape(it.key, prefix)
        }
    }

    fun next(prev: String, lang: String = "en"): List<String> {
        val out = ArrayList<String>()
        if (prev.isNotEmpty()) {
            val pre = "b_${prev}_"
            p.all.entries
                .filter { it.key.startsWith(pre) && it.value is Int }
                .sortedByDescending { it.value as Int }
                .take(3)
                .forEach { out.add(it.key.substring(pre.length)) }
        }
        val fill = if (lang == "bn") {
            if (prev.isEmpty()) listOf("আমি", "কেমন", "ধন্যবাদ") else listOf("আছেন", "ভালো", "করি")
        } else {
            if (prev.isEmpty()) listOf("I", "The", "Hello") else listOf("the", "and", "to")
        }
        for (d in fill) if (out.size < 3 && d !in out) out.add(d)
        return out
    }

    fun learn(word: String, prev: String) {
        val w = word.lowercase()
        if (w.length < 2) return
        if (p.all.size > 5000) return
        val e = p.edit()
        e.putInt("w_$w", p.getInt("w_$w", 0) + 1)
        if (prev.isNotEmpty()) e.putInt("b_${prev}_$w", p.getInt("b_${prev}_$w", 0) + 1)
        e.apply()
    }

    private fun shape(w: String, prefix: String): String = when {
        prefix.length > 1 && prefix.all { it.isUpperCase() } -> w.uppercase()
        prefix.first().isUpperCase() -> w.replaceFirstChar { it.uppercase() }
        else -> w
    }

    companion object {
        private const val BASE_EN = """
the be to of and a in that have it for not on with he as you do at this but his by from they we say her she
or an will my one all would there their what so up out if about who get which go me when make can like time
no just him know take people into year your good some could them see other than then now look only come its
over think also back after use two how our work first well way even new want because any these give day most us
hello hi hey thanks thank please sorry yes okay ok love great right really going little still thing things need
feel today tomorrow tonight morning night friend friends home school phone video game games minecraft keyboard
message call send happy should where why very much more here there been being before between both each few
beautiful awesome amazing cool nice best better last long little big small old young next early late
"""
        private const val BASE_BN = """
আমি তুমি আপনি সে আমরা তারা এই সেই কি কী কেন কিভাবে কোথায় কখন আজ কাল এখন
ভালো খারাপ সুন্দর ধন্যবাদ স্বাগতম দুঃখিত হ্যাঁ না ঠিক আছে কেমন আছেন আছি ভালোবাসি
বাংলা বাংলাদেশ ঢাকা সিলেট চট্টগ্রাম পরিবার বন্ধু বাড়ি স্কুল কলেজ কাজ সময় ফোন মেসেজ
"""
    }
}
