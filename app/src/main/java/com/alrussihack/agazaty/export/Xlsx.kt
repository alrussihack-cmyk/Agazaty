package com.alrussihack.agazaty.export

import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimal, dependency-free .xlsx writer (SpreadsheetML in a zip).
 * Supports exactly what the reports need: right-to-left sheets, coloured fills, borders, merged
 * title rows, frozen header, auto-filter, column widths, real dates and numbers.
 */

enum class HAlign(val xml: String?) { Start(null), Center("center") }

data class CellStyle(
    val bold: Boolean = false,
    val size: Int = 11,
    val color: String = "2B1B10",
    val fill: String? = null,
    val border: Boolean = false,
    val align: HAlign = HAlign.Start,
    val wrap: Boolean = false,
    val numFmt: String? = null
)

/** value: String, Int, Long, Double, LocalDate or null (styled empty cell). */
class Cell(val value: Any?, val style: CellStyle)

class Sheet(val name: String, val tabColor: String, val widths: List<Double>) {
    class Row(val cells: List<Cell>, val height: Double?)

    val rows = mutableListOf<Row>()
    val merges = mutableListOf<String>()
    var freezeRows = 0
    var filterHeaderRow: Int? = null

    /** Adds a row and returns its 1-based row number. */
    fun addRow(cells: List<Cell>, height: Double? = null): Int {
        rows += Row(cells, height)
        return rows.size
    }

    fun merge(row: Int, firstCol: Int, lastCol: Int) {
        if (lastCol > firstCol) merges += "${colRef(firstCol)}$row:${colRef(lastCol)}$row"
    }
}

fun colRef(index: Int): String {
    var n = index
    val sb = StringBuilder()
    do {
        sb.append('A' + n % 26)
        n = n / 26 - 1
    } while (n >= 0)
    return sb.reverse().toString()
}

private const val DATE_FMT = "dd/mm/yyyy"
fun dateCell(d: LocalDate?, style: CellStyle) = Cell(d, style.copy(numFmt = DATE_FMT))

private val EXCEL_EPOCH: LocalDate = LocalDate.of(1899, 12, 30)

private fun esc(s: String): String {
    val sb = StringBuilder(s.length + 8)
    for (ch in s) {
        when {
            ch == '&' -> sb.append("&amp;")
            ch == '<' -> sb.append("&lt;")
            ch == '>' -> sb.append("&gt;")
            ch == '"' -> sb.append("&quot;")
            ch == '\'' -> sb.append("&apos;")
            ch.code < 0x20 && ch != '\n' && ch != '\t' && ch != '\r' -> Unit // illegal in XML 1.0
            else -> sb.append(ch)
        }
    }
    return sb.toString()
}

private class StyleBook {
    private val fonts = mutableListOf<String>()
    private val fills = mutableListOf(
        "<fill><patternFill patternType=\"none\"/></fill>",
        "<fill><patternFill patternType=\"gray125\"/></fill>"
    )
    private val borders = mutableListOf("<border><left/><right/><top/><bottom/><diagonal/></border>")
    private val numFmts = linkedMapOf<String, Int>()
    private val xfs = mutableListOf<String>()
    private val cache = HashMap<CellStyle, Int>()

    init { index(CellStyle()) } // xf 0 is the default style

    fun index(s: CellStyle): Int = cache[s] ?: build(s).also { cache[s] = it }

    private fun <T> pool(list: MutableList<T>, item: T): Int {
        val i = list.indexOf(item)
        if (i >= 0) return i
        list += item
        return list.size - 1
    }

    private fun build(s: CellStyle): Int {
        val font = "<font>${if (s.bold) "<b/>" else ""}<sz val=\"${s.size}\"/><color rgb=\"FF${s.color}\"/><name val=\"Arial\"/><family val=\"2\"/></font>"
        val fontId = pool(fonts, font)
        val fillId = if (s.fill == null) 0 else pool(
            fills,
            "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF${s.fill}\"/><bgColor indexed=\"64\"/></patternFill></fill>"
        )
        val borderId = if (!s.border) 0 else {
            val side = { tag: String -> "<$tag style=\"thin\"><color rgb=\"FFE6CFB8\"/></$tag>" }
            pool(borders, "<border>${side("left")}${side("right")}${side("top")}${side("bottom")}<diagonal/></border>")
        }
        val fmtId = s.numFmt?.let { code -> numFmts.getOrPut(code) { 164 + numFmts.size } } ?: 0
        val horizontal = s.align.xml?.let { " horizontal=\"$it\"" } ?: ""
        val wrap = if (s.wrap) " wrapText=\"1\"" else ""
        xfs += "<xf numFmtId=\"$fmtId\" fontId=\"$fontId\" fillId=\"$fillId\" borderId=\"$borderId\" xfId=\"0\" " +
            "applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">" +
            "<alignment$horizontal vertical=\"center\"$wrap readingOrder=\"2\"/></xf>"
        return xfs.size - 1
    }

    fun xml(): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        if (numFmts.isNotEmpty()) {
            append("<numFmts count=\"${numFmts.size}\">")
            numFmts.forEach { (code, id) -> append("<numFmt numFmtId=\"$id\" formatCode=\"${esc(code)}\"/>") }
            append("</numFmts>")
        }
        append("<fonts count=\"${fonts.size}\">").append(fonts.joinToString("")).append("</fonts>")
        append("<fills count=\"${fills.size}\">").append(fills.joinToString("")).append("</fills>")
        append("<borders count=\"${borders.size}\">").append(borders.joinToString("")).append("</borders>")
        append("<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>")
        append("<cellXfs count=\"${xfs.size}\">").append(xfs.joinToString("")).append("</cellXfs>")
        append("<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>")
        append("</styleSheet>")
    }
}

