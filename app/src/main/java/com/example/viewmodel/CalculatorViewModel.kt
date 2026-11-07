package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.HapticManager
import com.example.engine.MathEvaluator
import com.example.engine.SpeechMathParser
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class AppTab(val title: String) {
    BASIC("Basic"),
    SCIENTIFIC("Scientific"),
    VAT("VAT / Tax"),
    CONVERTER("Converter"),
    DISCOUNT("Discount & Tip"),
    SETTINGS("Settings")
}

data class CalculatorUiState(
    val currentTab: AppTab = AppTab.BASIC,
    val expression: String = "",
    val liveResult: String = "",
    val isEvaluated: Boolean = false,
    val memoryValue: Double = 0.0,
    val hasMemory: Boolean = false,
    val isRadianMode: Boolean = false,
    val history: List<CalculationItem> = emptyList(),
    val quickVatRate: Double = 20.0, // Used by Tax+ / Tax- in basic calculator
    
    // VAT Tab State
    val vatMode: VatCalculationMode = VatCalculationMode.NET_TO_GROSS,
    val vatAmountInput: String = "100",
    val vatRateInput: String = "20",
    val vatGrossInput: String = "120",
    val vatPresets: List<VatPreset> = listOf(
        VatPreset(5.0, "5%", "Reduced (Food/Books)"),
        VatPreset(10.0, "10%", "Standard (Japan/AU)"),
        VatPreset(12.0, "12%", "Goods / Services"),
        VatPreset(18.0, "18%", "Standard (India GST)"),
        VatPreset(20.0, "20%", "Standard (UK/FR/EU)"),
        VatPreset(21.0, "21%", "Standard (ES/NL/BE)"),
        VatPreset(25.0, "25%", "Standard (Nordic)")
    ),
    val vatResult: VatCalculationResult = VatCalculationResult(
        netAmount = 100.0,
        vatRate = 20.0,
        vatAmount = 20.0,
        grossAmount = 120.0
    ),

    // Converter Tab State
    val converterDimension: UnitDimension = UnitDimension.LENGTH,
    val converterFromUnit: ConversionUnit = UnitRegistry.getUnitsForDimension(UnitDimension.LENGTH)[0],
    val converterToUnit: ConversionUnit = UnitRegistry.getUnitsForDimension(UnitDimension.LENGTH)[1],
    val converterInputValue: String = "1",
    val converterOutputValue: String = "0.001",

    // Discount & Tip State
    val discountOriginalBill: String = "85.00",
    val discountPercent: String = "15",
    val tipPercent: String = "18",
    val splitPeople: Int = 2,

    // Settings & Theme
    val themePalette: ThemePalette = ThemePalette.CYAN_OLED,
    val darkModePref: DarkModePreference = DarkModePreference.DARK,
    val hapticEnabled: Boolean = true,
    val decimalPrecision: Int = 8,

    // Feedback / Roadmap Dialog
    val showRoadmapSheet: Boolean = false,
    val roadmapVotes: Map<String, Int> = mapOf(
        "Live Currency Exchange Rates" to 42,
        "Loan / Mortgage EMI Amortization" to 38,
        "Programmer Base-2 / HEX / OCT / BIN Mode" to 29,
        "Matrix & Complex Numbers" to 19,
        "Date & Age Duration Calculator" to 34
    ),
    val userVotedFeatures: Set<String> = emptySet(),
    val voiceMessage: String? = null
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("sharp_calc_prefs", Context.MODE_PRIVATE)
    private val haptic = HapticManager(application)
    private val evaluator = MathEvaluator()

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
        recalculateVat()
        recalculateConversion()
    }

    private fun loadPreferences() {
        val themeName = prefs.getString("theme", ThemePalette.CYAN_OLED.name) ?: ThemePalette.CYAN_OLED.name
        val theme = try { ThemePalette.valueOf(themeName) } catch (_: Exception) { ThemePalette.CYAN_OLED }

        val darkPrefName = prefs.getString("dark_mode", DarkModePreference.DARK.name) ?: DarkModePreference.DARK.name
        val darkPref = try { DarkModePreference.valueOf(darkPrefName) } catch (_: Exception) { DarkModePreference.DARK }

        val haptics = prefs.getBoolean("haptic", true)
        val precision = prefs.getInt("precision", 8)
        val quickVat = prefs.getFloat("quick_vat", 20.0f).toDouble()

        evaluator.decimalPrecision = precision

        _uiState.update {
            it.copy(
                themePalette = theme,
                darkModePref = darkPref,
                hapticEnabled = haptics,
                decimalPrecision = precision,
                quickVatRate = quickVat
            )
        }
    }

    private fun savePreferences() {
        prefs.edit().apply {
            putString("theme", _uiState.value.themePalette.name)
            putString("dark_mode", _uiState.value.darkModePref.name)
            putBoolean("haptic", _uiState.value.hapticEnabled)
            putInt("precision", _uiState.value.decimalPrecision)
            putFloat("quick_vat", _uiState.value.quickVatRate.toFloat())
            apply()
        }
    }

    // --- Keypad & Calculation Actions ---

    fun onKeyClick(key: String) {
        haptic.vibrateKeyClick(_uiState.value.hapticEnabled)

        when (key) {
            "AC" -> clearAll()
            "C" -> clearEntry()
            "⌫" -> backspace()
            "=" -> evaluateAndCommit()
            "±" -> toggleSign()
            "DEG", "RAD" -> toggleAngleMode()
            "MC" -> memoryClear()
            "MR" -> memoryRecall()
            "M+" -> memoryAdd()
            "M-" -> memorySubtract()
            "TAX+" -> applyQuickTax(isAdd = true)
            "TAX-" -> applyQuickTax(isAdd = false)
            else -> appendInput(key)
        }
    }

    private fun appendInput(str: String) {
        _uiState.update { current ->
            val isFresh = current.isEvaluated && str.all { it.isDigit() || it == '.' }
            val newExpr = if (isFresh) {
                str
            } else {
                current.expression + str
            }
            val live = previewEvaluation(newExpr)
            current.copy(
                expression = newExpr,
                liveResult = live,
                isEvaluated = false
            )
        }
    }

    private fun clearAll() {
        _uiState.update {
            it.copy(
                expression = "",
                liveResult = "",
                isEvaluated = false
            )
        }
    }

    private fun clearEntry() {
        _uiState.update {
            it.copy(
                expression = "",
                liveResult = ""
            )
        }
    }

    private fun backspace() {
        _uiState.update { current ->
            if (current.expression.isNotEmpty()) {
                val newExpr = current.expression.dropLast(1)
                val live = if (newExpr.isNotEmpty()) previewEvaluation(newExpr) else ""
                current.copy(
                    expression = newExpr,
                    liveResult = live,
                    isEvaluated = false
                )
            } else {
                current
            }
        }
    }

    private fun toggleSign() {
        _uiState.update { current ->
            val expr = current.expression
            if (expr.isEmpty()) return@update current
            val newExpr = if (expr.startsWith("-")) {
                expr.substring(1)
            } else {
                "-$expr"
            }
            current.copy(
                expression = newExpr,
                liveResult = previewEvaluation(newExpr)
            )
        }
    }

    private fun toggleAngleMode() {
        val newRadian = !_uiState.value.isRadianMode
        evaluator.isRadianMode = newRadian
        _uiState.update {
            it.copy(
                isRadianMode = newRadian,
                liveResult = previewEvaluation(it.expression)
            )
        }
    }

    private fun previewEvaluation(expr: String): String {
        if (expr.isBlank()) return ""
        return when (val res = evaluator.evaluate(expr)) {
            is MathEvaluator.EvalResult.Success -> "= ${res.formatted}"
            is MathEvaluator.EvalResult.Error -> ""
        }
    }

    private fun evaluateAndCommit() {
        haptic.vibrateHeavy(_uiState.value.hapticEnabled)
        val currentExpr = _uiState.value.expression
        if (currentExpr.isBlank()) return

        when (val res = evaluator.evaluate(currentExpr)) {
            is MathEvaluator.EvalResult.Success -> {
                val item = CalculationItem(
                    expression = currentExpr,
                    result = res.formatted,
                    category = if (_uiState.value.currentTab == AppTab.SCIENTIFIC) "Scientific" else "Basic"
                )
                _uiState.update {
                    it.copy(
                        expression = res.formatted.replace(",", ""),
                        liveResult = "",
                        isEvaluated = true,
                        history = listOf(item) + it.history.take(49)
                    )
                }
            }
            is MathEvaluator.EvalResult.Error -> {
                _uiState.update {
                    it.copy(liveResult = "Error: ${res.message}")
                }
            }
        }
    }

    // --- Quick TAX+ / TAX- on Keypad ---

    private fun applyQuickTax(isAdd: Boolean) {
        val currentExpr = _uiState.value.expression
        if (currentExpr.isBlank()) return
        when (val eval = evaluator.evaluate(currentExpr)) {
            is MathEvaluator.EvalResult.Success -> {
                val base = eval.value
                val rate = _uiState.value.quickVatRate
                val resultVal = if (isAdd) {
                    base * (1.0 + rate / 100.0)
                } else {
                    base / (1.0 + rate / 100.0)
                }
                val taxAction = if (isAdd) "+Tax ($rate%)" else "-Tax ($rate%)"
                val formatted = MathEvaluator.formatResult(resultVal, _uiState.value.decimalPrecision)
                val item = CalculationItem(
                    expression = "$currentExpr $taxAction",
                    result = formatted,
                    category = "VAT Quick"
                )
                _uiState.update {
                    it.copy(
                        expression = formatted.replace(",", ""),
                        liveResult = "",
                        isEvaluated = true,
                        history = listOf(item) + it.history.take(49)
                    )
                }
            }
            is MathEvaluator.EvalResult.Error -> {}
        }
    }

    // --- Memory Functions ---

    private fun memoryClear() {
        _uiState.update { it.copy(memoryValue = 0.0, hasMemory = false) }
    }

    private fun memoryRecall() {
        val mem = _uiState.value.memoryValue
        val formatted = MathEvaluator.formatResult(mem, _uiState.value.decimalPrecision)
        appendInput(formatted.replace(",", ""))
    }

    private fun memoryAdd() {
        evalCurrentValue()?.let { v ->
            val newMem = _uiState.value.memoryValue + v
            _uiState.update { it.copy(memoryValue = newMem, hasMemory = true) }
        }
    }

    private fun memorySubtract() {
        evalCurrentValue()?.let { v ->
            val newMem = _uiState.value.memoryValue - v
            _uiState.update { it.copy(memoryValue = newMem, hasMemory = true) }
        }
    }

    private fun evalCurrentValue(): Double? {
        val expr = _uiState.value.expression
        if (expr.isBlank()) return null
        return when (val res = evaluator.evaluate(expr)) {
            is MathEvaluator.EvalResult.Success -> res.value
            is MathEvaluator.EvalResult.Error -> null
        }
    }

    // --- History actions ---

    fun onSelectHistoryItem(item: CalculationItem) {
        _uiState.update {
            it.copy(
                expression = item.result.replace(",", ""),
                liveResult = "",
                isEvaluated = true
            )
        }
    }

    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
    }

    // --- Tab Navigation ---

    fun switchTab(tab: AppTab) {
        haptic.vibrateKeyClick(_uiState.value.hapticEnabled)
        _uiState.update { it.copy(currentTab = tab) }
    }

    // --- Dedicated VAT Screen Actions ---

    fun setVatMode(mode: VatCalculationMode) {
        _uiState.update { it.copy(vatMode = mode) }
        recalculateVat()
    }

    fun setVatAmountInput(value: String) {
        _uiState.update { it.copy(vatAmountInput = value) }
        recalculateVat()
    }

    fun setVatRateInput(value: String) {
        _uiState.update { it.copy(vatRateInput = value) }
        recalculateVat()
    }

    fun setVatGrossInput(value: String) {
        _uiState.update { it.copy(vatGrossInput = value) }
        recalculateVat()
    }

    fun selectVatPreset(preset: VatPreset) {
        _uiState.update {
            it.copy(
                vatRateInput = if (preset.rate % 1.0 == 0.0) preset.rate.toInt().toString() else preset.rate.toString(),
                quickVatRate = preset.rate
            )
        }
        savePreferences()
        recalculateVat()
    }

    fun addCustomVatPreset(rate: Double, label: String) {
        val newPreset = VatPreset(rate, label)
        _uiState.update {
            it.copy(vatPresets = it.vatPresets + newPreset)
        }
    }

    private fun recalculateVat() {
        val state = _uiState.value
        val rate = state.vatRateInput.toDoubleOrNull() ?: 0.0

        val result = when (state.vatMode) {
            VatCalculationMode.NET_TO_GROSS -> {
                val net = state.vatAmountInput.toDoubleOrNull() ?: 0.0
                val vatAmt = net * (rate / 100.0)
                val gross = net + vatAmt
                VatCalculationResult(net, rate, vatAmt, gross)
            }
            VatCalculationMode.GROSS_TO_NET -> {
                val gross = state.vatGrossInput.toDoubleOrNull() ?: 0.0
                val net = if (1.0 + rate / 100.0 != 0.0) gross / (1.0 + rate / 100.0) else 0.0
                val vatAmt = gross - net
                VatCalculationResult(net, rate, vatAmt, gross)
            }
            VatCalculationMode.CUSTOM_ANALYSIS -> {
                val net = state.vatAmountInput.toDoubleOrNull() ?: 0.0
                val gross = state.vatGrossInput.toDoubleOrNull() ?: 0.0
                val vatAmt = gross - net
                val computedRate = if (net > 0) (vatAmt / net) * 100.0 else 0.0
                VatCalculationResult(net, computedRate, vatAmt, gross)
            }
        }
        _uiState.update { it.copy(vatResult = result) }
    }

    fun sendVatResultToCalc(value: Double) {
        val formatted = MathEvaluator.formatResult(value, _uiState.value.decimalPrecision)
        _uiState.update {
            it.copy(
                currentTab = AppTab.BASIC,
                expression = formatted.replace(",", ""),
                liveResult = "",
                isEvaluated = true
            )
        }
    }

    // --- Converter Tab Actions ---

    fun setConverterDimension(dimension: UnitDimension) {
        val units = UnitRegistry.getUnitsForDimension(dimension)
        _uiState.update {
            it.copy(
                converterDimension = dimension,
                converterFromUnit = units[0],
                converterToUnit = if (units.size > 1) units[1] else units[0]
            )
        }
        recalculateConversion()
    }

    fun setConverterFromUnit(unit: ConversionUnit) {
        _uiState.update { it.copy(converterFromUnit = unit) }
        recalculateConversion()
    }

    fun setConverterToUnit(unit: ConversionUnit) {
        _uiState.update { it.copy(converterToUnit = unit) }
        recalculateConversion()
    }

    fun setConverterInputValue(value: String) {
        _uiState.update { it.copy(converterInputValue = value) }
        recalculateConversion()
    }

    fun swapConverterUnits() {
        _uiState.update {
            it.copy(
                converterFromUnit = it.converterToUnit,
                converterToUnit = it.converterFromUnit
            )
        }
        recalculateConversion()
    }

    private fun recalculateConversion() {
        val state = _uiState.value
        val input = state.converterInputValue.toDoubleOrNull() ?: 0.0
        val converted = UnitRegistry.convert(
            value = input,
            from = state.converterFromUnit,
            to = state.converterToUnit,
            dimension = state.converterDimension
        )
        val formatted = MathEvaluator.formatResult(converted, state.decimalPrecision)
        _uiState.update { it.copy(converterOutputValue = formatted) }
    }

    // --- Discount & Tip Actions ---

    fun setDiscountBill(bill: String) {
        _uiState.update { it.copy(discountOriginalBill = bill) }
    }

    fun setDiscountPercent(percent: String) {
        _uiState.update { it.copy(discountPercent = percent) }
    }

    fun setTipPercent(percent: String) {
        _uiState.update { it.copy(tipPercent = percent) }
    }

    fun setSplitPeople(count: Int) {
        if (count >= 1) {
            _uiState.update { it.copy(splitPeople = count) }
        }
    }

    // --- Settings Actions ---

    fun setThemePalette(palette: ThemePalette) {
        _uiState.update { it.copy(themePalette = palette) }
        savePreferences()
    }

    fun setDarkModePref(pref: DarkModePreference) {
        _uiState.update { it.copy(darkModePref = pref) }
        savePreferences()
    }

    fun setHapticEnabled(enabled: Boolean) {
        _uiState.update { it.copy(hapticEnabled = enabled) }
        savePreferences()
    }

    fun setDecimalPrecision(precision: Int) {
        evaluator.decimalPrecision = precision
        _uiState.update { it.copy(decimalPrecision = precision) }
        savePreferences()
        recalculateVat()
        recalculateConversion()
    }

    // --- Roadmap & User Feedback Sheet Actions ---

    fun toggleRoadmapSheet(show: Boolean) {
        _uiState.update { it.copy(showRoadmapSheet = show) }
    }

    fun voteFeature(featureName: String) {
        haptic.vibrateKeyClick(_uiState.value.hapticEnabled)
        _uiState.update { current ->
            val alreadyVoted = featureName in current.userVotedFeatures
            val votes = current.roadmapVotes.toMutableMap()
            val currentCount = votes[featureName] ?: 0
            if (alreadyVoted) {
                votes[featureName] = (currentCount - 1).coerceAtLeast(0)
                current.copy(
                    roadmapVotes = votes,
                    userVotedFeatures = current.userVotedFeatures - featureName
                )
            } else {
                votes[featureName] = currentCount + 1
                current.copy(
                    roadmapVotes = votes,
                    userVotedFeatures = current.userVotedFeatures + featureName
                )
            }
        }
    }

    // --- Voice-To-Text Math Input Actions ---

    fun onSpokenEquationReceived(spokenText: String) {
        haptic.vibrateHeavy(_uiState.value.hapticEnabled)
        val parsed = SpeechMathParser.parseSpokenText(spokenText)

        if (parsed.isClearCommand) {
            clearAll()
            _uiState.update { it.copy(voiceMessage = "Cleared calculator via voice") }
            return
        }

        if (parsed.expression.isNotBlank()) {
            val expr = parsed.expression
            val live = previewEvaluation(expr)
            _uiState.update {
                it.copy(
                    expression = expr,
                    liveResult = live,
                    isEvaluated = false,
                    voiceMessage = "Heard: \"$spokenText\" → $expr"
                )
            }
            if (parsed.shouldEvaluate) {
                evaluateAndCommit()
            }
        } else {
            _uiState.update {
                it.copy(voiceMessage = "Could not parse math from: \"$spokenText\"")
            }
        }
    }

    fun clearVoiceMessage() {
        _uiState.update { it.copy(voiceMessage = null) }
    }
}
