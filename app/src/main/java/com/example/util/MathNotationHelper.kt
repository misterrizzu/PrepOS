package com.example.util

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Custom Diamond Shape for Jetpack Compose canvas and surfaces.
 * Connects the top-center, right-center, bottom-center, and left-center points.
 */
class DiamondShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(size.width / 2f, size.height)
            lineTo(0f, size.height / 2f)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Custom Hexagon Shape for Jetpack Compose canvas and surfaces.
 */
class HexagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            moveTo(w * 0.25f, 0f)
            lineTo(w * 0.75f, 0f)
            lineTo(w, h * 0.5f)
            lineTo(w * 0.75f, h)
            lineTo(w * 0.25f, h)
            lineTo(0f, h * 0.5f)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Mathematical notation formatter and Unicode helpers.
 * Handles superscript powers, subscript indices, radical roots, division signs,
 * fractions, and Greek/scientific notation characters.
 */
object MathNotationHelper {

    /**
     * Converts an ASCII exponent indicator like `x^2`, `(a+b)^n`, `10^-3` into superscript unicode.
     * Also converts common math tokens like `sqrt(x)` to `√(x)`, `cube_root(x)` to `∛(x)`,
     * `root(n, x)` to `ⁿ√(x)`, and `/` division tokens into `÷` or neat fractional representations.
     */
    fun formatMathNotation(text: String): String {
        if (text.isEmpty()) return text

        return try {
            var result = text

            // Strip LaTeX inline & display math delimiters ($...$, $$...$$)
            result = result.replace(Regex("""(?<!\\)\$\$([^\$]+)\$\$(?!\$)""")) { it.groupValues[1] }
            result = result.replace(Regex("""(?<!\\)\$([^\$]+)\$(?!\$)""")) { it.groupValues[1] }

            // Clean LaTeX wrappers like \text{...}, \mathrm{...}, \mathbf{...}, \mathit{...}
            result = result.replace(Regex("""\\(?:text|mathrm|mathbf|mathit|textbf|textit)\{([^{}]+)\}""")) { it.groupValues[1] }
            result = result.replace(Regex("""\\left\s*([(\[{|])""")) { it.groupValues[1] }
            result = result.replace(Regex("""\\right\s*([)\]}|])""")) { it.groupValues[1] }

            // 1. LaTeX Math Operators & Symbols
            result = result.replace(Regex("""\\pm\b"""), "±")
            result = result.replace(Regex("""\\mp\b"""), "∓")
            result = result.replace(Regex("""\\times\b"""), "×")
            result = result.replace(Regex("""\\div\b"""), "÷")
            result = result.replace(Regex("""\\cdot\b"""), "·")
            result = result.replace(Regex("""\\leq?\b"""), "≤")
            result = result.replace(Regex("""\\geq?\b"""), "≥")
            result = result.replace(Regex("""\\neq?\b"""), "≠")
            result = result.replace(Regex("""\\approx(?:eq)?\b"""), "≈")
            result = result.replace(Regex("""\\equiv\b"""), "≡")
            result = result.replace(Regex("""\\propto\b"""), "∝")
            result = result.replace(Regex("""\\infty\b"""), "∞")
            result = result.replace(Regex("""\\degree\b|\\circ\b|\^\\circ|\^\{\\circ\}"""), "°")
            result = result.replace(Regex("""\\angle\b"""), "∠")
            result = result.replace(Regex("""\\perp\b"""), "⊥")
            result = result.replace(Regex("""\\parallel\b"""), "∥")
            result = result.replace(Regex("""\\in\b"""), "∈")
            result = result.replace(Regex("""\\notin\b"""), "∉")
            result = result.replace(Regex("""\\subset(?:eq)?\b"""), "⊂")
            result = result.replace(Regex("""\\cup\b"""), "∪")
            result = result.replace(Regex("""\\cap\b"""), "∩")
            result = result.replace(Regex("""\\int\b"""), "∫")
            result = result.replace(Regex("""\\partial\b"""), "∂")
            result = result.replace(Regex("""\\sum\b"""), "∑")
            result = result.replace(Regex("""\\prod\b"""), "∏")
            result = result.replace(Regex("""\\to\b|\\rightarrow\b"""), "→")
            result = result.replace(Regex("""\\leftarrow\b"""), "←")
            result = result.replace(Regex("""\\implies\b|\\Rightarrow\b"""), "⇒")
            result = result.replace(Regex("""\\iff\b|\\Leftrightarrow\b"""), "⇔")
            result = result.replace(Regex("""\\therefore\b"""), "∴")
            result = result.replace(Regex("""\\because\b"""), "∵")

            // 2. Greek Letters (both \alpha and plain words)
            result = result.replace(Regex("""\\alpha\b|\balpha\b""", RegexOption.IGNORE_CASE), "α")
            result = result.replace(Regex("""\\beta\b|\bbeta\b""", RegexOption.IGNORE_CASE), "β")
            result = result.replace(Regex("""\\gamma\b|\bgamma\b""", RegexOption.IGNORE_CASE), "γ")
            result = result.replace(Regex("""\\delta\b|\bdelta\b""", RegexOption.IGNORE_CASE), "δ")
            result = result.replace(Regex("""\\Delta\b|\bDelta\b"""), "Δ")
            result = result.replace(Regex("""\\theta\b|\btheta\b""", RegexOption.IGNORE_CASE), "θ")
            result = result.replace(Regex("""\\pi\b|\bpi\b""", RegexOption.IGNORE_CASE), "π")
            result = result.replace(Regex("""\\lambda\b|\blambda\b""", RegexOption.IGNORE_CASE), "λ")
            result = result.replace(Regex("""\\mu\b|\bmu\b""", RegexOption.IGNORE_CASE), "μ")
            result = result.replace(Regex("""\\sigma\b|\bsigma\b""", RegexOption.IGNORE_CASE), "σ")
            result = result.replace(Regex("""\\Sigma\b|\bSigma\b"""), "Σ")
            result = result.replace(Regex("""\\omega\b|\bomega\b""", RegexOption.IGNORE_CASE), "ω")
            result = result.replace(Regex("""\\Omega\b|\bOmega\b"""), "Ω")
            result = result.replace(Regex("""\\phi\b|\bphi\b""", RegexOption.IGNORE_CASE), "φ")
            result = result.replace(Regex("""\\rho\b|\brho\b""", RegexOption.IGNORE_CASE), "ρ")
            result = result.replace(Regex("""\\tau\b|\btau\b""", RegexOption.IGNORE_CASE), "τ")
            result = result.replace(Regex("""\\epsilon\b|\bepsilon\b""", RegexOption.IGNORE_CASE), "ε")
            result = result.replace(Regex("""\\infinity\b|\binfinity\b|\binf\b""", RegexOption.IGNORE_CASE), "∞")

            // 3. Roots conversions: \sqrt[n]{x}, \sqrt{x}, cbrt, sqrt
            result = result.replace(Regex("""(?i)\bcbrt\s*\(([^)]+)\)""")) { "∛(${it.groupValues[1]})" }
            result = result.replace(Regex("""(?i)\bsqrt\s*\(([^)]+)\)""")) { "√(${it.groupValues[1]})" }
            result = result.replace(Regex("""(?i)\bcbrt\b"""), "∛")
            result = result.replace(Regex("""(?i)\bsqrt\b"""), "√")

            // 4. Common ASCII Math shortcuts
            result = result.replace(Regex("""\s*\+/-\s*"""), " ± ")
            result = result.replace(Regex("""\s*\+-\s*"""), " ± ")
            result = result.replace(" <= ", " ≤ ")
            result = result.replace(" >= ", " ≥ ")
            result = result.replace(" != ", " ≠ ")
            result = result.replace(" ~= ", " ≈ ")
            result = result.replace(" -> ", " → ")
            result = result.replace(" <- ", " ← ")
            result = result.replace(" <-> ", " ↔ ")
            result = result.replace(" => ", " ⇒ ")
            result = result.replace(" <=> ", " ⇔ ")

            // 5. Convert explicit power notations e.g. x^2, y^(-1), 10^5, r^3
            result = replacePowersWithSuperscript(result)

            // Clean redundant extra spaces while preserving neat math formatting
            result.replace(Regex("""[ \t]{2,}"""), " ").trim()
        } catch (e: Throwable) {
            text
        }
    }

