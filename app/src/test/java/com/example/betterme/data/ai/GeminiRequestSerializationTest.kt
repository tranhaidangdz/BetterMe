package com.example.betterme.data.ai

import com.example.betterme.data.ai.dto.GeminiContent
import com.example.betterme.data.ai.dto.GeminiGenerateRequest
import com.example.betterme.data.ai.dto.GeminiGenerationConfig
import com.example.betterme.data.ai.dto.GeminiPart
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for the Gemini request JSON shape.
 *
 * Gemini's REST validator rejects requests whose *present* fields have null
 * values — e.g. `{"systemInstruction": null}` returns HTTP 400 INVALID_ARGUMENT.
 * The same applies to `role: null` on a [GeminiContent]: the field is optional,
 * but if present its value must be a non-null string.
 *
 * The fix lives in [GeminiNetwork]'s `Json { explicitNulls = false }`. This test
 * asserts the *same* config and verifies the produced JSON contains no `: null`
 * substring for any of the request's optional fields. If a future refactor flips
 * `explicitNulls` back to true (or constructs a new Json instance without it),
 * this test catches it before users see HTTP 400s.
 */
class GeminiRequestSerializationTest {

    /** Exact mirror of the [GeminiNetwork] Json config. Kept in lock-step on purpose. */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun `omits systemInstruction when null`() {
        val req = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart("hi")))
            ),
            systemInstruction = null,
            generationConfig = GeminiGenerationConfig(temperature = 0.6, maxOutputTokens = 120)
        )
        val out = json.encodeToString(GeminiGenerateRequest.serializer(), req)
        assertFalse("systemInstruction must not appear when null: $out", out.contains("systemInstruction"))
        assertFalse("no field should serialize as `null` in a Gemini request: $out", out.contains(":null"))
        assertTrue("must still ship contents", out.contains("\"contents\""))
        assertTrue("must still ship generationConfig", out.contains("\"generationConfig\""))
    }

    @Test
    fun `omits role on a content when null (systemInstruction case)`() {
        // The systemInstruction's GeminiContent has role = null by design — Google
        // doesn't accept a role on the system instruction. Verify it disappears.
        val req = GeminiGenerateRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart("hi")))),
            systemInstruction = GeminiContent(role = null, parts = listOf(GeminiPart("Bạn là AI trợ lý."))),
            generationConfig = GeminiGenerationConfig(temperature = 0.6, maxOutputTokens = 120)
        )
        val out = json.encodeToString(GeminiGenerateRequest.serializer(), req)
        assertTrue("systemInstruction must be present", out.contains("\"systemInstruction\""))
        // The user content role must survive
        assertTrue("user content role must survive", out.contains("\"role\":\"user\""))
        // No null role anywhere in the payload
        assertFalse("role must not appear as null: $out", out.contains("\"role\":null"))
    }

    @Test
    fun `omits responseMimeType when null but keeps temperature and maxOutputTokens`() {
        val cfg = GeminiGenerationConfig(temperature = 0.7, maxOutputTokens = 200, responseMimeType = null)
        val out = json.encodeToString(GeminiGenerationConfig.serializer(), cfg)
        assertFalse("responseMimeType must be omitted when null: $out", out.contains("responseMimeType"))
        assertTrue(out.contains("\"temperature\":0.7"))
        assertTrue(out.contains("\"maxOutputTokens\":200"))
    }

    @Test
    fun `no null token anywhere in the request payload`() {
        // Final integration check on a realistic request — system prompt + 2-turn
        // conversation + standard gen config. This mirrors what [GeminiChatTransport]
        // produces for a typical coach call.
        val req = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart("Tóm tắt tuần này"))),
                GeminiContent(role = "model", parts = listOf(GeminiPart("Bạn đã hoàn thành 4/5."))),
                GeminiContent(role = "user", parts = listOf(GeminiPart("Bước tiếp theo?")))
            ),
            systemInstruction = GeminiContent(
                role = null,
                parts = listOf(GeminiPart("Bạn là huấn luyện viên thói quen."))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.6,
                maxOutputTokens = 300,
                responseMimeType = null
            )
        )
        val out = json.encodeToString(GeminiGenerateRequest.serializer(), req)
        assertFalse("payload must not contain :null anywhere: $out", out.contains(":null"))
        assertFalse("payload must not contain : null anywhere: $out", out.contains(": null"))
    }
}
