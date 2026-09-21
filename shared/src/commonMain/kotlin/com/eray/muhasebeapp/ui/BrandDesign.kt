package com.eray.muhasebeapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared by Android and iOS. Semantic colors keep warnings distinct from brand actions. */
object BrandColors {
    val Navy = Color(0xFF014B8A)
    val Teal = Color(0xFF007F83)
    val Background = Color(0xFFF3F7FA)
    val Ink = Color(0xFF17364D)
    val Muted = Color(0xFF5F7485)
    val Border = Color(0xFFDDE8EF)
    val Soft = Color(0xFFE9F2F6)
    val Success = Color(0xFF087F65)
    val Warning = Color(0xFF9A620C)
    val Danger = Color(0xFFBC3D4F)
    val Violet = Color(0xFF6B58A3)
}

@Composable
fun HesapBenimTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = BrandColors.Navy, onPrimary = Color.White,
            primaryContainer = Color(0xFFE0EFFB), onPrimaryContainer = BrandColors.Navy,
            secondary = BrandColors.Teal, onSecondary = Color.White,
            secondaryContainer = Color(0xFFDDF3EE), onSecondaryContainer = BrandColors.Teal,
            tertiary = BrandColors.Violet,
            background = BrandColors.Background, onBackground = BrandColors.Ink,
            surface = Color.White, onSurface = BrandColors.Ink,
            surfaceVariant = BrandColors.Soft, onSurfaceVariant = BrandColors.Muted,
            outline = BrandColors.Muted, outlineVariant = BrandColors.Border,
            error = BrandColors.Danger, onError = Color.White
        ),
        shapes = Shapes(
            small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp), extraLarge = RoundedCornerShape(22.dp)
        ),
        content = content
    )
}

/**
 * Detects a right swipe gesture to trigger back navigation.
 */
fun Modifier.swipeToBack(enabled: Boolean = true, onBack: () -> Unit): Modifier {
    if (!enabled) return this
    return this.pointerInput(Unit) {
        var totalDrag = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalDrag = 0f },
            onHorizontalDrag = { _, dragAmount ->
                if (dragAmount > 0) totalDrag += dragAmount
            },
            onDragEnd = {
                if (totalDrag > 150) onBack()
            }
        )
    }
}

@Composable
fun BrandTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(color = Color.Transparent, contentColor = Color.White) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(BrandColors.Navy, BrandColors.Teal)))
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
            Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Text(
                    title, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle, fontSize = 11.sp, lineHeight = 14.sp, color = Color(0xFFDCF3F5),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            actions()
        }
    }
}

/**
 * Compact metric tile. [value] is kept on a single line; very long amounts should be
 * shortened by the caller (e.g. "₺70,0 Mn") so nothing gets visually truncated.
 */
@Composable
fun BrandMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White
) {
    val selected = containerColor != Color.White
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) containerColor else accent.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, if (selected) containerColor else accent.copy(alpha = 0.16f))
    ) {
        Column(
            Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                label, fontSize = 11.sp, lineHeight = 14.sp,
                color = if (selected) Color.White.copy(alpha = 0.85f) else BrandColors.Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                value, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else accent,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Compact single-row record card: identity on the left, amount on the right.

 */
@Composable
fun BrandRecordCard(
    title: String,
    detail: String,
    value: String,
    icon: ImageVector,
    accent: Color = BrandColors.Teal,
    caption: String = "",
    onClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth().then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(14.dp), color = Color.White,
        border = BorderStroke(1.dp, BrandColors.Border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = 0.09f)) {
                Icon(
                    icon, contentDescription = null, tint = accent,
                    modifier = Modifier.padding(7.dp).size(19.dp)
                )
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    title, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
                    color = BrandColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (detail.isNotBlank()) {
                    Text(
                        detail, fontSize = 11.sp, lineHeight = 14.sp, color = BrandColors.Muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.widthIn(max = 150.dp)
            ) {
                if (caption.isNotBlank()) {
                    Text(
                        caption, fontSize = 10.sp, lineHeight = 12.sp, color = BrandColors.Muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End
                    )
                }
                Text(
                    value, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold,
                    color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }

            actions()
        }
    }
}