    /**
     * Converts a single number or expression string into unicode superscript.
     * e.g. "2" -> "²", "n" -> "ⁿ", "-1" -> "⁻¹", "10" -> "¹⁰"
     */
    fun toSuperscript(input: String): String {
        return buildString {
            for (ch in input) {
                append(charToSuperscript(ch))
            }
        }
    }

    /**
     * Converts a single number or index string into unicode subscript.
     * e.g. "1" -> "₁", "n" -> "ₙ", "2" -> "₂"
     */
    fun toSubscript(input: String): String {
        return buildString {
            for (ch in input) {
                append(charToSubscript(ch))
            }
        }
    }

    private fun charToSuperscript(c: Char): Char = when (c) {
        '0' -> '⁰'
        '1' -> '¹'
        '2' -> '²'
        '3' -> '³'
        '4' -> '⁴'
        '5' -> '⁵'
        '6' -> '⁶'
        '7' -> '⁷'
        '8' -> '⁸'
        '9' -> '⁹'
        '+' -> '⁺'
        '-' -> '⁻'
        '=' -> '⁼'
        '(' -> '⁽'
        ')' -> '⁾'
        'n' -> 'ⁿ'
        'i' -> 'ⁱ'
        'a' -> 'ᵃ'
        'b' -> 'ᵇ'
        'c' -> 'ᶜ'
        'd' -> 'ᵈ'
        'e' -> 'ᵉ'
        'f' -> 'ᶠ'
        'g' -> 'ᵍ'
        'h' -> 'ʰ'
        'j' -> 'ʲ'
        'k' -> 'ᵏ'
        'l' -> 'ˡ'
        'm' -> 'ᵐ'
        'o' -> 'ᵒ'
        'p' -> 'ᵖ'
        'r' -> 'ʳ'
        's' -> 'ˢ'
        't' -> 'ᵗ'
        'u' -> 'ᵘ'
        'v' -> 'ᵛ'
        'w' -> 'ʷ'
        'x' -> 'ˣ'
        'y' -> 'ʸ'
        'z' -> 'ᶻ'
        else -> c
    }

