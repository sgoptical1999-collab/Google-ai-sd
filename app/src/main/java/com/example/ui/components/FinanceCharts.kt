package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.CategoryBreakdownItem
import com.example.domain.CurrencyFormatter
import com.example.domain.PeriodChartBucket
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PurpleAccent

private val CHART_PALETTE = listOf(
    EmeraldPrimary,
    InfoCyan,
    PendingAmber,
    ExpenseCoral,
    PurpleAccent,
    Color(0xFF84CC16),
    Color(0xFFEC4899),
    Color(0xFF14B8A6)
)

@Composable
fun IncomeExpenseSplit100Card(
    title: String,
    income: Double,
    expense: Double,
    incomePercent: Float,
    expensePercent: Float,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val total = income + expense
    val incWeight = if (total > 0.0) (income / total).toFloat().coerceIn(0.02f, 0.98f) else 0.5f
    val expWeight = if (total > 0.0) (1f - incWeight).coerceIn(0.02f, 0.98f) else 0.5f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Out of 100%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSlate
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (total <= 0.0) {
                Text(
                    text = "No income or expenses recorded for this period yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(50))
                ) {
                    if (income > 0.0) {
                        Box(
                            modifier = Modifier
                                .weight(incWeight)
                                .height(14.dp)
                                .background(EmeraldPrimary)
                        )
                    }
                    if (expense > 0.0) {
                        Box(
                            modifier = Modifier
                                .weight(expWeight)
                                .height(14.dp)
                                .background(ExpenseCoral)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        LegendDot(color = EmeraldPrimary, label = "Income")
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f%%", incomePercent),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = CurrencyFormatter.format(income, currencySymbol),
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedSlate
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        LegendDot(color = ExpenseCoral, label = "Expense")
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f%%", expensePercent),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseCoral
                        )
                        Text(
                            text = CurrencyFormatter.format(expense, currencySymbol),
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedSlate
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeVsExpenseChartCard(
    title: String,
    buckets: List<PeriodChartBucket>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val hasData = buckets.any { it.income > 0 || it.expense > 0 }
    val animProgress by animateFloatAsState(
        targetValue = if (hasData) 1f else 0f,
        animationSpec = tween(durationMillis = 550),
        label = "barChartAnim"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot(color = EmeraldPrimary, label = "Income")
                    LegendDot(color = ExpenseCoral, label = "Expense")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasData) {
                EmptyChartState(
                    icon = Icons.Rounded.BarChart,
                    message = "No income or expense activity recorded in this period yet."
                )
            } else {
                val maxVal = buckets.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                ) {
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val y = size.height * (i.toFloat() / gridSteps)
                        drawLine(
                            color = NavyBorder.copy(alpha = 0.45f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val groupWidth = size.width / buckets.size.coerceAtLeast(1)
                    val barWidth = (groupWidth * 0.28f).coerceIn(6.dp.toPx(), 18.dp.toPx())
                    val gap = 3.dp.toPx()
                    val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                    buckets.forEachIndexed { index, bucket ->
                        val centerX = groupWidth * index + groupWidth / 2f
                        val incHeight = ((bucket.income / maxVal).toFloat() * size.height * 0.92f) * animProgress
                        val expHeight = ((bucket.expense / maxVal).toFloat() * size.height * 0.92f) * animProgress

                        if (incHeight > 0f) {
                            drawRoundRect(
                                color = EmeraldPrimary,
                                topLeft = Offset(centerX - barWidth - gap / 2f, size.height - incHeight),
                                size = Size(barWidth, incHeight),
                                cornerRadius = corner
                            )
                        }
                        if (expHeight > 0f) {
                            drawRoundRect(
                                color = ExpenseCoral,
                                topLeft = Offset(centerX + gap / 2f, size.height - expHeight),
                                size = Size(barWidth, expHeight),
                                cornerRadius = corner
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    buckets.forEach { bucket ->
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedSlate,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                val peak = buckets.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0
                Text(
                    text = "Peak bar: ${CurrencyFormatter.formatCompact(peak, currencySymbol)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate
                )
            }
        }
    }
}

@Composable
fun CategoryBreakdownChartCard(
    breakdown: List<CategoryBreakdownItem>,
    totalExpense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val sweepScale by animateFloatAsState(
        targetValue = if (breakdown.isNotEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "donutAnim"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Category-wise Expense Share",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (breakdown.isEmpty() || totalExpense <= 0.0) {
                EmptyChartState(
                    icon = Icons.Rounded.PieChart,
                    message = "Expense categories will visualize automatically as transactions are logged."
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(132.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(124.dp)) {
                            val strokeWidth = 18.dp.toPx()
                            var startAngle = -90f
                            breakdown.take(8).forEachIndexed { idx, item ->
                                val sweep = (item.percentage / 100f) * 360f * sweepScale
                                val color = CHART_PALETTE[idx % CHART_PALETTE.size]
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += sweep
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Spent",
                                style = MaterialTheme.typography.labelMedium,
                                color = MutedSlate
                            )
                            Text(
                                text = CurrencyFormatter.formatCompact(totalExpense, currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        breakdown.take(5).forEachIndexed { idx, item ->
                            val dotColor = CHART_PALETTE[idx % CHART_PALETTE.size]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.categoryName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${item.percentage.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedSlate
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MutedSlate
        )
    }
}

@Composable
private fun EmptyChartState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(vertical = 28.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MutedSlate,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlate
            )
        }
    }
}
