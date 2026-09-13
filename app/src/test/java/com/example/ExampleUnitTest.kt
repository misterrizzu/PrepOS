package com.example

import com.example.ai.OfflineQuestionParser
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testOfflineQuestionParserVariants() {
    // Standard numbered with letters
    val text1 = """
      1. What is the capital of France?
      A) Paris
      B) London
      C) Rome
      D) Berlin
      Answer: A
      Explanation: Paris is the capital.
    """.trimIndent()
    val res1 = OfflineQuestionParser.parseLocally(text1)
    println("RES1: size=${res1.size}, Q=${res1.firstOrNull()?.questionText}")
    assertTrue("res1 should not be empty", res1.isNotEmpty())

    // Markdown bold format
    val text2 = """
      **Question 1:** What is photosynthesis?
      **A)** Process by which plants make food
      **B)** Process of water absorption
      **C)** Respiration in animals
      **D)** Cell division
      **Answer:** A
      **Explanation:** Plants convert sunlight into energy.
    """.trimIndent()
    val res2 = OfflineQuestionParser.parseLocally(text2)
    println("RES2: size=${res2.size}")
    assertTrue("res2 should not be empty", res2.isNotEmpty())

    // Dot options without parentheses
    val text3 = """
      Q1. Which of the following is an input device?
      A. Keyboard
      B. Monitor
      C. Printer
      D. Speaker
      Ans: A
      Exp: Keyboard is used to input data.
    """.trimIndent()
    val res3 = OfflineQuestionParser.parseLocally(text3)
    println("RES3: size=${res3.size}")
    assertTrue("res3 should not be empty", res3.isNotEmpty())

    // Unnumbered questions separated by double newlines
    val text4 = """
      What is HTTP?
      (A) HyperText Transfer Protocol
      (B) High Text Transfer Protocol
      (C) Hyperlink Transfer Protocol
      (D) None
      Correct: A
      Reason: Standard web protocol.

      What is CPU?
      (A) Central Processing Unit
      (B) Computer Personal Unit
      (C) Central Power Unit
      (D) None
      Correct Answer: A
      Solution: Brain of the computer.
    """.trimIndent()
    val res4 = OfflineQuestionParser.parseLocally(text4)
    println("RES4: size=${res4.size}")
    assertTrue("res4 should have 2 questions, got ${res4.size}", res4.size == 2)

    val text5 = """
      1. What is the speed of light?
      1) 3 x 10^8 m/s
      2) 3 x 10^6 m/s
      3) 3 x 10^5 m/s
      4) 3 x 10^7 m/s
      Answer: 1
      Explanation: In vacuum, speed of light is 3 x 10^8 m/s.
    """.trimIndent()
    val res5 = OfflineQuestionParser.parseLocally(text5)
    println("RES5: size=${res5.size}")
    assertTrue("res5 should have 1 question", res5.size == 1)
    assertEquals(0, res5[0].correctOptionIndex)

    // Test text where answer is option text itself: Ans: Tokyo
    val text6 = """
      Q: What is the capital of Japan?
      a) Tokyo
      b) Kyoto
      c) Osaka
      d) Hiroshima
      Ans: Tokyo
      Explanation: Tokyo is the capital of Japan.
    """.trimIndent()
    val res6 = OfflineQuestionParser.parseLocally(text6)
    println("RES6: size=${res6.size}, correctIdx=${res6.firstOrNull()?.correctOptionIndex}")
    assertTrue("res6 should have 1 question", res6.size == 1)
    assertEquals(0, res6[0].correctOptionIndex)

    // Test JSON format
    val text7 = """
      [
        {
          "question": "What is Python?",
          "options": ["Programming language", "Snake only", "Operating system", "Database"],
          "answer": "A",
          "explanation": "Python is a high-level programming language."
        }
      ]
    """.trimIndent()
    val res7 = OfflineQuestionParser.parseLocally(text7)
    println("RES7: size=${res7.size}")
    assertTrue("res7 should have 1 question", res7.size == 1)

    // Test UPSC statement format with blank lines
    val text8 = """
      Consider the following statements regarding photosynthesis:
      1. Light reactions take place in the thylakoid membrane.
      2. Dark reactions take place in the stroma.

      Which of the statements given above is/are correct?

      A) 1 only

      B) 2 only

      C) Both 1 and 2

      D) Neither 1 nor 2

      Answer: C
      Explanation: Both statements are factually correct.
    """.trimIndent()
    val res8 = OfflineQuestionParser.parseLocally(text8)
    println("RES8: size=${res8.size}")
    assertTrue("res8 should have 1 question", res8.size == 1)
    assertEquals(2, res8[0].correctOptionIndex)
  }
}

