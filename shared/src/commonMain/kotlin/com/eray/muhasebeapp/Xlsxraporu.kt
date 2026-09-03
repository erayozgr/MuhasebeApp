package com.eray.muhasebeapp

import com.eray.muhasebeapp.database.Alis
import com.eray.muhasebeapp.database.AlisKalemi
import com.eray.muhasebeapp.database.Masraf
import com.eray.muhasebeapp.database.Satis
import com.eray.muhasebeapp.database.SatisKalemi
import com.eray.muhasebeapp.database.StokHareketi
import com.eray.muhasebeapp.database.Tahsilat
import com.eray.muhasebeapp.database.TedarikciOdemesi

/**
 * Android + iOS (Kotlin Multiplatform) uyumlu, harici Excel kutuphanesi kullanmadan
 * gercek .xlsx dosyasi ureten rapor modulu.
 *
 * Ozellikler:
 * - Genel / Satis / Alis / Masraf / Stok / Tahsilat raporlari
 * - Alis raporunda tedarikci odemeleri de bulunur
 * - Tarih araligi bilgisi
 * - Profesyonel basliklar, toplam satirlari ve TL bicimi
 * - Otomatik filtre
 * - Sabitlenen baslik satiri (freeze pane)
 * - Ayarlanmis sutun genislikleri
 * - Excel / Apple Numbers / Google Sheets uyumlu OOXML
 */
