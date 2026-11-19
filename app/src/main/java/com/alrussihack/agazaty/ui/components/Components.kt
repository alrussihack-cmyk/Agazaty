package com.alrussihack.agazaty.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alrussihack.agazaty.domain.LeaveStatus
import com.alrussihack.agazaty.ui.theme.Agz
import com.alrussihack.agazaty.ui.theme.AgzShape
import com.alrussihack.agazaty.ui.theme.AgzType

/* ---------- surfaces ---------- */

@Composable
fun AgzCard(
    modifier: Modifier = Modifier,
    shape: Shape = AgzShape.Card,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val border = BorderStroke(1.dp, Agz.Border)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = Agz.Surface, border = border, content = content)
    } else {
        Surface(modifier = modifier, shape = shape, color = Agz.Surface, border = border, content = content)
    }
}

@Composable
fun Avatar(initial: String, size: Dp = 38.dp, fontSize: TextUnit = 19.sp, bg: Color = Agz.Pale) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Text(initial, style = AgzType.CardTitle.copy(color = Agz.PrimaryDark, fontSize = fontSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    }
}

@Composable
fun Badge(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(AgzShape.Pill).background(bg).padding(horizontal = 11.dp, vertical = 4.dp)) {
        Text(text, style = AgzType.Badge.copy(color = fg), maxLines = 1)
    }
}

fun daysAgoText(n: Int): String = when (n) {
    0 -> "اليوم"
    1 -> "أمس"
    2 -> "منذ يومين"
    in 3..10 -> "منذ $n أيام"
    else -> "منذ $n يومًا"
}

@Composable
fun LeaveBadge(status: LeaveStatus, longReturn: Boolean = false) {
    when (status) {
        LeaveStatus.OnLeave -> Badge("في إجازة", Agz.Pale, Agz.PrimaryDark)
        LeaveStatus.Upcoming -> Badge("قادمة", Agz.Pale, Agz.PrimaryDark)
        LeaveStatus.NoReturn -> Badge("غير مسجّل", Agz.Stone, Agz.StoneInk)
        is LeaveStatus.Returned -> {
            val late = status.daysAgo > 30
            val text = if (longReturn && late) "عاد بعد 30 يومًا" else "عاد ${daysAgoText(status.daysAgo)}"
            Badge(text, if (late) Agz.Sky else Agz.Mint, if (late) Agz.SkyInk else Agz.MintInk)
        }
    }
}

@Composable
fun Pill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, small: Boolean = false) {
    val shape = AgzShape.Pill
    Box(
        modifier
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(if (selected) Agz.Primary else Agz.Surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, Agz.Border, shape))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (small) 6.dp else 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = AgzType.Body.copy(
                fontSize = if (small) 13.sp else 15.sp,
                color = if (selected) Color.White else Agz.Muted,
                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
            ),
            maxLines = 1
        )
    }
}

/* ---------- text ---------- */

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) = Text(text, style = AgzType.Section, modifier = modifier)

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) =
    Text(text, style = AgzType.Label, modifier = modifier.padding(bottom = 8.dp))

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    centerTrailing: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
        verticalAlignment = if (centerTrailing) Alignment.CenterVertically else Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) Text(eyebrow, style = AgzType.Small.copy(color = Agz.Muted), modifier = Modifier.padding(bottom = 2.dp))
            Text(title, style = AgzType.Title)
        }
        trailing?.invoke()
    }
}

/* ---------- inputs ---------- */

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, showIcon: Boolean = true) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth().height(46.dp),
        singleLine = true,
        textStyle = AgzType.Input.copy(fontSize = 15.sp),
        cursorBrush = SolidColor(Agz.Primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxSize().clip(AgzShape.Field).background(Agz.Surface).border(1.dp, Agz.Border, AgzShape.Field).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showIcon) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = Agz.Muted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(hint, style = AgzType.Body)
                    inner()
                }
            }
        }
    )
}

@Composable
fun AgzTextField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        textStyle = AgzType.Input,
        cursorBrush = SolidColor(Agz.Primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(AgzShape.Field).background(Agz.Surface)
                    .border(1.dp, Agz.Border, AgzShape.Field).padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) Text(placeholder, style = AgzType.Input.copy(color = Agz.Placeholder, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal))
                inner()
            }
        }
    )
}

