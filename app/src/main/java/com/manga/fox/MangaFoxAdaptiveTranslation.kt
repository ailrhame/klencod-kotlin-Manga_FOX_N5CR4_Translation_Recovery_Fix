package com.manga.fox

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * N5CR4 adaptive translation policy.
 * Classifies each OCR segment locally; no image or text is uploaded by this helper.
 */
internal object MangaFoxAdaptiveTranslation {
    enum class Kind(val wireName: String, val instruction: String) {
        DIALOGUE("dialogue", "حوار: حافظ على صوت المتحدث، الضمائر، الإيجاز الطبيعي، وعلامات الانفعال."),
        NARRATION("narration", "سرد أو مربع معلومات: استخدم صياغة عربية فصيحة مترابطة ولا تحوله إلى حوار."),
        SFX("sfx", "مؤثر صوتي: صغه بكلمة أو عبارة عربية قصيرة تعبّر عن الصوت أو الحركة، ولا تترك حروف المصدر."),
        SHORT("short", "نص قصير: لا تسقط الحروف أو التعجبات؛ انقل الاسم علماً صوتياً إذا كان اسماً."),
        MIXED("mixed", "نص مختلط: حافظ على الجزء العربي كما هو، وترجم الجزء الأجنبي فقط مع إبقاء المعنى متصلاً.")
    }

    data class Classification(val kind: Kind, val confidence: Float, val reason: String)

    fun classify(text: String): Classification {
        val value = text.trim()
        if (value.isEmpty()) return Classification(Kind.SHORT, 0f, "empty")
        val letters = value.count { it.isLetter() }.coerceAtLeast(1)
        val arabic = value.count { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' || it in '\u08A0'..'\u08FF' }
        val latin = value.count { it in 'A'..'Z' || it in 'a'..'z' }
        val cjk = value.count { it in '\u3040'..'\u30FF' || it in '\u4E00'..'\u9FFF' || it in '\uAC00'..'\uD7AF' }
        val punctuation = value.count { it in "!！?？…~～♪♫★☆" }
        val words = value.split(Regex("\\s+")).filter { it.isNotBlank() }
        val upperRatio = value.count { it.isUpperCase() && it.isLetter() }.toFloat() / letters
        return when {
            arabic > 0 && (latin + cjk) > 0 ->
                Classification(Kind.MIXED, 0.94f, "mixed_scripts")
            value.length <= 7 || (words.size <= 2 && punctuation > 0) ->
                Classification(Kind.SHORT, 0.78f, "short_segment")
            punctuation >= 2 && words.size <= 4 && (upperRatio > 0.45f || value.length <= 24) ->
                Classification(Kind.SFX, 0.72f, "expressive_short")
            words.size >= 7 || value.length >= 70 ->
                Classification(Kind.NARRATION, 0.68f, "long_segment")
            else -> Classification(Kind.DIALOGUE, 0.64f, "default_dialogue")
        }
    }

    fun hint(text: String): String = classify(text).let { "${it.kind.instruction} (تصنيف ${it.kind.wireName})" }
}

/** Local index of character appearances per work/chapter; bounded and safe against unbounded growth. */
internal object MangaFoxCharacterTracker {
    private const val PREFS = "mfox_character_chapter_index_v1"
    private const val KEY = "records"

    @Synchronized
    fun record(context: Context, work: String, chapter: String, sourceText: String, knownNames: List<String>) {
        if (knownNames.isEmpty() || sourceText.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val root = runCatching { JSONArray(prefs.getString(KEY, "[]")) }.getOrElse { JSONArray() }
        val safeWork = work.ifBlank { "عمل غير مصنف" }.take(100)
        val safeChapter = chapter.ifBlank { "فصل غير مسمى" }.take(100)
        knownNames.distinct().filter { name ->
            name.length >= 2 && Regex("(?i)(?<![\\p{L}\\p{N}])${Regex.escape(name)}(?![\\p{L}\\p{N}])")
                .containsMatchIn(sourceText)
        }.forEach { name ->
            val existing = (0 until root.length()).mapNotNull { root.optJSONObject(it) }
                .firstOrNull { it.optString("work") == safeWork && it.optString("chapter") == safeChapter && it.optString("name").equals(name, true) }
            if (existing != null) existing.put("mentions", existing.optInt("mentions") + 1)
            else root.put(JSONObject().put("work", safeWork).put("chapter", safeChapter).put("name", name.take(100)).put("mentions", 1))
        }
        val trimmed = JSONArray()
        val start = (root.length() - 1500).coerceAtLeast(0)
        for (i in start until root.length()) trimmed.put(root.getJSONObject(i))
        prefs.edit().putString(KEY, trimmed.toString()).apply()
    }

    fun summary(context: Context, work: String? = null): String {
        val root = runCatching { JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]")) }
            .getOrElse { JSONArray() }
        val rows = (0 until root.length()).mapNotNull { root.optJSONObject(it) }
            .filter { work.isNullOrBlank() || it.optString("work") == work }
        if (rows.isEmpty()) return "لا توجد ارتباطات فصول مسجلة بعد. ستُسجل الإشارات عندما يظهر اسم محفوظ في نص OCR."
        return rows.groupBy { it.optString("name") }.entries
            .sortedByDescending { entry -> entry.value.sumOf { it.optInt("mentions") } }
            .take(30).joinToString("\n") { (name, entries) ->
                val chapters = entries.map { it.optString("chapter") }.distinct().take(8).joinToString("، ")
                "• $name — ${entries.sumOf { it.optInt("mentions") }} إشارة | $chapters"
            }
    }
}
