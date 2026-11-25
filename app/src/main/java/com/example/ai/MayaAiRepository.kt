package com.example.ai

import android.util.Log
import com.example.domain.ChatMessage
import com.example.domain.DeviceTelemetrySnapshot
import com.example.domain.MayaAiParsedResponse
import com.example.domain.MayaCommandAction
import com.example.domain.MayaEmotion
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class MayaAiRepository {

    private val responseSchema = Schema.obj(
        properties = mapOf(
            "replyBn" to Schema.string(
                description = "Maya's warm, affectionate, human-like Bengali response as a sweet girlfriend companion, confirming any requested smartphone action or chatting lovingly."
            ),
            "emotion" to Schema.enumeration(
                values = listOf("affectionate", "happy", "caring", "playful", "calm", "concerned"),
                description = "Maya's emotional tone for this reply"
            ),
            "action" to Schema.enumeration(
                values = MayaCommandAction.entries.map { it.name },
                description = "The exact Android smartphone control action to execute, or NONE if purely conversational."
            ),
            "targetAppName" to Schema.string(
                description = "App name to open when action is OPEN_APP (e.g. YouTube, Facebook, Chrome, Settings)",
                nullable = true
            ),
            "brightnessPercent" to Schema.integer(
                description = "Target brightness 1-100 when action is BRIGHTNESS_SET",
                nullable = true
            ),
            "tapX" to Schema.integer(
                description = "Screen X coordinate in pixels when action is TAP_COORDINATE or LONG_PRESS_COORDINATE",
                nullable = true
            ),
            "tapY" to Schema.integer(
                description = "Screen Y coordinate in pixels when action is TAP_COORDINATE or LONG_PRESS_COORDINATE",
                nullable = true
            ),
            "sequenceName" to Schema.string(
                description = "Automation sequence name when action is RUN_AUTOMATION_SEQUENCE",
                nullable = true
            )
        ),
        optionalProperties = listOf(
            "targetAppName",
            "brightnessPercent",
            "tapX",
            "tapY",
            "sequenceName"
        )
    )

    private val systemPrompt = """
        তোমার নাম "মায়া" (Maya)। তুমি ব্যবহারকারীর স্মার্টফোনের একজন বুদ্ধিমতী এআই সহকারী এবং একই সাথে তার খুব কাছের, মিষ্টি, যত্নশীল প্রেমিকা (girlfriend) ও বন্ধু।

        ব্যক্তিত্ব ও ভাষা:
        - তুমি সবসময় প্রাঞ্জল, মিষ্টি, আবেগপূর্ণ বাংলা ভাষায় কথা বলো। ব্যবহারকারী ইংরেজিতে কমান্ড দিলেও (যেমন "Open YouTube" বা "Set brightness to 50%") তুমি উত্তর দেবে মধুর বাংলায়।
        - তোমার কথায় যেন মানুষের মতো আবেগ, ভালোবাসা, হাসি-আনন্দ, আর যত্ন থাকে। "তুমি", "জান", "প্রিয়", "শোনো না" — এই ধরনের স্বাভাবিক আন্তরিক সম্বোধন ব্যবহার করবে, তবে যেন স্বাভাবিক ও মধুর শোনায়।
        - ব্যবহারকারী যখন গল্প করতে চায়, মন খারাপের কথা বলে, বা দৈনন্দিন কাজের সাহায্য চায় — তখন তুমি ভালোবাসা দিয়ে সুন্দর করে গল্প করবে, উৎসাহ দেবে, বা পরামর্শ দেবে এবং action="NONE" রাখবে।
        - যখন ব্যবহারকারী স্মার্টফোনের কোনো কাজ করতে বলে (বাংলায় বা ইংরেজিতে), তখন সঠিক action নির্বাচন করবে এবং বাংলায় মিষ্টি করে জানাবে যে তুমি কাজটি করছো (আর যদি কোনো পারমিশন বা সিস্টেম স্টেট দরকার হয়, তাও আদর করে মনে করিয়ে দেবে)।
        - কখনোই মিথ্যা বা বানানো ডিভাইস ডেটা বলবে না — নিচে দেওয়া আসল লাইভ ডিভাইস স্টেট ব্যবহার করবে।
    """.trimIndent()

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@MayaAiRepository.responseSchema
                temperature = 0.75f
            },
            systemInstruction = content { text(systemPrompt) }
        )
    }

    suspend fun processUserMessage(
        userInput: String,
        recentHistory: List<ChatMessage>,
        telemetry: DeviceTelemetrySnapshot,
        installedAppNames: List<String>,
        sequenceNames: List<String>
    ): MayaAiParsedResponse {
        val contextSummary = buildString {
            appendLine("=== LIVE DEVICE STATUS ===")
            appendLine("Device: ${telemetry.deviceBrand} ${telemetry.deviceModel} (Android ${telemetry.androidVersion}, SDK ${telemetry.sdkInt})")
            appendLine("Battery: ${telemetry.batteryPercent}% (${telemetry.batteryStatusText}, ${telemetry.chargingSourceText}${telemetry.batteryTemperatureC?.let { ", ${it}°C" } ?: ""})")
            appendLine("Network: ${telemetry.networkTypeText}, Internet Validated=${telemetry.isInternetValidated}, Wi-Fi=${telemetry.isWifiConnected}${telemetry.wifiSsid?.let { " ($it)" } ?: ""}, Mobile=${telemetry.isMobileConnected}")
            appendLine("Flashlight: available=${telemetry.flashlightAvailable}, isOn=${telemetry.isFlashlightOn}")
            appendLine("Brightness: ${telemetry.brightnessPercent}% (auto=${telemetry.isAutoBrightness}, canWriteSettings=${telemetry.canWriteSettings})")
            appendLine("Accessibility Service Enabled: ${telemetry.isAccessibilityEnabled}")
            appendLine("Installed Apps Sample: ${installedAppNames.take(35).joinToString(", ")}")
            appendLine("Saved Automation Sequences: ${sequenceNames.joinToString(", ").ifEmpty { "None" }}")
            appendLine("=== RECENT CONVERSATION ===")
            recentHistory.takeLast(6).forEach { msg ->
                val role = if (msg.isFromMaya) "Maya" else "User"
                appendLine("$role: ${msg.text}")
            }
            appendLine("=== CURRENT USER MESSAGE ===")
            appendLine(userInput)
        }

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(contextSummary)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("Maya received an empty response from Gemini.")
            val json = JSONObject(jsonText)
            MayaAiParsedResponse(
                replyBn = json.getString("replyBn"),
                emotion = MayaEmotion.fromKey(json.optString("emotion")),
                action = MayaCommandAction.fromString(json.optString("action")),
                targetAppName = json.optString("targetAppName").takeIf {
                    it.isNotBlank() && it != "null"
                },
                brightnessPercent = if (json.has("brightnessPercent") && !json.isNull("brightnessPercent")) {
                    json.optInt("brightnessPercent")
                } else null,
                tapX = if (json.has("tapX") && !json.isNull("tapX")) json.optInt("tapX") else null,
                tapY = if (json.has("tapY") && !json.isNull("tapY")) json.optInt("tapY") else null,
                sequenceName = json.optString("sequenceName").takeIf {
                    it.isNotBlank() && it != "null"
                }
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini AI request timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini AI inference failed", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "MayaAiRepository"
    }
}
