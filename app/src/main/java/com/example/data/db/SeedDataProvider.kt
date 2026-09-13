package com.example.data.db

import android.content.Context
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ExamEntity
import com.example.data.entity.NoteDocumentEntity
import com.example.data.entity.SubjectEntity
import com.example.data.entity.UserPreferencesEntity
import com.example.model.CalloutType
import com.example.model.DiagramType
import com.example.model.DocElement
import com.example.model.ElementType
import com.example.model.PrepDocument
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader

object SeedDataProvider {

    /**
     * Seeds or updates database with syllabus:
     * 1. If empty, populates all data from uploaded JSON or asset.
     * 2. If database already has data, checks whether the syllabus in the data file was updated
     *    (by comparing fingerprint/chapter count) and seamlessly updates chapters, subjects, and notes
     *    while strictly preserving user reading progress, test attempts, streaks, and custom settings.
     */
    suspend fun populateInitialData(db: PrepOSDatabase, context: Context? = null) {
        syncOrUpdateSyllabus(db, context, force = false)
    }

    suspend fun syncOrUpdateSyllabus(db: PrepOSDatabase, context: Context? = null, force: Boolean = false): Boolean {
        val subjectDao = db.subjectDao()
        val chapterDao = db.chapterDao()
        val currentSubjectCount = subjectDao.getSubjectCount()
        val currentChapterCount = chapterDao.getChapterCount()

        val jsonPayload = tryLoadUploadedBackupJson(context)
        if (jsonPayload.isNullOrBlank()) {
            if (currentSubjectCount == 0) {
                populateFallbackData(db)
            }
            return false
        }

        val root = try { JSONObject(jsonPayload) } catch (_: Exception) { null } ?: return false
        val incomingChapters = root.optJSONArray("chapters") ?: JSONArray()
        val incomingSubjects = root.optJSONArray("subjects") ?: JSONArray()
        val incomingTimestamp = root.optLong("timestamp", 0L)
        val fingerprint = "${incomingTimestamp}_${incomingChapters.length()}_${incomingSubjects.length()}"

        val prefs = context?.getSharedPreferences("prepos_syllabus_meta", Context.MODE_PRIVATE)
        val lastSyncedFingerprint = prefs?.getString("last_syllabus_fingerprint", "") ?: ""

        val needsSync = force ||
                currentSubjectCount == 0 ||
                currentChapterCount < incomingChapters.length() ||
                lastSyncedFingerprint != fingerprint

        if (!needsSync) {
            return true
        }

        val success = populateFromJson(db, jsonPayload, preserveProgress = (currentSubjectCount > 0 && !force))
        if (success) {
            prefs?.edit()?.putString("last_syllabus_fingerprint", fingerprint)?.apply()
        }
        return success
    }

    private fun tryLoadUploadedBackupJson(context: Context?): String? {
        // Priority 1: From assets/default_jkssb_content.json
        try {
            if (context != null) {
                context.assets.open("default_jkssb_content.json").use { stream ->
                    val text = InputStreamReader(stream).readText()
                    if (text.isNotBlank()) return text
                }
            }
        } catch (_: Exception) {}

        // Priority 2: From assets/prepos_backup.json
        try {
            if (context != null) {
                context.assets.open("prepos_backup.json").use { stream ->
                    val text = InputStreamReader(stream).readText()
                    if (text.isNotBlank()) return text
                }
            }
        } catch (_: Exception) {}

        // Priority 3: Uploaded file in workspace root: prepos_backup[1].json
        try {
            val f = File("prepos_backup[1].json")
            if (f.exists() && f.length() > 0) {
                return f.readText()
            }
        } catch (_: Exception) {}

        // Priority 4: Direct root prepos_backup.json
        try {
            val rootFile = File("prepos_backup.json")
            if (rootFile.exists() && rootFile.length() > 0) {
                return rootFile.readText()
            }
        } catch (_: Exception) {}

        try {
            val assetFile = File("app/src/main/assets/default_jkssb_content.json")
            if (assetFile.exists() && assetFile.length() > 0) {
                return assetFile.readText()
            }
        } catch (_: Exception) {}

        return null
    }