    private fun charToSubscript(c: Char): Char = when (c) {
        '0' -> '₀'
        '1' -> '₁'
        '2' -> '₂'
        '3' -> '₃'
        '4' -> '₄'
        '5' -> '₅'
        '6' -> '₆'
        '7' -> '₇'
        '8' -> '₈'
        '9' -> '₉'
        '+' -> '₊'
        '-' -> '₋'
        '=' -> '₌'
        '(' -> '₍'
        ')' -> '₎'
        'a' -> 'ₐ'
        'e' -> 'ₑ'
        'h' -> 'ₕ'
        'i' -> 'ᵢ'
        'j' -> 'ⱼ'
        'k' -> 'ₖ'
        'l' -> 'ₗ'
        'm' -> 'ₘ'
        'n' -> 'ₙ'
        'o' -> 'ₒ'
        'p' -> 'ₚ'
        'r' -> 'ᵣ'
        's' -> 'ₛ'
        't' -> 'ₜ'
        'u' -> 'ᵤ'
        'v' -> 'ᵥ'
        'x' -> 'ₓ'
        else -> c
    }

    /**
     * Replaces `^(...)` or `^xyz` patterns with superscript characters.
     */
    private fun replacePowersWithSuperscript(input: String): String {
        // Pattern 1: ^(expression) -> superscript(expression)
        var result = input.replace(Regex("""\^\{([^{}]+)\}""")) { match ->
            toSuperscript(match.groupValues[1])
        }
        result = result.replace(Regex("""\^(\([^\)]+\))""")) { match ->
            toSuperscript(match.groupValues[1])
        }
        // Pattern 2: ^[-0-9a-zA-Z]+ -> superscript
        result = result.replace(Regex("""\^([0-9a-zA-Z\+\-]+)""")) { match ->
            toSuperscript(match.groupValues[1])
        }
        // Pattern 3: subscript _{expression} or _[0-9a-zA-Z]
        result = result.replace(Regex("""\_\{([^{}]+)\}""")) { match ->
            toSubscript(match.groupValues[1])
        }
        result = result.replace(Regex("""\_([0-9a-zA-Z\+\-]+)""")) { match ->
            toSubscript(match.groupValues[1])
        }
        return result
    }
}
