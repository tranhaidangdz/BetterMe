package com.example.betterme.presentation.categorydetail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Markdown-light renderer for AI coaching replies.
 *
 * The free-tier chat models sometimes ship lightly-marked prose:
 *   "**Tích cực**: Bạn đang ..." or "- Bạn nên tập trung vào..."
 * Rather than render that verbatim (giant wall of text, asterisks visible),
 * this composable parses the reply line-by-line and emits Compose primitives:
 *
 * - blank lines  → paragraph break (Spacer 10dp)
 * - lines starting with `- ` or `• ` or `* ` → bulleted line (accent disc + indented text)
 * - inline `**bold**` segments → SpanStyle(FontWeight.Bold)
 *
 * No external markdown library — that would be ~200KB of dependency for what the AI
 * actually produces. The parser is intentionally minimal; anything fancier than bold
 * + bullets falls through as plain text, which is the safer failure mode.
 */
@Composable
fun AiRichText(
    raw: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val lines = raw.trim().lines()
    Column(modifier = modifier) {
        for ((index, rawLine) in lines.withIndex()) {
            val line = rawLine.trim()
            when {
                line.isEmpty() -> {
                    // Paragraph break — but skip leading blank lines and don't double-up.
                    if (index > 0 && index < lines.size - 1) {
                        Spacer(Modifier.height(10.dp))
                    }
                }
                isBulletLine(line) -> BulletRow(
                    text = stripBulletMarker(line),
                    accent = accent
                )
                else -> Text(
                    text = parseInlineBold(line),
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Text.TextPrimary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun BulletRow(text: String, accent: Color) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Small accent dot — sits aligned with the first text line.
        Spacer(Modifier.width(2.dp))
        Text(
            text = "●",
            style = BetterMeTypography.Body.Small.Medium,
            color = accent,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = parseInlineBold(text),
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextPrimary,
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}

private fun isBulletLine(line: String): Boolean =
    line.startsWith("- ") ||
        line.startsWith("• ") ||
        line.startsWith("* ") ||
        line.length > 2 && line[0].isDigit() && line[1] == '.' && line[2] == ' '

private fun stripBulletMarker(line: String): String {
    val markers = listOf("- ", "• ", "* ")
    for (m in markers) if (line.startsWith(m)) return line.removePrefix(m).trim()
    // Numbered "1. text"
    if (line.length > 2 && line[0].isDigit() && line[1] == '.') {
        return line.substring(2).trim()
    }
    return line
}

/**
 * Parses inline `**bold**` markers, returning an AnnotatedString with the bold
 * spans applied. Robust against unmatched `**` — falls back to literal text.
 */
internal fun parseInlineBold(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val next = text.indexOf("**", i)
        if (next == -1) {
            append(text.substring(i))
            return@buildAnnotatedString
        }
        // Append text before the marker
        append(text.substring(i, next))
        val closing = text.indexOf("**", next + 2)
        if (closing == -1) {
            // Unmatched closing — emit the rest as plain text.
            append(text.substring(next))
            return@buildAnnotatedString
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(next + 2, closing))
        }
        i = closing + 2
    }
}