    suspend fun populateFromJson(db: PrepOSDatabase, json: String, preserveProgress: Boolean = false): Boolean {
        return try {
            val root = JSONObject(json)
            val examDao = db.examDao()
            val subjectDao = db.subjectDao()
            val chapterDao = db.chapterDao()
            val docDao = db.noteDocumentDao()
            val prefsDao = db.userPreferencesDao()

            val existingPrefs = prefsDao.getPreferences()
            val prefsObj = root.optJSONObject("preferences")
            if (existingPrefs == null || !preserveProgress) {
                if (prefsObj != null) {
                    prefsDao.savePreferences(
                        UserPreferencesEntity(
                            key = "global_prefs",
                            defaultPaperStyle = prefsObj.optString("defaultPaperStyle", "RULED"),
                            defaultFontFamily = prefsObj.optString("defaultFontFamily", "SANS_SERIF"),
                            defaultFontSizeSp = prefsObj.optDouble("defaultFontSizeSp", 16.0).toFloat(),
                            defaultThemeMode = prefsObj.optString("defaultThemeMode", "PAPER_LIGHT"),
                            defaultLineSpacing = prefsObj.optDouble("defaultLineSpacing", 1.4).toFloat(),
                            targetExamName = prefsObj.optString("targetExamName", "JKSSB Constable"),
                            targetExamDate = prefsObj.optString("targetExamDate", "2026-10-25"),
                            targetDailyStudyHours = prefsObj.optDouble("targetDailyStudyHours", 2.5).toFloat(),
                            targetScoreGoal = prefsObj.optInt("targetScoreGoal", 85)
                        )
                    )
                } else if (existingPrefs == null) {
                    prefsDao.savePreferences(
                        UserPreferencesEntity(
                            key = "global_prefs",
                            defaultPaperStyle = "RULED",
                            defaultFontFamily = "SANS_SERIF",
                            defaultFontSizeSp = 16f,
                            defaultThemeMode = "PAPER_LIGHT",
                            defaultLineSpacing = 1.4f,
                            targetExamName = "JKSSB Constable",
                            targetExamDate = "2026-10-25",
                            targetDailyStudyHours = 2.5f,
                            targetScoreGoal = 85
                        )
                    )
                }
            }

            val examsArr = root.optJSONArray("exams") ?: JSONArray()
            for (i in 0 until examsArr.length()) {
                val obj = examsArr.getJSONObject(i)
                examDao.insertExam(
                    ExamEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        code = obj.optString("code", ""),
                        colorHex = obj.optString("colorHex", "#059669"),
                        orderIndex = obj.optInt("orderIndex", i)
                    )
                )
            }

            val subArr = root.optJSONArray("subjects") ?: JSONArray()
            val existingSubjectIds = mutableSetOf<String>()
            for (i in 0 until subArr.length()) {
                val obj = subArr.getJSONObject(i)
                val subId = obj.getString("id")
                existingSubjectIds.add(subId)
                val examId = obj.optString("examId").takeIf { it.isNotBlank() }
                subjectDao.insertSubject(
                    SubjectEntity(
                        id = subId,
                        examId = examId,
                        name = obj.getString("name"),
                        iconName = obj.optString("iconName", "menu_book"),
                        colorHex = obj.optString("colorHex", "#3B82F6"),
                        orderIndex = obj.optInt("orderIndex", i)
                    )
                )
            }

            // Ensure auxiliary subjects exist if referenced by any chapters
            if (!existingSubjectIds.contains("sub_os")) {
                subjectDao.insertSubject(
                    SubjectEntity(
                        id = "sub_os",
                        examId = "exam_gate",
                        name = "Operating Systems",
                        iconName = "memory",
                        colorHex = "#7C3AED",
                        orderIndex = 10
                    )
                )
            }
            if (!existingSubjectIds.contains("sub_general_notes")) {
                subjectDao.insertSubject(
                    SubjectEntity(
                        id = "sub_general_notes",
                        examId = "exam_ssc_cgl",
                        name = "General Science",
                        iconName = "science",
                        colorHex = "#2563EB",
                        orderIndex = 11
                    )
                )
            }

            val chapArr = root.optJSONArray("chapters") ?: JSONArray()
            for (i in 0 until chapArr.length()) {
                val obj = chapArr.getJSONObject(i)
                val chapId = obj.getString("id")
                val rawSubjectId = obj.getString("subjectId")
                // Map legacy or re-generated subject IDs
                val mappedSubjectId = when (rawSubjectId) {
                    "sub_e672c511" -> "sub_acfcac11" // Indian History & Physics -> General Knowledge
                    "sub_reasoning" -> "sub_4956539c" // Legacy reasoning -> Reasoning Ability
                    else -> rawSubjectId
                }

                if (preserveProgress) {
                    val existing = chapterDao.getChapterById(chapId)
                    if (existing != null) {
                        chapterDao.insertChapter(
                            existing.copy(
                                subjectId = mappedSubjectId,
                                title = obj.getString("title"),
                                chapterNumber = obj.optInt("chapterNumber", existing.chapterNumber),
                                summary = obj.optString("summary", existing.summary),
                                questionsJson = obj.optString("questionsJson", existing.questionsJson),
                                orderIndex = obj.optInt("orderIndex", existing.orderIndex)
                            )
                        )
                    } else {
                        chapterDao.insertChapter(
                            ChapterEntity(
                                id = chapId,
                                subjectId = mappedSubjectId,
                                title = obj.getString("title"),
                                chapterNumber = obj.optInt("chapterNumber", 0),
                                summary = obj.optString("summary", ""),
                                questionsJson = obj.optString("questionsJson", "[]"),
                                orderIndex = obj.optInt("orderIndex", i),
                                readingProgress = obj.optDouble("readingProgress", 0.0).toFloat(),
                                lastReadScrollY = obj.optInt("lastReadScrollY", 0),
                                lastModified = obj.optLong("lastModified", System.currentTimeMillis())
                            )
                        )
                    }
                } else {
                    chapterDao.insertChapter(
                        ChapterEntity(
                            id = chapId,
                            subjectId = mappedSubjectId,
                            title = obj.getString("title"),
                            chapterNumber = obj.optInt("chapterNumber", 0),
                            summary = obj.optString("summary", ""),
                            questionsJson = obj.optString("questionsJson", "[]"),
                            orderIndex = obj.optInt("orderIndex", i),
                            readingProgress = obj.optDouble("readingProgress", 0.0).toFloat(),
                            lastReadScrollY = obj.optInt("lastReadScrollY", 0),
                            lastModified = obj.optLong("lastModified", System.currentTimeMillis())
                        )
                    )
                }
            }

            val docArr = root.optJSONArray("documents") ?: JSONArray()
            for (i in 0 until docArr.length()) {
                val obj = docArr.getJSONObject(i)
                docDao.insertOrUpdate(
                    NoteDocumentEntity(
                        chapterId = obj.getString("chapterId"),
                        contentJson = obj.getString("contentJson"),
                        paperStyle = obj.optString("paperStyle", "RULED"),
                        fontFamily = obj.optString("fontFamily", "SANS_SERIF"),
                        fontSizeSp = obj.optDouble("fontSizeSp", 16.0).toFloat(),
                        lineSpacingMultiplier = obj.optDouble("lineSpacingMultiplier", 1.4).toFloat(),
                        themeMode = obj.optString("themeMode", "WARM_SEPIA")
                    )
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun populateFallbackData(db: PrepOSDatabase) {
        val examDao = db.examDao()
        val subjectDao = db.subjectDao()
        val chapterDao = db.chapterDao()
        val docDao = db.noteDocumentDao()
        val prefsDao = db.userPreferencesDao()

        prefsDao.savePreferences(
            UserPreferencesEntity(
                key = "global_prefs",
                defaultPaperStyle = "RULED",
                defaultFontFamily = "SANS_SERIF",
                defaultFontSizeSp = 16f,
                defaultThemeMode = "PAPER_LIGHT",
                defaultLineSpacing = 1.4f,
                targetExamName = "JKSSB Constable",
                targetExamDate = "2026-10-25",
                targetDailyStudyHours = 2.5f,
                targetScoreGoal = 85
            )
        )

        val examJkssb = ExamEntity(
            id = "exam_7e6b57e3",
            name = "JKSSB Constable",
            code = "JKP",
            colorHex = "#059669",
            orderIndex = 0
        )
        examDao.insertExam(examJkssb)

        val subEnglish = SubjectEntity(
            id = "sub_5282834a",
            examId = "exam_7e6b57e3",
            name = "English",
            iconName = "menu_book",
            colorHex = "#EC4899",
            orderIndex = 0
        )
        val subComputer = SubjectEntity(
            id = "sub_b8bae1ce",
            examId = "exam_7e6b57e3",
            name = "Computer",
            iconName = "memory",
            colorHex = "#3B82F6",
            orderIndex = 1
        )
        val subGk = SubjectEntity(
            id = "sub_8776e497",
            examId = "exam_7e6b57e3",
            name = "GK J&k",
            iconName = "psychology",
            colorHex = "#10B981",
            orderIndex = 2
        )
        subjectDao.insertSubject(subEnglish)
        subjectDao.insertSubject(subComputer)
        subjectDao.insertSubject(subGk)
    }
}
