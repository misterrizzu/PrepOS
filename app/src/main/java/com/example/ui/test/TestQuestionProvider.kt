package com.example.ui.test

import android.content.Context
import com.example.data.entity.ChapterEntity
import com.example.data.entity.SubjectEntity
import com.example.data.entity.TestAttemptEntity
import com.example.model.QuestionItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object TestQuestionProvider {

    /**
     * Fallback high-yield questions for general competitive exams (UPSC, State PSC, SSC, JEE, NEET, Banking, etc.)
     */
    private val DEFAULT_QUESTION_POOL = listOf(
        QuestionItem(
            id = "q_polity_1",
            questionText = "Which Article of the Indian Constitution empowers the Supreme Court to issue writs for the enforcement of Fundamental Rights?",
            options = listOf("Article 32", "Article 226", "Article 136", "Article 143"),
            correctOptionIndex = 0,
            explanation = "Article 32 provides the right to constitutional remedies and empowers the Supreme Court to issue writs (Habeas Corpus, Mandamus, Prohibition, Quo-Warranto, and Certiorari). Dr. B.R. Ambedkar called Article 32 the 'Heart and Soul of the Constitution'.",
            difficulty = "EASY",
            tags = listOf("Polity", "Fundamental Rights", "Constitution"),
            examSource = "Polity & Governance"
        ),
        QuestionItem(
            id = "q_polity_2",
            questionText = "The Directive Principles of State Policy (DPSP) in the Constitution of India were borrowed from which country's constitution?",
            options = listOf("Irish Constitution", "US Constitution", "British Constitution", "Canadian Constitution"),
            correctOptionIndex = 0,
            explanation = "The Directive Principles of State Policy (Part IV, Articles 36–51) were borrowed from the Irish Constitution of 1937, which in turn had borrowed them from the Spanish Constitution.",
            difficulty = "EASY",
            tags = listOf("Polity", "DPSP", "Sources"),
            examSource = "Indian Polity"
        ),
        QuestionItem(
            id = "q_polity_3",
            questionText = "Under which Constitutional Amendment Act was the Right to Education (Article 21A) inserted into Fundamental Rights?",
            options = listOf("86th Amendment Act, 2002", "44th Amendment Act, 1978", "42nd Amendment Act, 1976", "91st Amendment Act, 2003"),
            correctOptionIndex = 0,
            explanation = "The 86th Constitutional Amendment Act, 2002 inserted Article 21A, making free and compulsory education for children between the ages of 6 and 14 a Fundamental Right.",
            difficulty = "MEDIUM",
            tags = listOf("Polity", "Amendments", "Education"),
            examSource = "Indian Polity"
        ),
        QuestionItem(
            id = "q_history_1",
            questionText = "Who presided over the historic Lahore Session of the Indian National Congress in December 1929 where 'Purna Swaraj' was declared?",
            options = listOf("Jawaharlal Nehru", "Mahatma Gandhi", "Subhash Chandra Bose", "Sardar Vallabhbhai Patel"),
            correctOptionIndex = 0,
            explanation = "Jawaharlal Nehru presided over the 1929 Lahore Session of the INC, where the historic resolution of 'Purna Swaraj' (Complete Independence) was adopted, and 26 January 1930 was declared as Independence Day.",
            difficulty = "MEDIUM",
            tags = listOf("Modern History", "Freedom Struggle", "INC"),
            examSource = "Indian History"
        ),
        QuestionItem(
            id = "q_history_2",
            questionText = "The famous Indus Valley Civilization site 'Lothal', known for its ancient tidal dockyard, is located in which present-day state?",
            options = listOf("Gujarat", "Rajasthan", "Punjab", "Haryana"),
            correctOptionIndex = 0,
            explanation = "Lothal is situated near the Gulf of Khambhat in Gujarat along the Bhogava river. It had the world's earliest known dockyard connected to an ancient trade route.",
            difficulty = "EASY",
            tags = listOf("Ancient History", "Indus Valley", "Archaeology"),
            examSource = "Ancient Indian History"
        ),
        QuestionItem(
            id = "q_geo_1",
            questionText = "Which Indian river is known as the 'Dakshin Ganga' (Ganga of the South)?",
            options = listOf("Godavari", "Cauvery", "Krishna", "Mahanadi"),
            correctOptionIndex = 0,
            explanation = "The Godavari is the longest river of peninsular India (1465 km) and is frequently referred to as 'Dakshin Ganga' or 'Vridha Ganga' due to its large size and religious sanctity.",
            difficulty = "EASY",
            tags = listOf("Geography", "Indian Rivers", "Drainage"),
            examSource = "Indian Geography"
        ),
        QuestionItem(
            id = "q_geo_2",
            questionText = "The 'Ten Degree Channel' separates which of the following islands?",
            options = listOf("Andaman Islands from Nicobar Islands", "Little Andaman from South Andaman", "Lakshadweep from Maldives", "Minicoy from Maldives"),
            correctOptionIndex = 0,
            explanation = "The Ten Degree Channel is a channel that separates the Andaman Islands from the Nicobar Islands in the Bay of Bengal along the 10° N parallel.",
            difficulty = "MEDIUM",
            tags = listOf("Geography", "Islands", "Physical Features"),
            examSource = "Physical Geography"
        ),
        QuestionItem(
            id = "q_eco_1",
            questionText = "What term describes a situation in an economy where high inflation is accompanied by stagnant economic growth and high unemployment?",
            options = listOf("Stagflation", "Hyperinflation", "Deflation", "Reflation"),
            correctOptionIndex = 0,
            explanation = "Stagflation is an economic condition characterized by slow economic growth, high unemployment, and high inflation simultaneously, posing a dilemma for economic policy.",
            difficulty = "MEDIUM",
            tags = listOf("Economy", "Macroeconomics", "Inflation"),
            examSource = "Indian Economy"
        ),
        QuestionItem(
            id = "q_eco_2",
            questionText = "Who acts as the Chairman of the NITI Aayog in India?",
            options = listOf("Prime Minister of India", "Finance Minister of India", "Governor of RBI", "Vice Chairman of NITI Aayog"),
            correctOptionIndex = 0,
            explanation = "The Prime Minister of India serves as the ex-officio Chairman of the NITI Aayog (National Institution for Transforming India), which replaced the Planning Commission in 2015.",
            difficulty = "EASY",
            tags = listOf("Economy", "Planning", "NITI Aayog"),
            examSource = "Indian Economy"
        ),
        QuestionItem(
            id = "q_sci_1",
            questionText = "Which organelle is known as the 'Powerhouse of the Cell' due to its role in cellular respiration and ATP synthesis?",
            options = listOf("Mitochondria", "Ribosome", "Golgi Apparatus", "Lysosome"),
            correctOptionIndex = 0,
            explanation = "Mitochondria are membrane-bound organelles that generate most of the chemical energy needed to power biochemical reactions via adenosine triphosphate (ATP).",
            difficulty = "EASY",
            tags = listOf("General Science", "Biology", "Cell Biology"),
            examSource = "General Science"
        ),
        QuestionItem(
            id = "q_sci_2",
            questionText = "Which phenomenon explains the sparkling of a diamond and the transmission of light signals in optical fiber cables?",
            options = listOf("Total Internal Reflection", "Refraction", "Diffraction", "Polarization"),
            correctOptionIndex = 0,
            explanation = "Total Internal Reflection (TIR) occurs when the angle of incidence exceeds the critical angle. It is responsible for the sparkle in cut diamonds, optical communication in fibers, and mirages.",
            difficulty = "MEDIUM",
            tags = listOf("General Science", "Physics", "Optics"),
            examSource = "General Science"
        ),
        QuestionItem(
            id = "q_env_1",
            questionText = "The 'Ramsar Convention', signed in 1971 in Iran, is an international treaty for the conservation and sustainable use of which ecosystems?",
            options = listOf("Wetlands", "Mangroves", "Coral Reefs", "Tropical Rainforests"),
            correctOptionIndex = 0,
            explanation = "The Ramsar Convention on Wetlands of International Importance especially as Waterfowl Habitat is an international treaty for the conservation and wise use of wetlands.",
            difficulty = "MEDIUM",
            tags = listOf("Environment", "Ecology", "Conventions"),
            examSource = "Ecology & Environment"
        ),
        QuestionItem(
            id = "q_reasoning_1",
            questionText = "In a code language, if 'LEARN' is written as 'NGCTP', how will 'STUDY' be written in the same code?",
            options = listOf("UVWFZ", "UWVFZ", "TUVEY", "UVVGA"),
            correctOptionIndex = 1,
            explanation = "Pattern is +2 for each letter: L+2=N, E+2=G, A+2=C, R+2=T, N+2=P. Applying to STUDY: S+2=U, T+2=V, U+2=W, D+2=F, Y+2=A (or Y+2=A, wrapping around). Here S+2=U, T+2=V or W, following +2 per character.",
            difficulty = "MEDIUM",
            tags = listOf("Reasoning", "Coding-Decoding"),
            examSource = "General Mental Ability"
        ),
        QuestionItem(
            id = "q_quant_1",
            questionText = "A train 180 meters long crosses a platform 220 meters long in 20 seconds. What is the speed of the train in km/h?",
            options = listOf("72 km/h", "54 km/h", "90 km/h", "60 km/h"),
            correctOptionIndex = 0,
            explanation = "Total distance = Train length + Platform length = 180 + 220 = 400 meters. Time = 20 s. Speed in m/s = 400 / 20 = 20 m/s. Speed in km/h = 20 * (18 / 5) = 72 km/h.",
            difficulty = "MEDIUM",
            tags = listOf("Aptitude", "Speed Time Distance"),
            examSource = "Quantitative Aptitude"
        ),
        QuestionItem(
            id = "q_comp_1",
            questionText = "In computer networking and cybersecurity, what does the protocol acronym 'HTTPS' stand for?",
            options = listOf("Hypertext Transfer Protocol Secure", "Hyperlink Text Transmission System", "High-level Transfer Protocol Standard", "Hypertext Translation Protocol Socket"),
            correctOptionIndex = 0,
            explanation = "HTTPS stands for Hypertext Transfer Protocol Secure. It is the secure version of HTTP and uses TLS/SSL encryption for secure communication over computer networks.",
            difficulty = "EASY",
            tags = listOf("Computer Awareness", "Networking", "Security"),
            examSource = "Computer Awareness"
        )
    )

    // In-memory cache for parsed JSON questions to ensure 0ms instantaneous load
    private val parsedQuestionsCache = java.util.concurrent.ConcurrentHashMap<String, List<QuestionItem>>()

    /**
     * Extracts questions from chapters, prioritizing unattempted questions first and supporting full-chapter questions.
     */
    fun buildQuestionPool(
        chapters: List<ChapterEntity>,
        subjectId: String? = null,
        chapterId: String? = null,
        count: Int = 10,
        recentAttempts: List<TestAttemptEntity> = emptyList()
    ): List<QuestionItem> {
        val extractedQuestions = mutableListOf<QuestionItem>()

        // 1. If a specific chapter is requested
        if (!chapterId.isNullOrBlank()) {
            val chap = chapters.find { it.id == chapterId }
            if (chap != null && chap.questionsJson.isNotBlank()) {
                extractedQuestions.addAll(parseQuestionsFromJson(chap.questionsJson))
            }
            // If the chapter has its own questions and count is 0 (or default full mode), return all of them
            if (extractedQuestions.isNotEmpty() && count <= 0) {
                // If attempts exist, prioritize unattempted in the chapter
                val attemptedTexts = recentAttempts.flatMap { attempt ->
                    parseQuestionsFromJson(attempt.questionsJson).map { it.questionText.trim().lowercase() }
                }.toSet()
                val (unattempted, attempted) = extractedQuestions.partition { it.questionText.trim().lowercase() !in attemptedTexts }
                return unattempted.shuffled() + attempted.shuffled()
            }
        } else if (!subjectId.isNullOrBlank()) {
            // 2. If a specific subject is requested
            val subjectChapters = chapters.filter { it.subjectId == subjectId }
            subjectChapters.forEach { chap ->
                if (chap.questionsJson.isNotBlank()) {
                    extractedQuestions.addAll(parseQuestionsFromJson(chap.questionsJson))
                }
            }
        } else {
            // 3. All chapters across all subjects
            chapters.forEach { chap ->
                if (chap.questionsJson.isNotBlank()) {
                    extractedQuestions.addAll(parseQuestionsFromJson(chap.questionsJson))
                }
            }
        }

        val targetCount = if (count <= 0) {
            if (extractedQuestions.isNotEmpty()) extractedQuestions.size else 10
        } else {
            count
        }

        // 4. Fallback / supplement with default rich question pool if needed
        if (extractedQuestions.size < targetCount) {
            val needed = targetCount - extractedQuestions.size
            val defaultShuffled = DEFAULT_QUESTION_POOL.shuffled()
            val toAdd = defaultShuffled.filter { defQ ->
                extractedQuestions.none { it.questionText.trim().equals(defQ.questionText.trim(), ignoreCase = true) }
            }.take(needed)
            extractedQuestions.addAll(toAdd)
        }

        // 5. Prioritize Unattempted Questions First
        val attemptedTexts = recentAttempts.flatMap { attempt ->
            parseQuestionsFromJson(attempt.questionsJson).map { it.questionText.trim().lowercase() }
        }.toSet()

        val (unattempted, attempted) = extractedQuestions.distinctBy { it.questionText.trim().lowercase() }
            .partition { it.questionText.trim().lowercase() !in attemptedTexts }

        val prioritized = (unattempted.shuffled() + attempted.shuffled())
        return if (count > 0) prioritized.take(count) else prioritized
    }

    /**
     * Builds a Rapid Fire Quiz (10 quick questions with unattempted priority).
     */
    fun buildRapidFireQuiz(
        chapters: List<ChapterEntity>,
        recentAttempts: List<TestAttemptEntity> = emptyList()
    ): List<QuestionItem> {
        return buildQuestionPool(chapters = chapters, count = 10, recentAttempts = recentAttempts)
    }

    /**
     * Builds a Full Mock Test (25 comprehensive questions with unattempted priority).
     */
    fun buildFullMockTest(
        chapters: List<ChapterEntity>,
        recentAttempts: List<TestAttemptEntity> = emptyList()
    ): List<QuestionItem> {
        return buildQuestionPool(chapters = chapters, count = 25, recentAttempts = recentAttempts)
    }

    /**
     * Extracts all unique questions from user's test attempts that were answered INCORRECTLY.
     */
    fun getMistakeQuestionsPool(recentAttempts: List<TestAttemptEntity>): List<QuestionItem> {
        val mistakes = mutableListOf<QuestionItem>()
        val seenKeys = mutableSetOf<String>()

        // Scan attempts from newest to oldest
        recentAttempts.sortedByDescending { it.completedAt }.forEach { attempt ->
            val questions = parseQuestionsFromJson(attempt.questionsJson)
            questions.forEach { q ->
                val userAns = q.userSelectedOptionIndex
                val key = q.questionText.trim().lowercase()
                
                // Explicit wrong answer check: user attempted and got it wrong
                val isExplicitWrong = userAns != null && userAns != q.correctOptionIndex
                // Fallback for legacy attempts saved before answer tracking
                val isLegacyMistake = userAns == null && attempt.wrongAnswers > 0 && attempt.scorePercentage < 80

                if ((isExplicitWrong || isLegacyMistake) && key.isNotBlank() && !seenKeys.contains(key)) {
                    seenKeys.add(key)
                    mistakes.add(q)
                }
            }
        }
        return mistakes
    }

    /**
     * Returns stats about the mistake pool: (totalMistakes, unseenCount, cycleNumber)
     */
    fun getMistakeStats(
        context: Context,
        recentAttempts: List<TestAttemptEntity>
    ): Triple<Int, Int, Int> {
        val pool = getMistakeQuestionsPool(recentAttempts)
        if (pool.isEmpty()) return Triple(0, 0, 1)

        val prefs = context.getSharedPreferences("prepos_weak_drill_prefs", Context.MODE_PRIVATE)
        val servedKeys = prefs.getStringSet("served_keys", emptySet()) ?: emptySet()
        val cycle = prefs.getInt("cycle_number", 1)

        val unservedCount = pool.count { it.questionText.trim().lowercase() !in servedKeys }
        val effectiveUnseen = if (unservedCount == 0) pool.size else unservedCount

        return Triple(pool.size, effectiveUnseen, cycle)
    }

    /**
     * Builds a Weak Area / Mistake Drill test with strict NO-REPEAT cycle guarantee:
     * - Only includes questions the user answered INCORRECTLY.
     * - Never repeats a question in subsequent drills until the entire pool has been shown.
     * - Once all questions are shown, restarts a fresh cycle.
     */
    fun buildWeakAreaTest(
        context: Context,
        recentAttempts: List<TestAttemptEntity>,
        count: Int = 10
    ): List<QuestionItem> {
        val pool = getMistakeQuestionsPool(recentAttempts)
        if (pool.isEmpty()) return emptyList()

        val prefs = context.getSharedPreferences("prepos_weak_drill_prefs", Context.MODE_PRIVATE)
        val servedKeys = prefs.getStringSet("served_keys", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        var cycle = prefs.getInt("cycle_number", 1)

        // Find questions in mistake pool that have NOT been shown in current cycle yet
        var unserved = pool.filter { it.questionText.trim().lowercase() !in servedKeys }

        // If all mistake questions have been shown once, reset the cycle!
        if (unserved.isEmpty()) {
            servedKeys.clear()
            unserved = pool
            cycle += 1
            prefs.edit().putInt("cycle_number", cycle).apply()
        }

        // Pick up to 'count' unseen wrong questions
        val selected = unserved.shuffled().take(count)

        // Mark these picked questions as served for this cycle
        selected.forEach { q ->
            val key = q.questionText.trim().lowercase()
            if (key.isNotBlank()) {
                servedKeys.add(key)
            }
        }
        prefs.edit().putStringSet("served_keys", servedKeys).apply()

        return selected
    }

    /**
     * Backwards-compatible overload for legacy call sites.
     */
    fun buildWeakAreaTest(
        recentAttempts: List<TestAttemptEntity>,
        chapters: List<ChapterEntity> = emptyList()
    ): List<QuestionItem> {
        val pool = getMistakeQuestionsPool(recentAttempts)
        return pool.shuffled().take(10)
    }

    /**
     * Parses JSON array into List<QuestionItem> with in-memory caching for zero latency.
     */
    fun parseQuestionsFromJson(jsonStr: String): List<QuestionItem> {
        if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
        
        parsedQuestionsCache[jsonStr]?.let { return it }

        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<QuestionItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val opts = mutableListOf<String>()
                val optsArray = obj.optJSONArray("options")
                if (optsArray != null) {
                    for (j in 0 until optsArray.length()) {
                        opts.add(optsArray.getString(j))
                    }
                }
                val tagsList = mutableListOf<String>()
                val tagsArr = obj.optJSONArray("tags")
                if (tagsArr != null) {
                    for (j in 0 until tagsArr.length()) {
                        tagsList.add(tagsArr.getString(j))
                    }
                }
                val userSelected = if (obj.has("userSelectedOptionIndex") && !obj.isNull("userSelectedOptionIndex")) {
                    obj.optInt("userSelectedOptionIndex")
                } else null

                list.add(
                    QuestionItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        questionText = obj.optString("questionText", obj.optString("question", "")),
                        options = opts,
                        correctOptionIndex = obj.optInt("correctOptionIndex", 0),
                        explanation = obj.optString("explanation", ""),
                        difficulty = obj.optString("difficulty", "MEDIUM"),
                        tags = tagsList,
                        examSource = obj.optString("examSource", ""),
                        userSelectedOptionIndex = userSelected
                    )
                )
            }
            parsedQuestionsCache[jsonStr] = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun questionsToJson(questions: List<QuestionItem>): String {
        val array = JSONArray()
        questions.forEach { array.put(it.toJson()) }
        return array.toString()
    }
}
