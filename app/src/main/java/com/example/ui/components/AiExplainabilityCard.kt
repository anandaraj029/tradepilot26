package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiConsensusResult
import com.example.ui.theme.*

@Composable
fun AiExplainabilityCard(
    deliberation: AiConsensusResult,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    FintechCard(
        modifier = modifier.testTag("ai_explainability_card"),
        borderColor = if (deliberation.isNoTradeAdvised) BearishRed.copy(alpha = 0.6f) else ElectricTeal.copy(alpha = 0.6f)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ElectricTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = ElectricTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI Explainability Protocol",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "4-Pillar Decision Transparency Engine",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = { isExpanded = !isExpanded }) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextSecondary
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {

                // 1. WHAT IS THE DECISION?
                ExplainPillarSection(
                    number = "1",
                    title = "WHAT IS THE DECISION?",
                    badgeColor = ElectricTeal,
                    testTag = "explain_pillar_what"
                ) {
                    Text(
                        text = deliberation.whatDecision.ifEmpty {
                            "${deliberation.consensusDecision.label} — ${deliberation.timeHorizon} (${deliberation.overallConfidence}% Confidence, Risk: ${deliberation.riskScoreAvg}/100)"
                        },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(6.dp), color = SurfaceElevated) {
                            Text(
                                text = "Action: ${deliberation.consensusDecision.label}",
                                color = if (deliberation.isNoTradeAdvised) BearishRed else BullishGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = SurfaceElevated) {
                            Text(
                                text = "Horizon: ${deliberation.timeHorizon}",
                                color = CyanBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. WHY WAS THIS DECISION MADE?
                ExplainPillarSection(
                    number = "2",
                    title = "WHY WAS THIS DECISION MADE?",
                    badgeColor = CyanBlue,
                    testTag = "explain_pillar_why"
                ) {
                    val whyList = if (deliberation.whyReasoning.isNotEmpty()) {
                        deliberation.whyReasoning
                    } else {
                        listOf(deliberation.synthesizedReason)
                    }

                    whyList.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(CyanBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reason,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. WHAT FACTORS COULD CHANGE THIS DECISION?
                ExplainPillarSection(
                    number = "3",
                    title = "WHAT FACTORS COULD CHANGE THIS DECISION?",
                    badgeColor = GoldAccent,
                    testTag = "explain_pillar_change"
                ) {
                    val changeList = if (deliberation.decisionChangeFactors.isNotEmpty()) {
                        deliberation.decisionChangeFactors
                    } else {
                        deliberation.keyCatalysts
                    }

                    changeList.forEach { factor ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChangeCircle,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = factor,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. WHAT CONDITIONS COULD INVALIDATE THE CURRENT ANALYSIS?
                ExplainPillarSection(
                    number = "4",
                    title = "WHAT CONDITIONS INVALIDATE THIS ANALYSIS?",
                    badgeColor = BearishRed,
                    testTag = "explain_pillar_invalidation"
                ) {
                    val invalidList = if (deliberation.invalidationConditionsList.isNotEmpty()) {
                        deliberation.invalidationConditionsList
                    } else {
                        deliberation.invalidatingConditions
                    }

                    invalidList.forEach { condition ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = BearishRed,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = condition,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExplainPillarSection(
    number: String,
    title: String,
    badgeColor: Color,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = badgeColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