private fun sheetXml(sheet: Sheet, book: StyleBook, selected: Boolean): String = buildString {
    val lastCol = maxOf(sheet.widths.size, sheet.rows.maxOfOrNull { it.cells.size } ?: 1)
    val lastRow = maxOf(sheet.rows.size, 1)
    append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
    append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
    append("<sheetPr><tabColor rgb=\"FF${sheet.tabColor}\"/><pageSetUpPr fitToPage=\"1\"/></sheetPr>")
    append("<dimension ref=\"A1:${colRef(lastCol - 1)}$lastRow\"/>")
    append("<sheetViews><sheetView rightToLeft=\"1\" showGridLines=\"0\"${if (selected) " tabSelected=\"1\"" else ""} workbookViewId=\"0\">")
    if (sheet.freezeRows > 0) {
        val top = sheet.freezeRows + 1
        append("<pane ySplit=\"${sheet.freezeRows}\" topLeftCell=\"A$top\" activePane=\"bottomLeft\" state=\"frozen\"/>")
        append("<selection pane=\"bottomLeft\" activeCell=\"A$top\" sqref=\"A$top\"/>")
    }
    append("</sheetView></sheetViews>")
    append("<sheetFormatPr defaultRowHeight=\"21\"/>")
    append("<cols>")
    sheet.widths.forEachIndexed { i, w -> append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>") }
    append("</cols>")
    append("<sheetData>")
    sheet.rows.forEachIndexed { r, row ->
        val rn = r + 1
        append("<row r=\"$rn\"")
        if (row.height != null) append(" ht=\"${row.height}\" customHeight=\"1\"")
        append(">")
        row.cells.forEachIndexed { c, cell ->
            val ref = "${colRef(c)}$rn"
            val s = book.index(cell.style)
            when (val v = cell.value) {
                null -> append("<c r=\"$ref\" s=\"$s\"/>")
                is String -> append("<c r=\"$ref\" s=\"$s\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v)}</t></is></c>")
                is LocalDate -> append("<c r=\"$ref\" s=\"$s\"><v>${ChronoUnit.DAYS.between(EXCEL_EPOCH, v)}</v></c>")
                is Number -> append("<c r=\"$ref\" s=\"$s\"><v>$v</v></c>")
                else -> append("<c r=\"$ref\" s=\"$s\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v.toString())}</t></is></c>")
            }
        }
        append("</row>")
    }
    append("</sheetData>")
    sheet.filterHeaderRow?.let { h -> append("<autoFilter ref=\"A$h:${colRef(lastCol - 1)}$lastRow\"/>") }
    if (sheet.merges.isNotEmpty()) {
        append("<mergeCells count=\"${sheet.merges.size}\">")
        sheet.merges.forEach { append("<mergeCell ref=\"$it\"/>") }
        append("</mergeCells>")
    }
    append("<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.6\" bottom=\"0.6\" header=\"0.3\" footer=\"0.3\"/>")
    append("<pageSetup paperSize=\"9\" orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>")
    append("</worksheet>")
}

private fun safeSheetName(name: String): String =
    name.filter { it !in "[]:*?/\\" }.take(31).ifBlank { "Sheet" }

fun buildXlsx(sheets: List<Sheet>): ByteArray {
    require(sheets.isNotEmpty()) { "A workbook needs at least one sheet" }
    val book = StyleBook()
    val sheetParts = sheets.mapIndexed { i, s -> sheetXml(s, book, selected = i == 0) } // fills the style book
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        fun put(path: String, content: String) {
            zip.putNextEntry(ZipEntry(path))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        val head = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        val relNs = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

        put(
            "[Content_Types].xml",
            head + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                sheets.indices.joinToString("") {
                    "<Override PartName=\"/xl/worksheets/sheet${it + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                } +
                "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
                "</Types>"
        )
        put(
            "_rels/.rels",
            head + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"$relNs/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>"
        )
        put(
            "xl/workbook.xml",
            head + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"$relNs\">" +
                "<bookViews><workbookView xWindow=\"0\" yWindow=\"0\" windowWidth=\"24000\" windowHeight=\"12000\"/></bookViews><sheets>" +
                sheets.mapIndexed { i, s -> "<sheet name=\"${esc(safeSheetName(s.name))}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>" }.joinToString("") +
                "</sheets></workbook>"
        )
        put(
            "xl/_rels/workbook.xml.rels",
            head + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                sheets.indices.joinToString("") { "<Relationship Id=\"rId${it + 1}\" Type=\"$relNs/worksheet\" Target=\"worksheets/sheet${it + 1}.xml\"/>" } +
                "<Relationship Id=\"rId${sheets.size + 1}\" Type=\"$relNs/styles\" Target=\"styles.xml\"/></Relationships>"
        )
        put("xl/styles.xml", book.xml())
        sheetParts.forEachIndexed { i, xml -> put("xl/worksheets/sheet${i + 1}.xml", xml) }
    }
    return out.toByteArray()
}
