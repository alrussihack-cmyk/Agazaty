package com.alrussihack.agazaty.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alrussihack.agazaty.R

/** Identity tokens, sampled from the six reference screens. */
object Agz {
    val Cream = Color(0xFFFFF8F1)       // screen background
    val Surface = Color(0xFFFFFFFF)     // cards, inputs, nav bar
    val Primary = Color(0xFFC2410C)     // hero card, selected pills, CTA, FAB
    val PrimaryDark = Color(0xFF9A3412) // text on pale orange
    val Pale = Color(0xFFFFEDD5)        // avatars, "في إجازة" badge, autofill card
    val Peach = Color(0xFFFDBA74)       // chart bars
    val Ink = Color(0xFF2B1B10)         // titles
    val Muted = Color(0xFF6B5645)       // secondary text, labels, inactive nav
    val Border = Color(0xFFF3E3D3)      // 1dp hairline on every card / input / pill
    val Placeholder = Color(0xFF757575)
    val Mint = Color(0xFFCCFBF1);  val MintInk = Color(0xFF115E59)   // returned ≤ 30 days / active
    val Sky = Color(0xFFDBEAFE);   val SkyInk = Color(0xFF1E40AF)    // returned > 30 days
    val Stone = Color(0xFFE7E5E4); val StoneInk = Color(0xFF44403C)  // not recorded
    val StoneDot = Color(0xFF78716C)
    val OnPrimaryChip = Color.White.copy(alpha = 0.18f)
}

object AgzShape {
    val Card = RoundedCornerShape(22.dp)
    val Hero = RoundedCornerShape(28.dp)
    val Field = RoundedCornerShape(22.dp)
    val Pill = RoundedCornerShape(50)
    val Button = RoundedCornerShape(24.dp)
}

val Cairo = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
    Font(R.font.cairo_extrabold, FontWeight.ExtraBold),
    Font(R.font.cairo_black, FontWeight.Black),
)

private fun base(size: Int, weight: FontWeight, color: Color = Agz.Ink, line: Int = (size * 1.4).toInt()) = TextStyle(
    fontFamily = Cairo,
    fontSize = size.sp,
    fontWeight = weight,
    color = color,
    lineHeight = line.sp,
    textDirection = TextDirection.Rtl,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
)

/** Named styles used across screens. */
object AgzType {
    val Display = base(42, FontWeight.Black, line = 48)
    val Title = base(28, FontWeight.ExtraBold, line = 38)
    val Heading = base(24, FontWeight.ExtraBold, line = 32)
    val Section = base(19, FontWeight.Bold, line = 26)
    val CardTitle = base(17, FontWeight.Bold, line = 24)
    val Body = base(15, FontWeight.Normal, Agz.Muted, 22)
    val BodyStrong = base(15, FontWeight.SemiBold, Agz.Muted, 22)
    val Small = base(13, FontWeight.Normal, Agz.Muted, 19)
    val Badge = base(12, FontWeight.SemiBold, Agz.Ink, 16)
    val Label = base(14, FontWeight.Bold, Agz.Muted, 20)
    val Input = base(17, FontWeight.Medium, Agz.Ink, 24)
    val Button = base(18, FontWeight.Bold, Color.White, 24)
    val Nav = base(12, FontWeight.Medium, Agz.Muted, 16)
    val Metric = base(32, FontWeight.ExtraBold, Agz.Ink, 40)
}

private val AgzTypography = Typography().let { t ->
    fun TextStyle.agz() = copy(fontFamily = Cairo, textDirection = TextDirection.Rtl, color = Agz.Ink)
    t.copy(
        displayLarge = t.displayLarge.agz(), displayMedium = t.displayMedium.agz(), displaySmall = t.displaySmall.agz(),
        headlineLarge = t.headlineLarge.agz(), headlineMedium = t.headlineMedium.agz(), headlineSmall = t.headlineSmall.agz(),
        titleLarge = t.titleLarge.agz(), titleMedium = t.titleMedium.agz(), titleSmall = t.titleSmall.agz(),
        bodyLarge = t.bodyLarge.agz(), bodyMedium = t.bodyMedium.agz(), bodySmall = t.bodySmall.agz(),
        labelLarge = t.labelLarge.agz(), labelMedium = t.labelMedium.agz(), labelSmall = t.labelSmall.agz()
    )
}

@Composable
fun AgazatyTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = Agz.Primary, onPrimary = Color.White,
                primaryContainer = Agz.Pale, onPrimaryContainer = Agz.PrimaryDark,
                background = Agz.Cream, onBackground = Agz.Ink,
                surface = Agz.Surface, onSurface = Agz.Ink,
                surfaceVariant = Agz.Pale, onSurfaceVariant = Agz.Muted,
                outline = Agz.Border, outlineVariant = Agz.Border,
                surfaceContainerHigh = Agz.Cream, surfaceContainerHighest = Agz.Pale,
            ),
            typography = AgzTypography,
            shapes = Shapes(
                small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(22.dp),
                large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(28.dp)
            ),
            content = content
        )
    }
}
