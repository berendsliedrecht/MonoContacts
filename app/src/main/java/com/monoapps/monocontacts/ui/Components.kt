package com.monoapps.monocontacts.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Hairline dotted divider matching the stock Mudita list style. */
@Composable
fun DashedDivider(modifier: Modifier = Modifier, thickness: Dp = 1.dp) {
    Canvas(modifier = modifier.fillMaxWidth().height(thickness)) {
        drawLine(
            color = Color.Black,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = thickness.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx()), 0f),
        )
    }
}

/** Click without ripple; animations ghost on e-ink. */
fun Modifier.calmClickable(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    )
)

/** "First **Last**" styling: every word regular except the last, which is bold. */
fun styledName(displayName: String) = buildAnnotatedString {
    val words = displayName.trim().split(" ")
    if (words.size > 1) append(words.dropLast(1).joinToString(" ") + " ")
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(words.last()) }
}

/**
 * The stock app's circular FAB, but outlined (white fill, black ring)
 * instead of color-filled.
 */
@Composable
fun OutlinedFab(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, Color.Black, CircleShape)
            .calmClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = contentDescription,
            tint = Color.Black,
            modifier = Modifier.size(36.dp),
        )
    }
}