/** Read-only field that opens a picker (used for dates). */
@Composable
fun PickerField(value: String, placeholder: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(AgzShape.Field).background(Agz.Surface)
            .border(1.dp, Agz.Border, AgzShape.Field).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            if (value.isEmpty()) placeholder else value,
            style = AgzType.Input.copy(
                color = if (value.isEmpty()) Agz.Placeholder else Agz.Ink,
                fontWeight = if (value.isEmpty()) androidx.compose.ui.text.font.FontWeight.Normal else androidx.compose.ui.text.font.FontWeight.Medium
            )
        )
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier.fillMaxWidth().height(50.dp).clip(AgzShape.Button)
            .background(if (enabled) Agz.Primary else Agz.Primary.copy(alpha = 0.38f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, style = AgzType.Button) }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Agz.Primary) {
    Box(modifier.heightIn(min = 44.dp).clip(AgzShape.Pill).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, style = AgzType.BodyStrong.copy(color = color))
    }
}

/** Card shown when a form field is filled from the employee database. */
@Composable
fun AutofillCard(initial: String, code: Int, job: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(AgzShape.Card).background(Agz.Pale).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Text(initial, style = AgzType.CardTitle.copy(color = Agz.PrimaryDark))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("يُعبّأ تلقائيًا من قاعدة الموظفين", style = AgzType.Small.copy(color = Agz.PrimaryDark))
            Text("الكود $code · $job", style = AgzType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/* ---------- glyph buttons (thin strokes, like the prototype) ---------- */

@Composable
fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = "إغلاق" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(16.dp)) {
            val s = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Agz.Muted, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = s.width, cap = s.cap)
            drawLine(Agz.Muted, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = s.width, cap = s.cap)
        }
    }
}

/** ">" in RTL — points toward the screen edge the user came from. */
@Composable
fun BackChevron(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = "رجوع" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(width = 10.dp, height = 18.dp)) {
            val p = Path().apply {
                moveTo(0f, 0f); lineTo(size.width, size.height / 2); lineTo(0f, size.height)
            }
            drawPath(p, Agz.Muted, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = description, tint = Agz.Muted, modifier = Modifier.size(26.dp))
    }
}

/* ---------- form container (full screen, no bottom bar) ---------- */

@Composable
fun FormScaffold(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(Agz.Cream).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Box(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp)) {
            Text(title, style = AgzType.Heading, modifier = Modifier.align(Alignment.Center))
            CloseButton(onClose, Modifier.align(Alignment.CenterStart))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            content = content
        )
    }
}

/* ---------- bottom navigation ---------- */

private data class NavEntry(val label: String, val icon: ImageVector)

private val NAV = listOf(
    NavEntry("الرئيسية", Icons.Outlined.Home),
    NavEntry("الإجازات", Icons.Outlined.CalendarToday),
    NavEntry("الشيفتات", Icons.Outlined.GridView),
    NavEntry("الموظفون", Icons.Outlined.People)
)

/** Order in RTL (right → left): الرئيسية، الإجازات، ＋، الشيفتات، الموظفون. */
@Composable
fun AgzBottomBar(selected: Int, onSelect: (Int) -> Unit, onAdd: () -> Unit, addDescription: String) {
    Box(
        Modifier.fillMaxWidth().background(Agz.Surface)
            .drawBehind { drawLine(Agz.Border, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().height(72.dp)) {
            NavSlot(NAV[0], selected == 0) { onSelect(0) }
            NavSlot(NAV[1], selected == 1) { onSelect(1) }
            Spacer(Modifier.weight(1f))
            NavSlot(NAV[2], selected == 2) { onSelect(2) }
            NavSlot(NAV[3], selected == 3) { onSelect(3) }
        }
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-6).dp).size(54.dp)
                .shadow(14.dp, CircleShape, ambientColor = Agz.Primary.copy(alpha = 0.35f), spotColor = Agz.Primary.copy(alpha = 0.55f))
                .clip(CircleShape).background(Agz.Primary)
                .clickable(role = Role.Button, onClick = onAdd)
                .semantics { contentDescription = addDescription },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp)) }
    }
}

@Composable
private fun RowScope.NavSlot(entry: NavEntry, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) Agz.Primary else Agz.Muted
    Column(
        Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(entry.icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(3.dp))
        Text(entry.label, style = AgzType.Nav.copy(color = tint))
    }
}
