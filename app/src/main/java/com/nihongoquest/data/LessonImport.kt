package com.nihongoquest.data

import org.json.JSONObject

/**
 * Portable lesson format used by NIHONGO QUEST.
 * Files use the .nqlesson extension and contain UTF-8 JSON.
 */
data class LessonImportItem(
    val japanese: String,
    val reading: String = "",
    val romaji: String = "",
    val meaning: String,
    val example: String = "",
    val exampleMeaning: String = "",
    val jlpt: String = "N5",
    val category: String = "General",
    val notes: String = ""
)

data class LessonImport(
    val title: String = "",
    val description: String = "",
    val items: List<LessonImportItem>
)

fun parseLessonFile(source: String): LessonImport {
    val root = JSONObject(source)
    require(root.optString("format") == "nihongo-quest-lesson") {
        "This file is not a NIHONGO QUEST lesson file."
    }
    require(root.optInt("schemaVersion", 0) == 1) {
        "This lesson file uses an unsupported format version."
    }
    val array = root.optJSONArray("items")
        ?: throw IllegalArgumentException("The lesson file has no vocabulary list.")
    val items = buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val japanese = item.optString("japanese").trim()
            val meaning = item.optString("meaning").trim()
            if (japanese.isNotEmpty() && meaning.isNotEmpty()) {
                add(
                    LessonImportItem(
                        japanese = japanese,
                        reading = item.optString("reading"),
                        romaji = item.optString("romaji"),
                        meaning = meaning,
                        example = item.optString("example"),
                        exampleMeaning = item.optString("exampleMeaning"),
                        jlpt = item.optString("jlpt", "N5"),
                        category = item.optString("category", "General"),
                        notes = item.optString("notes")
                    )
                )
            }
        }
    }
    require(items.isNotEmpty()) {
        "No valid vocabulary entries were found in this file."
    }
    return LessonImport(
        title = root.optString("title"),
        description = root.optString("description"),
        items = items
    )
}