fun excelXlsxOlustur(
    satislar: List<Satis>,
    alislar: List<Alis>,
    masraflar: List<Masraf>,
    stokHareketleri: List<StokHareketi> = emptyList(),
    tahsilatlar: List<Tahsilat> = emptyList(),
    tedarikciOdemeleri: List<TedarikciOdemesi> = emptyList(),
    raporTuru: String = "Genel Rapor",
    satisKalemleriGetir: (Long) -> List<SatisKalemi> = { emptyList() },
    alisKalemleriGetir: (Long) -> List<AlisKalemi> = { emptyList() },
    baslangicMs: Long? = null,
    bitisMs: Long? = null
): ByteArray {

    val toplamSatis = satislar.sumOf { it.toplamTutar }
    val toplamAlis = alislar.sumOf { it.toplamTutar }
    val toplamMasraf = masraflar.sumOf { it.tutar }
    val toplamTahsilat = tahsilatlar.sumOf { it.tutar }
    val toplamTedarikciOdemesi = tedarikciOdemeleri.sumOf { it.tutar }

    val brutKar = toplamSatis - toplamAlis
    val netKar = toplamSatis - toplamAlis - toplamMasraf
    val ortalamaSatis = if (satislar.isNotEmpty()) toplamSatis / satislar.size else 0.0
    val ortalamaAlis = if (alislar.isNotEmpty()) toplamAlis / alislar.size else 0.0
    val stokHareketToplami = stokHareketleri.sumOf { it.birimFiyat * it.miktar }

    val donemMetni = raporDonemMetni(baslangicMs, bitisMs)

    fun raporUstBilgi(builder: SheetBuilder, kolonSayisi: Int, baslik: String) {
        builder.title(baslik, kolonSayisi)
        builder.meta("Rapor Türü", raporTuru, kolonSayisi)
        builder.meta("Rapor Dönemi", donemMetni, kolonSayisi)
        builder.blank()
    }

    fun ozetSheet(): WorksheetData {
        val b = SheetBuilder(
            widths = listOf(38.0, 22.0),
            tabColor = "1F4E78"
        )

        b.title("MUHASEBE RAPORU - YÖNETİCİ ÖZETİ", 2)
        b.meta("Rapor Türü", raporTuru, 2)
        b.meta("Rapor Dönemi", donemMetni, 2)
        b.blank()

        b.section("FİNANSAL ÖZET", 2)
        b.row(Cell.Text("Toplam Satış (Ciro)", S_LABEL), Cell.Num(toplamSatis, S_CURRENCY))
        b.row(Cell.Text("Toplam Alış", S_LABEL), Cell.Num(toplamAlis, S_CURRENCY))
        b.row(Cell.Text("Toplam Masraf", S_LABEL), Cell.Num(toplamMasraf, S_CURRENCY))
        b.row(Cell.Text("Brüt Kâr (Satış - Alış)", S_LABEL), Cell.Num(brutKar, S_CURRENCY))
        b.row(Cell.Text("Net Kâr / Zarar", S_TOTAL_LABEL), Cell.Num(netKar, S_TOTAL_CURRENCY))
        b.blank()

        b.section("NAKİT AKIŞI", 2)
        b.row(Cell.Text("Müşteri Tahsilatı", S_LABEL), Cell.Num(toplamTahsilat, S_CURRENCY))
        b.row(Cell.Text("Tedarikçi Ödemesi", S_LABEL), Cell.Num(toplamTedarikciOdemesi, S_CURRENCY))
        b.row(
            Cell.Text("Net Nakit Hareketi (Tahsilat - Tedarikçi Ödemesi - Masraf)", S_TOTAL_LABEL),
            Cell.Num(toplamTahsilat - toplamTedarikciOdemesi - toplamMasraf, S_TOTAL_CURRENCY)
        )
        b.blank()

        b.section("İŞLEM İSTATİSTİKLERİ", 2)
        b.row(Cell.Text("Satış Adedi", S_LABEL), Cell.Num(satislar.size.toDouble(), S_INTEGER))
        b.row(Cell.Text("Alış Adedi", S_LABEL), Cell.Num(alislar.size.toDouble(), S_INTEGER))
        b.row(Cell.Text("Masraf Adedi", S_LABEL), Cell.Num(masraflar.size.toDouble(), S_INTEGER))
        b.row(Cell.Text("Tahsilat Adedi", S_LABEL), Cell.Num(tahsilatlar.size.toDouble(), S_INTEGER))
        b.row(Cell.Text("Tedarikçi Ödemesi Adedi", S_LABEL), Cell.Num(tedarikciOdemeleri.size.toDouble(), S_INTEGER))
        b.row(Cell.Text("Stok Hareketi Adedi", S_LABEL), Cell.Num(stokHareketleri.size.toDouble(), S_INTEGER))
        b.row(Cell.Text("Ortalama Satış Tutarı", S_LABEL), Cell.Num(ortalamaSatis, S_CURRENCY))
        b.row(Cell.Text("Ortalama Alış Tutarı", S_LABEL), Cell.Num(ortalamaAlis, S_CURRENCY))
        b.row(Cell.Text("Stok Hareketleri Toplam Tutarı", S_LABEL), Cell.Num(stokHareketToplami, S_CURRENCY))

        return b.build()
    }

    fun satislarSheet(): WorksheetData {
        val b = SheetBuilder(
            widths = listOf(20.0, 28.0, 34.0, 12.0, 14.0, 18.0, 18.0),
            tabColor = "34C759"
        )
        raporUstBilgi(b, 7, "SATIŞ RAPORU")

        val headerRow = b.nextRowNumber
        b.row(
            Cell.Text("Tarih", S_HEADER),
            Cell.Text("Müşteri", S_HEADER),
            Cell.Text("Ürün", S_HEADER),
            Cell.Text("Adet", S_HEADER),
            Cell.Text("Birim", S_HEADER),
            Cell.Text("Birim Fiyat", S_HEADER),
            Cell.Text("Satış Tutarı", S_HEADER)
        )

        var detaySatirSayisi = 0
        satislar.forEach { s ->
            val kalemler = satisKalemleriGetir(s.id)
            if (kalemler.isEmpty()) {
                b.row(
                    Cell.Text(raporTarih(s.tarih), S_DATE_TEXT),
                    Cell.Text(s.musteriAdi),
                    Cell.Text("-"),
                    Cell.Text("-", S_CENTER),
                    Cell.Text("-", S_CENTER),
                    Cell.Text("-", S_CENTER),
                    Cell.Num(s.toplamTutar, S_CURRENCY)
                )
                detaySatirSayisi++
            } else {
                kalemler.forEach { k ->
                    b.row(
                        Cell.Text(raporTarih(s.tarih), S_DATE_TEXT),
                        Cell.Text(s.musteriAdi),
                        Cell.Text(k.urunAdi),
                        Cell.Num(k.adet.toDouble(), S_INTEGER),
                        Cell.Text(k.birim, S_CENTER),
                        Cell.Num(k.birimFiyat, S_CURRENCY),
                        Cell.Num(k.toplam, S_CURRENCY)
                    )
                    detaySatirSayisi++
                }
            }
        }

        val lastDataRow = if (detaySatirSayisi > 0) b.currentRowNumber else headerRow
        b.row(
            Cell.Text("TOPLAM", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Text("", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Text("", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Num(toplamSatis, S_TOTAL_CURRENCY)
        )
        b.freezeBelow(headerRow)
        b.autoFilter(headerRow, lastDataRow, 7)
        return b.build()
    }

    fun alislarSheet(): WorksheetData {
        val b = SheetBuilder(
            widths = listOf(20.0, 28.0, 34.0, 12.0, 18.0, 18.0),
            tabColor = "FF9500"
        )
        raporUstBilgi(b, 6, "ALIŞ RAPORU")

        val headerRow = b.nextRowNumber
        b.row(
            Cell.Text("Tarih", S_HEADER),
            Cell.Text("Tedarikçi", S_HEADER),
            Cell.Text("Ürün", S_HEADER),
            Cell.Text("Adet", S_HEADER),
            Cell.Text("Birim Fiyat", S_HEADER),
            Cell.Text("Alış Tutarı", S_HEADER)
        )

        var detaySatirSayisi = 0
        alislar.forEach { a ->
            val kalemler = alisKalemleriGetir(a.id)
            if (kalemler.isEmpty()) {
                b.row(
                    Cell.Text(raporTarih(a.tarih), S_DATE_TEXT),
                    Cell.Text(a.tedarikciAdi),
                    Cell.Text("-"),
                    Cell.Text("-", S_CENTER),
                    Cell.Text("-", S_CENTER),
                    Cell.Num(a.toplamTutar, S_CURRENCY)
                )
                detaySatirSayisi++
            } else {
                kalemler.forEach { k ->
                    b.row(
                        Cell.Text(raporTarih(a.tarih), S_DATE_TEXT),
                        Cell.Text(a.tedarikciAdi),
                        Cell.Text(k.urunAdi),
                        Cell.Num(k.adet.toDouble(), S_INTEGER),
                        Cell.Num(k.birimFiyat, S_CURRENCY),
                        Cell.Num(k.toplam, S_CURRENCY)
                    )
                    detaySatirSayisi++
                }
            }
        }

        val lastDataRow = if (detaySatirSayisi > 0) b.currentRowNumber else headerRow
        b.row(
            Cell.Text("TOPLAM", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Text("", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Text("", S_TOTAL_LABEL), Cell.Num(toplamAlis, S_TOTAL_CURRENCY)
        )
        b.freezeBelow(headerRow)
        b.autoFilter(headerRow, lastDataRow, 6)
        return b.build()
    }

    fun masraflarSheet(): WorksheetData {
        val b = SheetBuilder(
            widths = listOf(20.0, 24.0, 52.0, 18.0),
            tabColor = "FF3B30"
        )
        raporUstBilgi(b, 4, "MASRAF RAPORU")

        val headerRow = b.nextRowNumber
        b.row(
            Cell.Text("Tarih", S_HEADER),
            Cell.Text("Kategori", S_HEADER),
            Cell.Text("Açıklama", S_HEADER),
            Cell.Text("Tutar", S_HEADER)
        )
        masraflar.forEach { m ->
            b.row(
                Cell.Text(raporTarih(m.tarih), S_DATE_TEXT),
                Cell.Text(m.kategori),
                Cell.Text(m.aciklama),
                Cell.Num(m.tutar, S_CURRENCY)
            )
        }
        val lastDataRow = if (masraflar.isNotEmpty()) b.currentRowNumber else headerRow
        b.row(
            Cell.Text("TOPLAM", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Text("", S_TOTAL_LABEL), Cell.Num(toplamMasraf, S_TOTAL_CURRENCY)
        )
        b.freezeBelow(headerRow)
        b.autoFilter(headerRow, lastDataRow, 4)
        return b.build()
    }

    fun stokSheet(): WorksheetData {
        val b = SheetBuilder(
            widths = listOf(20.0, 32.0, 22.0, 12.0, 18.0, 44.0, 18.0),
            tabColor = "5856D6"
        )
        raporUstBilgi(b, 7, "STOK HAREKETİ RAPORU")

        val headerRow = b.nextRowNumber
        b.row(
            Cell.Text("Tarih", S_HEADER),
            Cell.Text("Ürün", S_HEADER),
            Cell.Text("Hareket Türü", S_HEADER),
            Cell.Text("Miktar", S_HEADER),
            Cell.Text("Birim Fiyat", S_HEADER),
            Cell.Text("Açıklama", S_HEADER),
            Cell.Text("Tutar", S_HEADER)
        )
        stokHareketleri.forEach { h ->
            b.row(
                Cell.Text(raporTarih(h.tarih), S_DATE_TEXT),
                Cell.Text(h.urunAdi),
                Cell.Text(h.hareketTuru),
                Cell.Num(h.miktar.toDouble(), S_INTEGER),
                Cell.Num(h.birimFiyat, S_CURRENCY),
                Cell.Text(h.aciklama),
                Cell.Num(h.birimFiyat * h.miktar, S_CURRENCY)
            )
        }
        val lastDataRow = if (stokHareketleri.isNotEmpty()) b.currentRowNumber else headerRow
        b.row(
            Cell.Text("TOPLAM", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Text("", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Text("", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Num(stokHareketToplami, S_TOTAL_CURRENCY)
        )
        b.freezeBelow(headerRow)
        b.autoFilter(headerRow, lastDataRow, 7)
        return b.build()
    }

    fun tahsilatlarSheet(): WorksheetData {
        val b = SheetBuilder(
            widths = listOf(20.0, 36.0, 22.0),
            tabColor = "30B0C7"
        )
        raporUstBilgi(b, 3, "TAHSİLAT RAPORU")

        val headerRow = b.nextRowNumber
        b.row(
            Cell.Text("Tarih", S_HEADER),
            Cell.Text("Müşteri", S_HEADER),
            Cell.Text("Tahsil Edilen Tutar", S_HEADER)
        )
        tahsilatlar.forEach { t ->
            b.row(
                Cell.Text(raporTarih(t.tarih), S_DATE_TEXT),
                Cell.Text(t.musteriAdi),
                Cell.Num(t.tutar, S_CURRENCY)
            )
        }
        val lastDataRow = if (tahsilatlar.isNotEmpty()) b.currentRowNumber else headerRow
        b.row(
            Cell.Text("TOPLAM", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Num(toplamTahsilat, S_TOTAL_CURRENCY)
        )
        b.freezeBelow(headerRow)
        b.autoFilter(headerRow, lastDataRow, 3)
        return b.build()
    }

    fun tedarikciOdemeleriSheet(): WorksheetData {
        val b = SheetBuilder(
            widths = listOf(20.0, 36.0, 22.0),
            tabColor = "AF52DE"
        )
        raporUstBilgi(b, 3, "TEDARİKÇİ ÖDEMELERİ")

        val headerRow = b.nextRowNumber
        b.row(
            Cell.Text("Tarih", S_HEADER),
            Cell.Text("Tedarikçi", S_HEADER),
            Cell.Text("Ödenen Tutar", S_HEADER)
        )
        tedarikciOdemeleri.forEach { o ->
            b.row(
                Cell.Text(raporTarih(o.tarih), S_DATE_TEXT),
                Cell.Text(o.tedarikciAdi),
                Cell.Num(o.tutar, S_CURRENCY)
            )
        }
        val lastDataRow = if (tedarikciOdemeleri.isNotEmpty()) b.currentRowNumber else headerRow
        b.row(
            Cell.Text("TOPLAM", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Num(toplamTedarikciOdemesi, S_TOTAL_CURRENCY)
        )
        b.freezeBelow(headerRow)
        b.autoFilter(headerRow, lastDataRow, 3)
        return b.build()
    }

    val sayfalar = when (raporTuru) {
        "Satış Raporu" -> listOf(
            SheetSpec("Özet", ozetSheet()),
            SheetSpec("Satışlar", satislarSheet())
        )

        "Alış Raporu" -> listOf(
            SheetSpec("Özet", ozetSheet()),
            SheetSpec("Alışlar", alislarSheet()),
            SheetSpec("Tedarikçi Ödemeleri", tedarikciOdemeleriSheet())
        )

        "Masraf Raporu" -> listOf(
            SheetSpec("Özet", ozetSheet()),
            SheetSpec("Masraflar", masraflarSheet())
        )

        "Stok Hareketi Raporu" -> listOf(
            SheetSpec("Özet", ozetSheet()),
            SheetSpec("Stok Hareketleri", stokSheet())
        )

        "Tahsilat Raporu" -> listOf(
            SheetSpec("Özet", ozetSheet()),
            SheetSpec("Tahsilatlar", tahsilatlarSheet())
        )

        else -> listOf(
            SheetSpec("Özet", ozetSheet()),
            SheetSpec("Satışlar", satislarSheet()),
            SheetSpec("Alışlar", alislarSheet()),
            SheetSpec("Masraflar", masraflarSheet()),
            SheetSpec("Stok Hareketleri", stokSheet()),
            SheetSpec("Tahsilatlar", tahsilatlarSheet()),
            SheetSpec("Tedarikçi Ödemeleri", tedarikciOdemeleriSheet())
        )
    }

    return buildXlsx(sayfalar)
}

private fun raporDonemMetni(baslangicMs: Long?, bitisMs: Long?): String {
    if (baslangicMs == null && bitisMs == null) return "Tüm Zamanlar"

    val baslangic = baslangicMs?.let { raporTarih(it.toString()) } ?: "-"
    val bitis = bitisMs?.let { raporTarih(it.toString()) } ?: "-"
    return "$baslangic - $bitis"
}

private fun raporTarih(raw: String): String =
    formatTarih(raw).substringBefore(" ")

// -----------------------------------------------------------------------------
// XLSX MODEL
// -----------------------------------------------------------------------------

private data class SheetSpec(
    val name: String,
    val data: WorksheetData
)

private data class WorksheetData(
    val xml: String
)

private sealed class Cell {
    data class Text(val value: String, val style: Int = S_DEFAULT) : Cell()
    data class Num(val value: Double, val style: Int = S_NUMBER) : Cell()
}

private const val S_DEFAULT = 0
private const val S_TITLE = 1
private const val S_META_LABEL = 2
private const val S_META_VALUE = 3
private const val S_SECTION = 4
private const val S_HEADER = 5
private const val S_TEXT = 6
private const val S_CENTER = 7
private const val S_INTEGER = 8
private const val S_NUMBER = 9
private const val S_CURRENCY = 10
private const val S_LABEL = 11
private const val S_TOTAL_LABEL = 12
private const val S_TOTAL_CURRENCY = 13
private const val S_DATE_TEXT = 14

private class SheetBuilder(
    private val widths: List<Double>,
    private val tabColor: String = "1F4E78"
) {
    private val rows = StringBuilder()
    private val merges = mutableListOf<String>()
    private var freezeRow: Int? = null
    private var filterRef: String? = null
    private var rowNum = 0
    private var maxCol = widths.size.coerceAtLeast(1)

    val currentRowNumber: Int get() = rowNum
    val nextRowNumber: Int get() = rowNum + 1

    fun title(text: String, columnCount: Int) {
        rowNum++
        val lastCol = colLetter(columnCount - 1)
        rows.append("<row r=\"$rowNum\" ht=\"30\" customHeight=\"1\">")
        rows.append(textCell("A$rowNum", text, S_TITLE))
        rows.append("</row>")
        merges += "A$rowNum:$lastCol$rowNum"
        maxCol = maxOf(maxCol, columnCount)
    }

    fun meta(label: String, value: String, columnCount: Int) {
        rowNum++
        val lastCol = colLetter(columnCount - 1)
        rows.append("<row r=\"$rowNum\" ht=\"20\" customHeight=\"1\">")
        rows.append(textCell("A$rowNum", label, S_META_LABEL))
        rows.append(textCell("B$rowNum", value, S_META_VALUE))
        rows.append("</row>")
        if (columnCount > 2) merges += "B$rowNum:$lastCol$rowNum"
        maxCol = maxOf(maxCol, columnCount)
    }

    fun section(text: String, columnCount: Int) {
        rowNum++
        val lastCol = colLetter(columnCount - 1)
        rows.append("<row r=\"$rowNum\" ht=\"22\" customHeight=\"1\">")
        rows.append(textCell("A$rowNum", text, S_SECTION))
        rows.append("</row>")
        merges += "A$rowNum:$lastCol$rowNum"
        maxCol = maxOf(maxCol, columnCount)
    }

    fun row(vararg cells: Cell) {
        rowNum++
        maxCol = maxOf(maxCol, cells.size)
        rows.append("<row r=\"$rowNum\" ht=\"20\" customHeight=\"1\">")
        cells.forEachIndexed { index, cell ->
            val ref = "${colLetter(index)}$rowNum"
            when (cell) {
                is Cell.Text -> rows.append(textCell(ref, cell.value, cell.style))
                is Cell.Num -> rows.append(numberCell(ref, cell.value, cell.style))
            }
        }
        rows.append("</row>")
    }

    fun blank() {
        rowNum++
        rows.append("<row r=\"$rowNum\" ht=\"8\" customHeight=\"1\"/>")
    }

    fun freezeBelow(row: Int) {
        freezeRow = row + 1
    }

    fun autoFilter(headerRow: Int, lastDataRow: Int, columnCount: Int) {
        val endRow = maxOf(headerRow, lastDataRow)
        filterRef = "A$headerRow:${colLetter(columnCount - 1)}$endRow"
    }

    fun build(): WorksheetData {
        val colsXml = widths.mapIndexed { index, width ->
            val i = index + 1
            "<col min=\"$i\" max=\"$i\" width=\"${fmtNum(width)}\" customWidth=\"1\"/>"
        }.joinToString("")

        val paneXml = freezeRow?.let { row ->
            "<pane ySplit=\"${row - 1}\" topLeftCell=\"A$row\" activePane=\"bottomLeft\" state=\"frozen\"/>"
        } ?: ""

        val mergeXml = if (merges.isNotEmpty()) {
            "<mergeCells count=\"${merges.size}\">" +
                    merges.joinToString("") { "<mergeCell ref=\"$it\"/>" } +
                    "</mergeCells>"
        } else ""

        val filterXml = filterRef?.let { "<autoFilter ref=\"$it\"/>" } ?: ""
        val lastCell = "${colLetter(maxCol - 1)}${rowNum.coerceAtLeast(1)}"

        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetPr><tabColor rgb="FF$tabColor"/></sheetPr>
<dimension ref="A1:$lastCell"/>
<sheetViews>
  <sheetView workbookViewId="0" showGridLines="0">$paneXml</sheetView>
</sheetViews>
<sheetFormatPr defaultRowHeight="20"/>
<cols>$colsXml</cols>
<sheetData>$rows</sheetData>
$filterXml
$mergeXml
<pageMargins left="0.35" right="0.35" top="0.5" bottom="0.5" header="0.2" footer="0.2"/>
<pageSetup orientation="landscape" fitToWidth="1" fitToHeight="0" paperSize="9"/>
</worksheet>"""

        return WorksheetData(xml)
    }
}

private fun textCell(ref: String, value: String, style: Int): String {
    val escaped = xmlEsc(value)
    val preserve = if (value.startsWith(" ") || value.endsWith(" ") || value.contains("\n")) " xml:space=\"preserve\"" else ""
    return "<c r=\"$ref\" t=\"inlineStr\" s=\"$style\"><is><t$preserve>$escaped</t></is></c>"
}

private fun numberCell(ref: String, value: Double, style: Int): String =
    "<c r=\"$ref\" s=\"$style\"><v>${fmtNum(value)}</v></c>"

private fun colLetter(zeroBasedIndex: Int): String {
    var n = zeroBasedIndex + 1
    val out = StringBuilder()
    while (n > 0) {
        val rem = (n - 1) % 26
        out.insert(0, ('A'.code + rem).toChar())
        n = (n - 1) / 26
    }
    return out.toString()
}

private fun xmlEsc(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

private fun fmtNum(v: Double): String {
    if (!v.isFinite()) return "0"
    return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}

// -----------------------------------------------------------------------------
// XLSX PACKAGE
// -----------------------------------------------------------------------------

private fun buildXlsx(sheets: List<SheetSpec>): ByteArray {
    val sheetEntries = sheets.mapIndexed { index, sheet ->
        ZipEntryData(
            name = "xl/worksheets/sheet${index + 1}.xml",
            data = sheet.data.xml.encodeToByteArray()
        )
    }

    val worksheetOverrides = sheets.indices.joinToString("") { i ->
        "<Override PartName=\"/xl/worksheets/sheet${i + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
    }

    val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
$worksheetOverrides
</Types>"""

    val sheetsXml = sheets.mapIndexed { index, sheet ->
        "<sheet name=\"${xmlEsc(sanitizeSheetName(sheet.name))}\" sheetId=\"${index + 1}\" r:id=\"rId${index + 1}\"/>"
    }.joinToString("")

    val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<workbookPr date1904="0"/>
<bookViews><workbookView xWindow="0" yWindow="0" windowWidth="24000" windowHeight="14000"/></bookViews>
<sheets>$sheetsXml</sheets>
<calcPr calcId="191029" fullCalcOnLoad="1"/>
</workbook>"""

    val worksheetRelations = sheets.indices.joinToString("") { i ->
        "<Relationship Id=\"rId${i + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${i + 1}.xml\"/>"
    }
    val styleRelationId = "rId${sheets.size + 1}"

    val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$worksheetRelations
<Relationship Id="$styleRelationId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    val entries = listOf(
        ZipEntryData("[Content_Types].xml", contentTypesXml.encodeToByteArray()),
        ZipEntryData("_rels/.rels", ROOT_RELS_XML.encodeToByteArray()),
        ZipEntryData("xl/workbook.xml", workbookXml.encodeToByteArray()),
        ZipEntryData("xl/_rels/workbook.xml.rels", workbookRelsXml.encodeToByteArray()),
        ZipEntryData("xl/styles.xml", STYLES_XML.encodeToByteArray())
    ) + sheetEntries

    return buildZip(entries)
}

private fun sanitizeSheetName(name: String): String {
    val cleaned = name
        .replace("[", "(")
        .replace("]", ")")
        .replace(":", "-")
        .replace("*", "-")
        .replace("?", "-")
        .replace("/", "-")
        .replace("\\", "-")
    return cleaned.take(31).ifBlank { "Rapor" }
}

private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="1">
  <numFmt numFmtId="164" formatCode="#,##0.00&quot; TL&quot;"/>
</numFmts>

<fonts count="5">
  <font><sz val="11"/><name val="Calibri"/><family val="2"/></font>
  <font><b/><sz val="18"/><color rgb="FFFFFFFF"/><name val="Calibri"/><family val="2"/></font>
  <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/><family val="2"/></font>
  <font><b/><sz val="11"/><color rgb="FF1F1F1F"/><name val="Calibri"/><family val="2"/></font>
  <font><sz val="10"/><color rgb="FF666666"/><name val="Calibri"/><family val="2"/></font>
</fonts>

<fills count="8">
  <fill><patternFill patternType="none"/></fill>
  <fill><patternFill patternType="gray125"/></fill>
  <fill><patternFill patternType="solid"><fgColor rgb="FF1F4E78"/><bgColor indexed="64"/></patternFill></fill>
  <fill><patternFill patternType="solid"><fgColor rgb="FF2E75B6"/><bgColor indexed="64"/></patternFill></fill>
  <fill><patternFill patternType="solid"><fgColor rgb="FFD9EAF7"/><bgColor indexed="64"/></patternFill></fill>
  <fill><patternFill patternType="solid"><fgColor rgb="FFF3F6F9"/><bgColor indexed="64"/></patternFill></fill>
  <fill><patternFill patternType="solid"><fgColor rgb="FFDDEBF7"/><bgColor indexed="64"/></patternFill></fill>
  <fill><patternFill patternType="solid"><fgColor rgb="FFE2F0D9"/><bgColor indexed="64"/></patternFill></fill>
</fills>

<borders count="3">
  <border><left/><right/><top/><bottom/><diagonal/></border>
  <border>
    <left style="thin"><color rgb="FFD9D9D9"/></left>
    <right style="thin"><color rgb="FFD9D9D9"/></right>
    <top style="thin"><color rgb="FFD9D9D9"/></top>
    <bottom style="thin"><color rgb="FFD9D9D9"/></bottom>
    <diagonal/>
  </border>
  <border>
    <left style="thin"><color rgb="FF9EADBA"/></left>
    <right style="thin"><color rgb="FF9EADBA"/></right>
    <top style="medium"><color rgb="FF2E75B6"/></top>
    <bottom style="thin"><color rgb="FF9EADBA"/></bottom>
    <diagonal/>
  </border>
</borders>

<cellStyleXfs count="1">
  <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
</cellStyleXfs>

<cellXfs count="15">
  <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
  <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"><alignment horizontal="left" vertical="center"/></xf>
  <xf numFmtId="0" fontId="3" fillId="5" borderId="0" xfId="0" applyFont="1" applyFill="1"><alignment vertical="center"/></xf>
  <xf numFmtId="0" fontId="0" fillId="5" borderId="0" xfId="0" applyFill="1"><alignment vertical="center"/></xf>
  <xf numFmtId="0" fontId="3" fillId="4" borderId="0" xfId="0" applyFont="1" applyFill="1"><alignment vertical="center"/></xf>
  <xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
  <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"><alignment vertical="center" wrapText="1"/></xf>
  <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
  <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
  <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"><alignment horizontal="right" vertical="center"/></xf>
  <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"><alignment horizontal="right" vertical="center"/></xf>
  <xf numFmtId="0" fontId="3" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment vertical="center"/></xf>
  <xf numFmtId="0" fontId="3" fillId="6" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment vertical="center"/></xf>
  <xf numFmtId="164" fontId="3" fillId="6" borderId="2" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="right" vertical="center"/></xf>
  <xf numFmtId="0" fontId="4" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
</cellXfs>
</styleSheet>"""

// -----------------------------------------------------------------------------
// KMP-UYUMLU ZIP (store/no-compression)
// -----------------------------------------------------------------------------

private data class ZipEntryData(
    val name: String,
    val data: ByteArray
)

private class BytesBuilder {
    private val list = ArrayList<Byte>()

    fun u8(v: Int) {
        list.add((v and 0xFF).toByte())
    }

    fun u16(v: Int) {
        u8(v)
        u8(v shr 8)
    }

    fun u32(v: Int) {
        u8(v)
        u8(v shr 8)
        u8(v shr 16)
        u8(v shr 24)
    }

    fun bytes(bytes: ByteArray) {
        for (b in bytes) list.add(b)
    }

    val size: Int get() = list.size

    fun toByteArray(): ByteArray = list.toByteArray()
}

private val CRC_TABLE = IntArray(256).also { table ->
    for (n in 0 until 256) {
        var c = n
        repeat(8) {
            c = if ((c and 1) != 0) {
                (c ushr 1) xor 0xEDB88320.toInt()
            } else {
                c ushr 1
            }
        }
        table[n] = c
    }
}

private fun crc32(data: ByteArray): Long {
    var c = 0xFFFFFFFF.toInt()
    for (b in data) {
        c = CRC_TABLE[(c xor b.toInt()) and 0xFF] xor (c ushr 8)
    }
    return (c.toLong() xor 0xFFFFFFFFL) and 0xFFFFFFFFL
}

private fun buildZip(entries: List<ZipEntryData>): ByteArray {
    val body = BytesBuilder()

    data class LocalRef(
        val name: String,
        val crc: Long,
        val size: Int,
        val offset: Int
    )

    val refs = ArrayList<LocalRef>()

    for (entry in entries) {
        val nameBytes = entry.name.encodeToByteArray()
        val crc = crc32(entry.data)
        val offset = body.size

        body.u32(0x04034b50)
        body.u16(20)
        body.u16(0)
        body.u16(0)
        body.u16(0)
        body.u16(0x21)
        body.u32(crc.toInt())
        body.u32(entry.data.size)
        body.u32(entry.data.size)
        body.u16(nameBytes.size)
        body.u16(0)
        body.bytes(nameBytes)
        body.bytes(entry.data)

        refs.add(LocalRef(entry.name, crc, entry.data.size, offset))
    }

    val centralStart = body.size

    for (r in refs) {
        val nameBytes = r.name.encodeToByteArray()

        body.u32(0x02014b50)
        body.u16(20)
        body.u16(20)
        body.u16(0)
        body.u16(0)
        body.u16(0)
        body.u16(0x21)
        body.u32(r.crc.toInt())
        body.u32(r.size)
        body.u32(r.size)
        body.u16(nameBytes.size)
        body.u16(0)
        body.u16(0)
        body.u16(0)
        body.u16(0)
        body.u32(0)
        body.u32(r.offset)
        body.bytes(nameBytes)
    }

    val centralSize = body.size - centralStart

    body.u32(0x06054b50)
    body.u16(0)
    body.u16(0)
    body.u16(refs.size)
    body.u16(refs.size)
    body.u32(centralSize)
    body.u32(centralStart)
    body.u16(0)

    return body.toByteArray()
}
