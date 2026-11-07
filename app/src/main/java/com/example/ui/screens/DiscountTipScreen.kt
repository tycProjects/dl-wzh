package com.example.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.MathEvaluator
import com.example.model.DarkModePreference
import com.example.viewmodel.CalculatorUiState

@Composable
fun DiscountTipScreen(
    state: CalculatorUiState,
    onSetBill: (String) -> Unit,
    onSetDiscountPercent: (String) -> Unit,
    onSetTipPercent: (String) -> Unit,
    onSetSplitPeople: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = when (state.darkModePref) {
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
    }

    val bill = state.discountOriginalBill.toDoubleOrNull() ?: 0.0
    val discPct = state.discountPercent.toDoubleOrNull() ?: 0.0
    val tipPct = state.tipPercent.toDoubleOrNull() ?: 0.0
    val people = state.splitPeople.coerceAtLeast(1)

    val discountAmount = bill * (discPct / 100.0)
    val subtotalAfterDiscount = (bill - discountAmount).coerceAtLeast(0.0)
    val tipAmount = subtotalAfterDiscount * (tipPct / 100.0)
    val totalBill = subtotalAfterDiscount + tipAmount
    val perPerson = totalBill / people

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bill Amount Input Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) state.themePalette.darkSurface else state.themePalette.lightSurface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Bill & Offers",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = state.discountOriginalBill,
                    onValueChange = onSetBill,
                    label = { Text("Original Bill Amount") },
                    prefix = { Text("\$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_bill_amount")
                )

                // Discount % selector
                Text(
                    text = "Discount: ${discPct.toInt()}%",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("0", "10", "15", "20", "25", "30").forEach { pct ->
                        val isSelected = state.discountPercent == pct
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSetDiscountPercent(pct) },
                            label = { Text("$pct%") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = state.themePalette.primary,
                                selectedLabelColor = if (isDark) Color.Black else Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Tip % selector
                Text(
                    text = "Tip: ${tipPct.toInt()}%",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("0", "10", "15", "18", "20", "25").forEach { pct ->
                        val isSelected = state.tipPercent == pct
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSetTipPercent(pct) },
                            label = { Text("$pct%") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = state.themePalette.accent,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Split Between People
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Split Between",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { onSetSplitPeople(people - 1) },
                            enabled = people > 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease people")
                        }

                        Text(
                            text = "$people ${if (people == 1) "Person" else "People"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        FilledTonalIconButton(
                            onClick = { onSetSplitPeople(people + 1) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase people")
                        }
                    }
                }
            }
        }

        // Summary Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) state.themePalette.darkSurfaceVariant else state.themePalette.lightSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Summary",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Discount Saved", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("-\$" + MathEvaluator.formatResult(discountAmount, 2), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = state.themePalette.primary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("\$" + MathEvaluator.formatResult(subtotalAfterDiscount, 2), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tip Amount", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+\$" + MathEvaluator.formatResult(tipAmount, 2), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = state.themePalette.accent)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Bill", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "\$" + MathEvaluator.formatResult(totalBill, 2),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        color = state.themePalette.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("Per Person", fontSize = 11.sp, color = state.themePalette.primary, fontWeight = FontWeight.Bold)
                            Text(
                                text = "\$" + MathEvaluator.formatResult(perPerson, 2),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = state.themePalette.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
